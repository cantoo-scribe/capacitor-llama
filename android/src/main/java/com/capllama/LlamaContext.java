package com.capllama;

import android.os.Build;
import android.util.Log;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.StringBuilder;
import org.json.JSONArray;
import org.json.JSONException;

public class LlamaContext {

    public static final String NAME = "LlamaContext";

    private static String loadedLibrary = "";

    // private static class NativeLogCallback {
    //   DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;

    //   public NativeLogCallback(ReactApplicationContext reactContext) {
    //     this.eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
    //   }

    //   void emitNativeLog(String level, String text) {
    //     WritableMap event = Arguments.createMap();
    //     event.putString("level", level);
    //     event.putString("text", text);
    //     eventEmitter.emit("@RNLlama_onNativeLog", event);
    //   }
    // }

    // static void toggleNativeLog(ReactApplicationContext reactContext, boolean enabled) {
    //   if (LlamaContext.isArchNotSupported()) {
    //     throw new IllegalStateException("Only 64-bit architectures are supported");
    //   }
    //   if (enabled) {
    //     setupLog(new NativeLogCallback(reactContext));
    //   } else {
    //     unsetLog();
    //   }
    // }

    private int id;
    // private ReactApplicationContext reactContext;
    private long context;
    private JSObject modelDetails;
    private int jobId = -1;
    private boolean gpuEnabled;
    private String reasonNoGPU = "";
    private String systemInfo = "";
    private JSArray devices = null;

    // private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;

    public LlamaContext(int id, JSObject params) {
        // if (LlamaContext.isArchNotSupported()) {
        //   throw new IllegalStateException("Only 64-bit architectures are supported");
        // }
        if (!params.has("model")) {
            throw new IllegalArgumentException("Missing required parameter: model");
        }
        // eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        this.id = id;

        JSObject initResult = initContext(
            params,
            // LoadProgressCallback load_progress_callback
            // params.hasKey("use_progress_callback") ? new LoadProgressCallback(this) : null
            null
        );

        if (initResult == null) {
            throw new IllegalStateException("Failed to initialize context");
        }
        if (initResult.has("_error")) {
            throw new IllegalStateException(initResult.getString("_error"));
        }
        if (!initResult.has("context")) {
            throw new IllegalStateException("Failed to initialize context");
        }
        String contextPtr = initResult.getString("context");
        if (contextPtr == null || contextPtr.isEmpty()) {
            throw new IllegalStateException("Failed to initialize context");
        }
        try {
            this.context = Long.parseLong(contextPtr);
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalStateException("Invalid native context pointer", numberFormatException);
        }
        if (this.context == 0) {
            throw new IllegalStateException("Failed to initialize context");
        }

        this.gpuEnabled = initResult.has("gpu") && initResult.getBoolean("gpu", false);
        this.reasonNoGPU = initResult.has("reasonNoGPU") ? initResult.getString("reasonNoGPU") : "";
        if (this.reasonNoGPU == null) {
            this.reasonNoGPU = "";
        }
        if (!this.gpuEnabled && params.has("no_gpu_devices") && params.getBoolean("no_gpu_devices", false)) {
            this.reasonNoGPU = "GPU devices disabled by user";
        }
        if (initResult.has("devices")) {
            try {
                this.devices = JSArray.from(initResult.getJSONArray("devices"));
            } catch (JSONException ignored) {}
        }
        this.systemInfo = initResult.has("systemInfo") ? initResult.getString("systemInfo") : "";
        if (this.systemInfo == null) {
            this.systemInfo = "";
        }

        if (this.context == -1) {
            throw new IllegalStateException("Failed to initialize context");
        }
        this.modelDetails = loadModelDetails(this.context);
    }

    public void interruptLoad() {
        interruptLoad(this.context);
    }

    public long getContext() {
        return context;
    }

    public JSObject getModelDetails() {
        return modelDetails;
    }

    public String getLoadedLibrary() {
        return loadedLibrary;
    }

    public JSObject getFormattedChatWithJinja(String messages, String chatTemplate, JSObject params) {
        return getFormattedChatWithJinja(this.context, messages, chatTemplate == null ? "" : chatTemplate, params);
    }

    public String getFormattedChat(String messages, String chatTemplate) {
        return getFormattedChat(this.context, messages, chatTemplate == null ? "" : chatTemplate);
    }

    private void emitLoadProgress(int progress) {
        // WritableMap event = Arguments.createMap();
        // event.putInt("contextId", LlamaContext.this.id);
        // event.putInt("progress", progress);
        // eventEmitter.emit("@RNLlama_onInitContextProgress", event);
    }

    private static class LoadProgressCallback {

        LlamaContext context;

        public LoadProgressCallback(LlamaContext context) {
            this.context = context;
        }

        void onLoadProgress(int progress) {
            context.emitLoadProgress(progress);
        }
    }

    private void emitPartialCompletion(JSObject tokenResult) {
        //        JSObject ret = new JSObject();
        //        ret.put("contextId", this.id);
        //        ret.put("tokenResult", tokenResult);
        //        notifyListeners("onToken", ret);
    }

    // public WritableMap loadSession(String path) {
    //   if (path == null || path.isEmpty()) {
    //     throw new IllegalArgumentException("File path is empty");
    //   }
    //   File file = new File(path);
    //   if (!file.exists()) {
    //     throw new IllegalArgumentException("File does not exist: " + path);
    //   }
    //   WritableMap result = loadSession(this.context, path);
    //   if (result.hasKey("error")) {
    //     throw new IllegalStateException(result.getString("error"));
    //   }
    //   return result;
    // }

    // public int saveSession(String path, int size) {
    //   if (path == null || path.isEmpty()) {
    //     throw new IllegalArgumentException("File path is empty");
    //   }
    //   return saveSession(this.context, path, size);
    // }

    public JSObject completion(JSObject params, PartialCompletionCallback callback) {
        if (!params.has("prompt")) {
            throw new IllegalArgumentException("Missing required parameter: prompt");
        }
        JSObject result = doCompletion(this.context, params, callback);
        if (result.has("error")) {
            throw new IllegalStateException(result.getString("error"));
        }
        return result;
    }

    public void stopCompletion() {
        stopCompletion(this.context);
    }

    // public boolean isPredicting() {
    //   return isPredicting(this.context);
    // }

    public JSObject tokenize(String text) {
        JSObject result = new JSObject();
        result.put("tokens", tokenize(this.context, text, new String[0]));
        return result;
    }

    public String detokenize(JSObject params) {
        try {
            JSONArray tokens = params.getJSONArray("tokens");
            int[] toks = new int[tokens.length()];
            for (int i = 0; i < tokens.length(); i++) {
                toks[i] = (int) tokens.getDouble(i);
            }
            return detokenize(this.context, toks);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSObject getVocab() {
        JSObject result = new JSObject();
        result.put("vocab", getVocab(this.context));
        return result;
    }

    // public WritableMap getEmbedding(String text, ReadableMap params) {
    //   if (isEmbeddingEnabled(this.context) == false) {
    //     throw new IllegalStateException("Embedding is not enabled");
    //   }
    //   WritableMap result = embedding(
    //     this.context,
    //     text,
    //     // int embd_normalize,
    //     params.hasKey("embd_normalize") ? params.getInt("embd_normalize") : -1
    //   );
    //   if (result.hasKey("error")) {
    //     throw new IllegalStateException(result.getString("error"));
    //   }
    //   return result;
    // }

    // public String bench(int pp, int tg, int pl, int nr) {
    //   return bench(this.context, pp, tg, pl, nr);
    // }

    // public int applyLoraAdapters(ReadableArray loraAdapters) {
    //   int result = applyLoraAdapters(this.context, loraAdapters);
    //   if (result != 0) {
    //     throw new IllegalStateException("Failed to apply lora adapters");
    //   }
    //   return result;
    // }

    // public void removeLoraAdapters() {
    //   removeLoraAdapters(this.context);
    // }

    // public WritableArray getLoadedLoraAdapters() {
    //   return getLoadedLoraAdapters(this.context);
    // }

    public void release() {
        freeContext(context);
    }

    static {
        Log.d(NAME, "Primary ABI: " + Build.SUPPORTED_ABIS[0]);

        String cpuFeatures = LlamaContext.getCpuFeatures();
        Log.d(NAME, "CPU features: " + cpuFeatures);
        boolean hasFp16 = cpuFeatures.contains("fp16") || cpuFeatures.contains("fphp");
        boolean hasDotProd = cpuFeatures.contains("dotprod") || cpuFeatures.contains("asimddp");
        boolean hasSve = cpuFeatures.contains("sve");
        boolean hasI8mm = cpuFeatures.contains("i8mm");
        boolean isAtLeastArmV82 = cpuFeatures.contains("asimd") && cpuFeatures.contains("crc32") && cpuFeatures.contains("aes");
        boolean isAtLeastArmV84 = cpuFeatures.contains("dcpop") && cpuFeatures.contains("uscat");
        Log.d(NAME, "- hasFp16: " + hasFp16);
        Log.d(NAME, "- hasDotProd: " + hasDotProd);
        Log.d(NAME, "- hasSve: " + hasSve);
        Log.d(NAME, "- hasI8mm: " + hasI8mm);
        Log.d(NAME, "- isAtLeastArmV82: " + isAtLeastArmV82);
        Log.d(NAME, "- isAtLeastArmV84: " + isAtLeastArmV84);

        // TODO: Add runtime check for cpu features
        if (LlamaContext.isArm64V8a()) {
            // if (hasDotProd && hasI8mm) {
            //     Log.d(NAME, "Loading libcapllama_v8_2_dotprod_i8mm.so");
            //     System.loadLibrary("capllama_v8_2_dotprod_i8mm");
            //     loadedLibrary = "capllama_v8_2_dotprod_i8mm";
            // } else if (hasDotProd) {
            //     Log.d(NAME, "Loading libcapllama_v8_2_dotprod.so");
            //     System.loadLibrary("capllama_v8_2_dotprod");
            //     loadedLibrary = "capllama_v8_2_dotprod";
            // } else if (hasI8mm) {
            //     Log.d(NAME, "Loading libcapllama_v8_2_i8mm.so");
            //     System.loadLibrary("capllama_v8_2_i8mm");
            //     loadedLibrary = "capllama_v8_2_i8mm";
            // } else if (hasFp16) {
            //     Log.d(NAME, "Loading libcapllama_v8_2.so");
            //     System.loadLibrary("capllama_v8_2");
            //     loadedLibrary = "capllama_v8_2";
            // } else {
            //     Log.d(NAME, "Loading default libcapllama_v8.so");
            //     System.loadLibrary("capllama_v8");
            //     loadedLibrary = "capllama_v8";
            // }
            if (hasFp16 && isAtLeastArmV84) {
                Log.d(NAME, "Loading libcapllama_v8_2_dotprod_i8mm.so with runtime feature detection");
                System.loadLibrary("capllama_jni_v8_2_dotprod_i8mm");
                loadedLibrary = "capllama_jni_v8_2_dotprod_i8mm";
            } else {
                Log.d(NAME, "Loading default libcapllama_v8.so");
                System.loadLibrary("capllama_jni_v8");
                loadedLibrary = "capllama_jni_v8";
            }
        } else if (LlamaContext.isX86_64()) {
            Log.d(NAME, "Loading libcapllama_x86_64.so");
            System.loadLibrary("capllama_jni_x86_64");
            loadedLibrary = "capllama_jni_x86_64";
        } else {
            Log.d(NAME, "ARM32 is not supported, skipping loading library");
        }
    }

    private static boolean isArm64V8a() {
        return Build.SUPPORTED_ABIS[0].equals("arm64-v8a");
    }

    private static boolean isX86_64() {
        return Build.SUPPORTED_ABIS[0].equals("x86_64");
    }

    private static boolean isArchNotSupported() {
        return isArm64V8a() == false && isX86_64() == false;
    }

    private static String getCpuFeatures() {
        File file = new File("/proc/cpuinfo");
        StringBuilder stringBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("Features")) {
                    stringBuilder.append(line);
                    break;
                }
            }
            bufferedReader.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            Log.w(NAME, "Couldn't read /proc/cpuinfo", e);
            return "";
        }
    }

    // protected static native WritableMap modelInfo(
    //   String model,
    //   String[] skip
    // );
    protected static native JSObject initContext(JSObject params, LoadProgressCallback loadProgressCallback);

    protected static native void interruptLoad(long contextPtr);

    protected static native JSObject loadModelDetails(long contextPtr);

    protected static native JSObject getFormattedChatWithJinja(long contextPtr, String messages, String chatTemplate, JSObject params);

    protected static native String getFormattedChat(long contextPtr, String messages, String chatTemplate);

    // protected static native WritableMap loadSession(
    //   long contextPtr,
    //   String path
    // );
    // protected static native int saveSession(
    //   long contextPtr,
    //   String path,
    //   int size
    // );
    protected static native JSObject doCompletion(long context_ptr, JSObject params, PartialCompletionCallback partial_completion_callback);

    protected static native void stopCompletion(long contextPtr);

    // protected static native boolean isPredicting(long contextPtr);
    protected static native JSArray tokenize(long contextPtr, String text, String[] media_paths);

    protected static native String detokenize(long contextPtr, int[] tokens);

    protected static native JSArray getVocab(long contextPtr);

    // protected static native boolean isEmbeddingEnabled(long contextPtr);
    // protected static native WritableMap embedding(
    //   long contextPtr,
    //   String text,
    //   int embd_normalize
    // );
    // protected static native String bench(long contextPtr, int pp, int tg, int pl, int nr);
    // protected static native int applyLoraAdapters(long contextPtr, ReadableArray loraAdapters);
    // protected static native void removeLoraAdapters(long contextPtr);
    // protected static native WritableArray getLoadedLoraAdapters(long contextPtr);
    protected static native void freeContext(long contextPtr);
    // protected static native void setupLog(NativeLogCallback logCallback);
    // protected static native void unsetLog();
}
