package com.capllama;

import android.content.res.AssetManager;
import android.os.Build;

import android.util.Log;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.StringBuilder;
import java.util.HashMap;
import java.util.Map;
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

    // private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;

    public LlamaContext(int id, JSObject params/* ReactApplicationContext reactContext, ReadableMap params */) {
        // if (LlamaContext.isArchNotSupported()) {
        //   throw new IllegalStateException("Only 64-bit architectures are supported");
        // }
        if (!params.has("model")) {
            throw new IllegalArgumentException("Missing required parameter: model");
        }
        // eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        this.id = id;

        this.context = initContext(
            // String model,
            params.getString("model"),
            // String chat_template,
            params.getString("chat_template", ""),
            // String reasoning_format,
            params.getString("reasoning_format", "none"),
            // boolean embedding,
            params.getBoolean("embedding", false),
            // int embd_normalize,
            params.getInteger("embd_normalize", -1),
            // int n_ctx,
            params.getInteger("n_ctx", 512),
            // int n_batch,
            params.getInteger("n_batch", 512),
            // int n_ubatch,
            params.getInteger("n_ubatch", 512),
            // int n_threads,
            params.getInteger("n_threads", 0),
            // int n_gpu_layers, // TODO: Support this
            params.getInteger("n_gpu_layers", 0),
            // boolean flash_attn,
            params.getBoolean("flash_attn", false),
            // String cache_type_k,
            params.getString("cache_type_k", "f16"),
            // String cache_type_v,
            params.getString("cache_type_v", "f16"),
            // boolean use_mlock,
            params.getBoolean("use_mlock", true),
            // boolean use_mmap,
            params.getBoolean("use_mmap", true),
            //boolean vocab_only,
            params.getBoolean("vocab_only", false),
            // String lora,
            params.getString("lora", ""),
            // float lora_scaled,
            (float) params.optDouble("lora_scaled", 1.0f),
            // ReadableArray lora_adapters,
            // params.getArray("lora_list", null),
            null,
            // float rope_freq_base,
            (float) params.optDouble("rope_freq_base", 0.0f),
            // float rope_freq_scale
            (float) params.optDouble("rope_freq_scale", 0.0f),
            // int pooling_type,
            params.getInteger("pooling_type", -1),
            // LoadProgressCallback load_progress_callback
            // params.hasKey("use_progress_callback") ? new LoadProgressCallback(this) : null
            null
        );
        if (this.context == -1) {
            throw new IllegalStateException("Failed to initialize context");
        }
        this.modelDetails = loadModelDetails(this.context);
        // this.reactContext = reactContext;
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
        String jsonSchema = params.getString("json_schema", "");
        String tools = params.getString("tools", "");
        Boolean parallelToolCalls = params.getBoolean("parallel_tool_calls", false);
        String toolChoice = params.getString("tool_choice", "");
        return getFormattedChatWithJinja(
            this.context,
            messages,
            chatTemplate == null ? "" : chatTemplate,
            jsonSchema,
            tools,
            parallelToolCalls,
            toolChoice
        );
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
        // WritableMap event = Arguments.createMap();
        // event.putInt("contextId", LlamaContext.this.id);
        // event.putMap("tokenResult", tokenResult);
        // eventEmitter.emit("@RNLlama_onToken", event);
    }

    private static class PartialCompletionCallback {

        LlamaContext context;
        boolean emitNeeded;

        public PartialCompletionCallback(LlamaContext context, boolean emitNeeded) {
            this.context = context;
            this.emitNeeded = emitNeeded;
        }

        void onPartialCompletion(JSObject tokenResult) {
            if (this.emitNeeded) {
                context.emitPartialCompletion(tokenResult);
            }
        }
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

    public JSObject completion(JSObject params) {
        if (!params.has("prompt")) {
            throw new IllegalArgumentException("Missing required parameter: prompt");
        }
        String[] stop = new String[0];
        String[] dry_sequence_breakers = new String[] { "\n", ":", "\"", "*" };
        double[][] logit_bias = new double[0][0];
        try {
            if (params.has("logit_bias")) {
                JSONArray logit_bias_array = params.getJSONArray("logit_bias");
                logit_bias = new double[logit_bias_array.length()][];
                for (int i = 0; i < logit_bias_array.length(); i++) {
                    JSONArray logit_bias_row = logit_bias_array.getJSONArray(i);
                    logit_bias[i] = new double[logit_bias_row.length()];
                    for (int j = 0; j < logit_bias_row.length(); j++) {
                        logit_bias[i][j] = logit_bias_row.getDouble(j);
                    }
                }
            }

            if (params.has("stop")) {
                JSONArray jsArr = params.getJSONArray("stop");
                stop = new String[jsArr.length()];
                for (int i = 0; i < jsArr.length(); i++) {
                    stop[i] = jsArr.getString(i);
                }
            }

            if (params.has("dry_sequence_breakers")) {
                JSONArray jsArr = params.getJSONArray("dry_sequence_breakers");
                dry_sequence_breakers = new String[jsArr.length()];
                for (int i = 0; i < jsArr.length(); i++) {
                    dry_sequence_breakers[i] = jsArr.getString(i);
                }
            }

            JSObject result = doCompletion(
                this.context,
                // String prompt,
                params.getString("prompt"),
                // int chat_format,
                params.getInteger("chat_format", 0),
                // String grammar,
                params.getString("grammar", ""),
                // String json_schema,
                params.getString("json_schema", ""),
                // boolean grammar_lazy,
                params.getBoolean("grammar_lazy", false),
                // ReadableArray grammar_triggers,
                //       params.hasKey("grammar_triggers") ? params.getArray("grammar_triggers") : null,
                null,
                // ReadableArray preserved_tokens,
                //       params.hasKey("preserved_tokens") ? params.getArray("preserved_tokens") : null,
                null,
                // float temperature,
                (float) params.optDouble("temperature", 0.7f),
                // int n_threads,
                params.getInteger("n_threads", 0),
                // int n_predict,
                params.getInteger("n_predict", -1),
                // int n_probs,
                params.getInteger("n_probs", 0),
                // int penalty_last_n,
                params.getInteger("penalty_last_n", 64),
                // float penalty_repeat,
                (float) params.optDouble("penalty_repeat", 1.00f),
                // float penalty_freq,
                (float) params.optDouble("penalty_freq", 0.00f),
                // float penalty_present,
                (float) params.optDouble("penalty_present", 0.00f),
                // float mirostat,
                (float) params.optDouble("mirostat", 0.00f),
                // float mirostat_tau,
                (float) params.optDouble("mirostat_tau", 5.00f),
                // float mirostat_eta,
                (float) params.optDouble("mirostat_eta", 0.10f),
                // int top_k,
                params.getInteger("top_k", 40),
                // float top_p,
                (float) params.optDouble("top_p", 0.95f),
                // float min_p,
                (float) params.optDouble("min_p", 0.05f),
                // float xtc_threshold,
                (float) params.optDouble("xtc_threshold", 0.00f),
                // float xtc_probability,
                (float) params.optDouble("xtc_probability", 0.00f),
                // float typical_p,
                (float) params.optDouble("typical_p", 1.00f),
                // int seed,
                params.getInteger("seed", -1),
                // String[] stop,
                stop,
                // boolean ignore_eos,
                params.getBoolean("ignore_eos", false),
                // double[][] logit_bias,
                logit_bias,
                // float dry_multiplier,
                (float) params.optDouble("dry_multiplier", 0.00f),
                // float dry_base,
                (float) params.optDouble("dry_base", 1.75f),
                // int dry_allowed_length,
                params.getInteger("dry_allowed_length", 2),
                // int dry_penalty_last_n,
                params.getInteger("dry_penalty_last_n", -1),
                // float top_n_sigma,
                (float) params.optDouble("top_n_sigma", -1.0f),
                // String[] dry_sequence_breakers, when undef, we use the default definition from common.h
                dry_sequence_breakers,
                // PartialCompletionCallback partial_completion_callback
                new PartialCompletionCallback(this, params.getBoolean("emit_partial_completion", false))
            );
            if (result.has("error")) {
                throw new IllegalStateException(result.getString("error"));
            }
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopCompletion() {
      stopCompletion(this.context);
    }

    // public boolean isPredicting() {
    //   return isPredicting(this.context);
    // }

    // public WritableMap tokenize(String text) {
    //   WritableMap result = Arguments.createMap();
    //   result.putArray("tokens", tokenize(this.context, text));
    //   return result;
    // }

    // public String detokenize(ReadableArray tokens) {
    //   int[] toks = new int[tokens.size()];
    //   for (int i = 0; i < tokens.size(); i++) {
    //     toks[i] = (int) tokens.getDouble(i);
    //   }
    //   return detokenize(this.context, toks);
    // }

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
            if (hasDotProd && hasI8mm) {
                Log.d(NAME, "Loading librnllama_v8_2_dotprod_i8mm.so");
                System.loadLibrary("rnllama_v8_2_dotprod_i8mm");
                loadedLibrary = "rnllama_v8_2_dotprod_i8mm";
            } else if (hasDotProd) {
                Log.d(NAME, "Loading librnllama_v8_2_dotprod.so");
                System.loadLibrary("rnllama_v8_2_dotprod");
                loadedLibrary = "rnllama_v8_2_dotprod";
            } else if (hasI8mm) {
                Log.d(NAME, "Loading librnllama_v8_2_i8mm.so");
                System.loadLibrary("rnllama_v8_2_i8mm");
                loadedLibrary = "rnllama_v8_2_i8mm";
            } else if (hasFp16) {
                Log.d(NAME, "Loading librnllama_v8_2.so");
                System.loadLibrary("rnllama_v8_2");
                loadedLibrary = "rnllama_v8_2";
            } else {
                Log.d(NAME, "Loading default librnllama_v8.so");
                System.loadLibrary("rnllama_v8");
                loadedLibrary = "rnllama_v8";
            }
            //  Log.d(NAME, "Loading librnllama_v8_7.so with runtime feature detection");
            //  System.loadLibrary("rnllama_v8_7");
        } else if (LlamaContext.isX86_64()) {
            Log.d(NAME, "Loading librnllama_x86_64.so");
            System.loadLibrary("rnllama_x86_64");
            loadedLibrary = "rnllama_x86_64";
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
    protected static native long initContext(
        String model,
        String chat_template,
        String reasoning_format,
        boolean embedding,
        int embd_normalize,
        int n_ctx,
        int n_batch,
        int n_ubatch,
        int n_threads,
        int n_gpu_layers, // TODO: Support this
        boolean flash_attn,
        String cache_type_k,
        String cache_type_v,
        boolean use_mlock,
        boolean use_mmap,
        boolean vocab_only,
        String lora,
        float lora_scaled,
        // TODO: this type is wrong
        // ReadableArray lora_list,
        int[] lora_list,
        float rope_freq_base,
        float rope_freq_scale,
        int pooling_type,
        LoadProgressCallback load_progress_callback
    );

    protected static native void interruptLoad(long contextPtr);

    protected static native JSObject loadModelDetails(long contextPtr);

    protected static native JSObject getFormattedChatWithJinja(
        long contextPtr,
        String messages,
        String chatTemplate,
        String jsonSchema,
        String tools,
        boolean parallelToolCalls,
        String toolChoice
    );

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
    protected static native JSObject doCompletion(
        long context_ptr,
        String prompt,
        int chat_format,
        String grammar,
        String json_schema,
        boolean grammar_lazy,
        JSArray grammar_triggers,
        JSArray preserved_tokens,
        float temperature,
        int n_threads,
        int n_predict,
        int n_probs,
        int penalty_last_n,
        float penalty_repeat,
        float penalty_freq,
        float penalty_present,
        float mirostat,
        float mirostat_tau,
        float mirostat_eta,
        int top_k,
        float top_p,
        float min_p,
        float xtc_threshold,
        float xtc_probability,
        float typical_p,
        int seed,
        String[] stop,
        boolean ignore_eos,
        double[][] logit_bias,
        float dry_multiplier,
        float dry_base,
        int dry_allowed_length,
        int dry_penalty_last_n,
        float top_n_sigma,
        String[] dry_sequence_breakers,
        PartialCompletionCallback partial_completion_callback
    );
    protected static native void stopCompletion(long contextPtr);
    // protected static native boolean isPredicting(long contextPtr);
    // protected static native WritableArray tokenize(long contextPtr, String text);
    // protected static native String detokenize(long contextPtr, int[] tokens);
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
