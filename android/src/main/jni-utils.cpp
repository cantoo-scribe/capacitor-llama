#include "jni-utils.h"
#include <android/log.h>
#include <cstring>

#define TAG "CAPLLAMA_JNI_UTILS"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace rnbridge {

// Sanitize UTF-8 string for JNI NewStringUTF
// Replaces invalid UTF-8 sequences with '?' to prevent JNI errors
std::string sanitize_utf8_for_jni(const char* text) {
    if (!text) return "";

    std::string result;
    result.reserve(strlen(text));

    const unsigned char* bytes = reinterpret_cast<const unsigned char*>(text);
    size_t i = 0;

    while (bytes[i] != 0) {
        unsigned char c = bytes[i];

        // ASCII (0x00-0x7F)
        if (c <= 0x7F) {
            result += static_cast<char>(c);
            i++;
        }
        // 2-byte sequence (0xC0-0xDF)
        else if ((c & 0xE0) == 0xC0) {
            if (bytes[i+1] != 0 && (bytes[i+1] & 0xC0) == 0x80) {
                result += static_cast<char>(bytes[i]);
                result += static_cast<char>(bytes[i+1]);
                i += 2;
            } else {
                result += '?';  // Invalid sequence
                i++;
            }
        }
        // 3-byte sequence (0xE0-0xEF)
        else if ((c & 0xF0) == 0xE0) {
            if (bytes[i+1] != 0 && (bytes[i+1] & 0xC0) == 0x80 &&
                bytes[i+2] != 0 && (bytes[i+2] & 0xC0) == 0x80) {
                result += static_cast<char>(bytes[i]);
                result += static_cast<char>(bytes[i+1]);
                result += static_cast<char>(bytes[i+2]);
                i += 3;
            } else {
                result += '?';  // Invalid sequence
                i++;
            }
        }
        // 4-byte sequence (0xF0-0xF7)
        else if ((c & 0xF8) == 0xF0) {
            if (bytes[i+1] != 0 && (bytes[i+1] & 0xC0) == 0x80 &&
                bytes[i+2] != 0 && (bytes[i+2] & 0xC0) == 0x80 &&
                bytes[i+3] != 0 && (bytes[i+3] & 0xC0) == 0x80) {
                result += static_cast<char>(bytes[i]);
                result += static_cast<char>(bytes[i+1]);
                result += static_cast<char>(bytes[i+2]);
                result += static_cast<char>(bytes[i+3]);
                i += 4;
            } else {
                result += '?';  // Invalid sequence
                i++;
            }
        }
        // Invalid start byte
        else {
            result += '?';
            i++;
        }
    }

    return result;
}

using namespace internal;

// Initialize cached class references - must be called during JNI_OnLoad
bool initialize(JNIEnv* env) {
    jclass jsObjectClass;
    jclass jsArrayClass;

    // Cache Capacitor JSObject class and methods
    jsObjectClass = env->FindClass("com/getcapacitor/JSObject");
    if (jsObjectClass == nullptr) {
        LOGE("initialize: Failed to find JSObject class");
        return false;
    }

    g_JSObjectClass = reinterpret_cast<jclass>(env->NewGlobalRef(jsObjectClass));
    env->DeleteLocalRef(jsObjectClass);

    if (g_JSObjectClass == nullptr) {
        LOGE("initialize: Failed to create global ref for JSObject class");
        return false;
    }

    g_JSObjectCtor = env->GetMethodID(g_JSObjectClass, "<init>", "()V");
    if (g_JSObjectCtor == nullptr) {
        LOGE("initialize: Failed to find JSObject constructor");
        goto cleanup;
    }

    g_putStringMethod = env->GetMethodID(g_JSObjectClass, "put", "(Ljava/lang/String;Ljava/lang/String;)Lcom/getcapacitor/JSObject;");
    g_putIntMethod = env->GetMethodID(g_JSObjectClass, "put", "(Ljava/lang/String;I)Lcom/getcapacitor/JSObject;");
    g_putDoubleMethod = env->GetMethodID(g_JSObjectClass, "put", "(Ljava/lang/String;D)Lcom/getcapacitor/JSObject;");
    g_putBooleanMethod = env->GetMethodID(g_JSObjectClass, "put", "(Ljava/lang/String;Z)Lcom/getcapacitor/JSObject;");
    g_putObjectMethod = env->GetMethodID(g_JSObjectClass, "put", "(Ljava/lang/String;Ljava/lang/Object;)Lcom/getcapacitor/JSObject;");

    if (g_putStringMethod == nullptr || g_putIntMethod == nullptr || g_putDoubleMethod == nullptr ||
        g_putBooleanMethod == nullptr || g_putObjectMethod == nullptr) {
        LOGE("initialize: Failed to find JSObject put methods");
        goto cleanup;
    }

    // Cache Capacitor JSArray class and methods
    jsArrayClass = env->FindClass("com/getcapacitor/JSArray");
    if (jsArrayClass == nullptr) {
        LOGE("initialize: Failed to find JSArray class");
        goto cleanup;
    }

    g_JSArrayClass = reinterpret_cast<jclass>(env->NewGlobalRef(jsArrayClass));
    env->DeleteLocalRef(jsArrayClass);

    if (g_JSArrayClass == nullptr) {
        LOGE("initialize: Failed to create global ref for JSArray class");
        goto cleanup;
    }

    g_JSArrayCtor = env->GetMethodID(g_JSArrayClass, "<init>", "()V");
    if (g_JSArrayCtor == nullptr) {
        LOGE("initialize: Failed to find JSArray constructor");
        goto cleanup;
    }

    g_pushIntMethod = env->GetMethodID(g_JSArrayClass, "put", "(I)Lorg/json/JSONArray;");
    g_pushDoubleMethod = env->GetMethodID(g_JSArrayClass, "put", "(D)Lorg/json/JSONArray;");
    g_pushStringMethod = env->GetMethodID(g_JSArrayClass, "put", "(Ljava/lang/Object;)Lorg/json/JSONArray;");
    g_pushObjectMethod = env->GetMethodID(g_JSArrayClass, "put", "(Ljava/lang/Object;)Lorg/json/JSONArray;");

    if (g_pushIntMethod == nullptr || g_pushDoubleMethod == nullptr ||
        g_pushStringMethod == nullptr || g_pushObjectMethod == nullptr) {
        LOGE("initialize: Failed to find JSArray put methods");
        goto cleanup;
    }

    LOGI("Successfully cached Capacitor class references");
    return true;

cleanup:
    cleanup(env);
    return false;
}

// Cleanup cached class references - called during JNI_OnUnload
void cleanup(JNIEnv* env) {
    if (g_JSObjectClass != nullptr) {
        env->DeleteGlobalRef(g_JSObjectClass);
        g_JSObjectClass = nullptr;
    }
    if (g_JSArrayClass != nullptr) {
        env->DeleteGlobalRef(g_JSArrayClass);
        g_JSArrayClass = nullptr;
    }
    g_JSObjectCtor = nullptr;
    g_JSArrayCtor = nullptr;
    g_putStringMethod = nullptr;
    g_putIntMethod = nullptr;
    g_putDoubleMethod = nullptr;
    g_putBooleanMethod = nullptr;
    g_putObjectMethod = nullptr;
    g_pushIntMethod = nullptr;
    g_pushDoubleMethod = nullptr;
    g_pushStringMethod = nullptr;
    g_pushObjectMethod = nullptr;
}

// WritableMap creation and manipulation

jobject createMap(JNIEnv *env) {
    if (g_JSObjectClass == nullptr || g_JSObjectCtor == nullptr) {
        jclass jsObjectClass = env->FindClass("com/getcapacitor/JSObject");
        if (jsObjectClass == nullptr) {
            LOGE("createMap: Failed to find JSObject class");
            return nullptr;
        }
        jmethodID ctor = env->GetMethodID(jsObjectClass, "<init>", "()V");
        if (ctor == nullptr) {
            LOGE("createMap: Failed to find JSObject constructor");
            return nullptr;
        }
        jobject map = env->NewObject(jsObjectClass, ctor);
        env->DeleteLocalRef(jsObjectClass);
        return map;
    }

    return env->NewObject(g_JSObjectClass, g_JSObjectCtor);
}

void putString(JNIEnv *env, jobject map, const char *key, const char *value) {
    jmethodID putStringMethod = g_putStringMethod;
    if (putStringMethod == nullptr) {
        jclass mapClass = env->FindClass("com/getcapacitor/JSObject");
        putStringMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/String;Ljava/lang/String;)Lcom/getcapacitor/JSObject;");
    }

    jstring jKey = env->NewStringUTF(key);
    std::string sanitized_value = sanitize_utf8_for_jni(value);
    jstring jValue = env->NewStringUTF(sanitized_value.c_str());

    env->CallObjectMethod(map, putStringMethod, jKey, jValue);
}

void putInt(JNIEnv *env, jobject map, const char *key, int value) {
    jmethodID putIntMethod = g_putIntMethod;
    if (putIntMethod == nullptr) {
        jclass mapClass = env->FindClass("com/getcapacitor/JSObject");
        putIntMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/String;I)Lcom/getcapacitor/JSObject;");
    }

    jstring jKey = env->NewStringUTF(key);
    env->CallObjectMethod(map, putIntMethod, jKey, value);
}

void putDouble(JNIEnv *env, jobject map, const char *key, double value) {
    jmethodID putDoubleMethod = g_putDoubleMethod;
    if (putDoubleMethod == nullptr) {
        jclass mapClass = env->FindClass("com/getcapacitor/JSObject");
        putDoubleMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/String;D)Lcom/getcapacitor/JSObject;");
    }

    jstring jKey = env->NewStringUTF(key);
    env->CallObjectMethod(map, putDoubleMethod, jKey, value);
}

void putBoolean(JNIEnv *env, jobject map, const char *key, bool value) {
    jmethodID putBooleanMethod = g_putBooleanMethod;
    if (putBooleanMethod == nullptr) {
        jclass mapClass = env->FindClass("com/getcapacitor/JSObject");
        putBooleanMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/String;Z)Lcom/getcapacitor/JSObject;");
    }

    jstring jKey = env->NewStringUTF(key);
    env->CallObjectMethod(map, putBooleanMethod, jKey, value);
}

void putMap(JNIEnv *env, jobject map, const char *key, jobject value) {
    jmethodID putObjectMethod = g_putObjectMethod;
    if (putObjectMethod == nullptr) {
        jclass mapClass = env->FindClass("com/getcapacitor/JSObject");
        putObjectMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/String;Ljava/lang/Object;)Lcom/getcapacitor/JSObject;");
    }

    jstring jKey = env->NewStringUTF(key);
    env->CallObjectMethod(map, putObjectMethod, jKey, value);
}

void putArray(JNIEnv *env, jobject map, const char *key, jobject value) {
    jmethodID putObjectMethod = g_putObjectMethod;
    if (putObjectMethod == nullptr) {
        jclass mapClass = env->FindClass("com/getcapacitor/JSObject");
        putObjectMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/String;Ljava/lang/Object;)Lcom/getcapacitor/JSObject;");
    }

    jstring jKey = env->NewStringUTF(key);
    env->CallObjectMethod(map, putObjectMethod, jKey, value);
}

// WritableArray creation and manipulation

jobject createArray(JNIEnv *env) {
    if (g_JSArrayClass == nullptr || g_JSArrayCtor == nullptr) {
        jclass jsArrayClass = env->FindClass("com/getcapacitor/JSArray");
        if (jsArrayClass == nullptr) {
            LOGE("createArray: Failed to find JSArray class");
            return nullptr;
        }
        jmethodID ctor = env->GetMethodID(jsArrayClass, "<init>", "()V");
        if (ctor == nullptr) {
            LOGE("createArray: Failed to find JSArray constructor");
            return nullptr;
        }
        jobject arr = env->NewObject(jsArrayClass, ctor);
        env->DeleteLocalRef(jsArrayClass);
        return arr;
    }

    return env->NewObject(g_JSArrayClass, g_JSArrayCtor);
}

void pushInt(JNIEnv *env, jobject arr, int value) {
    jmethodID pushIntMethod = g_pushIntMethod;
    if (pushIntMethod == nullptr) {
        jclass arrayClass = env->FindClass("com/getcapacitor/JSArray");
        pushIntMethod = env->GetMethodID(arrayClass, "put", "(I)Lorg/json/JSONArray;");
    }

    env->CallObjectMethod(arr, pushIntMethod, value);
}

void pushDouble(JNIEnv *env, jobject arr, double value) {
    jmethodID pushDoubleMethod = g_pushDoubleMethod;
    if (pushDoubleMethod == nullptr) {
        jclass arrayClass = env->FindClass("com/getcapacitor/JSArray");
        pushDoubleMethod = env->GetMethodID(arrayClass, "put", "(D)Lorg/json/JSONArray;");
    }

    env->CallObjectMethod(arr, pushDoubleMethod, value);
}

void pushString(JNIEnv *env, jobject arr, const char *value) {
    jmethodID pushStringMethod = g_pushStringMethod;
    if (pushStringMethod == nullptr) {
        jclass arrayClass = env->FindClass("com/getcapacitor/JSArray");
        pushStringMethod = env->GetMethodID(arrayClass, "put", "(Ljava/lang/Object;)Lorg/json/JSONArray;");
    }

    std::string sanitized_value = sanitize_utf8_for_jni(value);
    jstring jValue = env->NewStringUTF(sanitized_value.c_str());
    env->CallObjectMethod(arr, pushStringMethod, jValue);
}

void pushMap(JNIEnv *env, jobject arr, jobject value) {
    jmethodID pushObjectMethod = g_pushObjectMethod;
    if (pushObjectMethod == nullptr) {
        jclass arrayClass = env->FindClass("com/getcapacitor/JSArray");
        pushObjectMethod = env->GetMethodID(arrayClass, "put", "(Ljava/lang/Object;)Lorg/json/JSONArray;");
    }

    env->CallObjectMethod(arr, pushObjectMethod, value);
}

} // namespace rnbridge
