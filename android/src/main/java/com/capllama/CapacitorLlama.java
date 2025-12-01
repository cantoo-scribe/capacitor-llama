package com.capllama;

import android.util.Log;
import com.getcapacitor.JSObject;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.JSONArray;

public class CapacitorLlama {

    public static final String NAME = "CapacitorLlama";

    public CapacitorLlama() {}

    // TODO: release the executor service on destroy
    // executorService.shutdown()
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private HashMap<Integer, LlamaContext> contexts = new HashMap<>();

    // public void toggleNativeLog(boolean enabled, Promise promise) {
    //   new AsyncTask<Void, Void, Boolean>() {
    //     private Exception exception;

    //     @Override
    //     protected Boolean doInBackground(Void... voids) {
    //       try {
    //         LlamaContext.toggleNativeLog(reactContext, enabled);
    //         return true;
    //       } catch (Exception e) {
    //         exception = e;
    //       }
    //       return null;
    //     }

    //     @Override
    //     protected void onPostExecute(Boolean result) {
    //       if (exception != null) {
    //         promise.reject(exception);
    //         return;
    //       }
    //       promise.resolve(result);
    //     }
    //   }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    // }

    private int llamaContextLimit = -1;

    // public void setContextLimit(double limit, Promise promise) {
    //   llamaContextLimit = (int) limit;
    //   promise.resolve(null);
    // }

    // public void modelInfo(final String model, final ReadableArray skip, final Promise promise) {
    //   new AsyncTask<Void, Void, WritableMap>() {
    //     private Exception exception;

    //     @Override
    //     protected WritableMap doInBackground(Void... voids) {
    //       try {
    //         String[] skipArray = new String[skip.size()];
    //         for (int i = 0; i < skip.size(); i++) {
    //           skipArray[i] = skip.getString(i);
    //         }
    //         return LlamaContext.modelInfo(model, skipArray);
    //       } catch (Exception e) {
    //         exception = e;
    //       }
    //       return null;
    //     }

    //     @Override
    //     protected void onPostExecute(WritableMap result) {
    //       if (exception != null) {
    //         promise.reject(exception);
    //         return;
    //       }
    //       promise.resolve(result);
    //     }
    //   }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    // }

    public CompletableFuture<JSObject> initContext(double id, final JSObject params) {
        final int contextId = (int) id;
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    LlamaContext context = contexts.get(contextId);
                    if (context != null) {
                        throw new Exception("Context already exists");
                    }
                    if (llamaContextLimit > -1 && contexts.size() >= llamaContextLimit) {
                        throw new Exception("Context limit reached");
                    }
                    LlamaContext llamaContext = new LlamaContext(contextId, params);
                    if (llamaContext.getContext() == 0) {
                        throw new Exception("Failed to initialize context");
                    }
                    contexts.put(contextId, llamaContext);
                    JSObject result = new JSObject();
                    // result.put("contextId", contextId);
                    result.put("gpu", false);
                    result.put("reasonNoGPU", "Currently not supported");
                    result.put("model", llamaContext.getModelDetails());
                    // result.put("androidLib", llamaContext.getLoadedLibrary());
                    return result;
                } catch (Exception e) {
                    return null;
                }
            },
            executor
        );
    }

    public CompletableFuture<JSObject> getFormattedChat(final JSObject params) {
        final int contextId = (int) params.getInteger("id", -1);
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    LlamaContext context = contexts.get(contextId);
                    String messages = params.getString("messages", "");
                    String chatTemplate = params.getString("chatTemplate", "");
                    if (context == null) {
                        throw new Exception("Context not found");
                    }

                    if (params.getBoolean("jinja", false)) {
                        JSObject result = context.getFormattedChatWithJinja(messages, chatTemplate, params);
                        if (result.has("_error")) {
                            throw new Exception(result.getString("_error", "error"));
                        }
                        return result;
                    }
                    JSObject result = new JSObject();
                    String formattedChat = context.getFormattedChat(messages, chatTemplate);
                    result.put("prompt", formattedChat);
                    return result;
                } catch (Exception e) {
                    Log.e("err", "getFormattedChat: ", e);
                    return null;
                }
            },
            executor
        );
    }

    // public void loadSession(double id, final String path, Promise promise) {
    //   final int contextId = (int) id;
    //   AsyncTask task = new AsyncTask<Void, Void, WritableMap>() {
    //     private Exception exception;

    //     @Override
    //     protected WritableMap doInBackground(Void... voids) {
    //       try {
    //         LlamaContext context = contexts.get(contextId);
    //         if (context == null) {
    //           throw new Exception("Context not found");
    //         }
    //         WritableMap result = context.loadSession(path);
    //         return result;
    //       } catch (Exception e) {
    //         exception = e;
    //       }
    //       return null;
    //     }

    //     @Override
    //     protected void onPostExecute(WritableMap result) {
    //       if (exception != null) {
    //         promise.reject(exception);
    //         return;
    //       }
    //       promise.resolve(result);
    //       tasks.remove(this);
    //     }
    //   }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    //   tasks.put(task, "loadSession-" + contextId);
    // }

    // public void saveSession(double id, final String path, double size, Promise promise) {
    //   final int contextId = (int) id;
    //   AsyncTask task = new AsyncTask<Void, Void, Integer>() {
    //     private Exception exception;

    //     @Override
    //     protected Integer doInBackground(Void... voids) {
    //       try {
    //         LlamaContext context = contexts.get(contextId);
    //         if (context == null) {
    //           throw new Exception("Context not found");
    //         }
    //         Integer count = context.saveSession(path, (int) size);
    //         return count;
    //       } catch (Exception e) {
    //         exception = e;
    //       }
    //       return -1;
    //     }

    //     @Override
    //     protected void onPostExecute(Integer result) {
    //       if (exception != null) {
    //         promise.reject(exception);
    //         return;
    //       }
    //       promise.resolve(result);
    //       tasks.remove(this);
    //     }
    //   }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    //   tasks.put(task, "saveSession-" + contextId);
    // }

    public CompletableFuture<JSObject> completion(final JSObject params, PartialCompletionCallback callback) {
        final int contextId = (int) params.getInteger("id", -1);
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    LlamaContext context = contexts.get(contextId);
                    if (context == null) {
                        throw new Exception("Context not found");
                    }
                    //      if (context.isPredicting()) {
                    //        throw new Exception("Context is busy");
                    //      }
                    // TODO: implement default parameters
                    JSObject completionParams = params.getJSObject("params");
                    assert completionParams != null;
                    JSONArray messages = completionParams.optJSONArray("messages");

                    completionParams.put("emit_partial_completion", false);
                    if (messages != null) {
                        String prompt = context.getFormattedChat(messages.toString(), "");
                        Log.d("debug", "completion prompt: " + prompt);
                        completionParams.put("prompt", prompt);
                    }
                    JSObject result = context.completion(completionParams, callback);
                    return result;
                } catch (Exception e) {
                    Log.e(NAME, "Failed completion", e);
                    return null;
                }
            },
            executor
        );
    }

    public CompletableFuture<Void> stopCompletion(double id) {
        final int contextId = (int) id;
        return CompletableFuture.runAsync(
            () -> {
                try {
                    LlamaContext context = contexts.get(contextId);
                    if (context == null) {
                        throw new Exception("Context not found");
                    }
                    context.stopCompletion();
                } catch (Exception e) {}
            },
            executor
        );
    }

    public CompletableFuture<JSObject> tokenize(final JSObject params) {
        final int contextId = (int) params.getInteger("id", -1);
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    LlamaContext context = contexts.get(contextId);
                    if (context == null) {
                        throw new Exception("Context not found");
                    }
                    String text = params.getString("text");
                    return context.tokenize(text);
                } catch (Exception e) {
                    Log.e(NAME, "Failed tokenize", e);
                    return null;
                }
            },
            executor
        );
    }

    public CompletableFuture<JSObject> detokenize(final JSObject params) {
        final int contextId = (int) params.getInteger("id", -1);
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    LlamaContext context = contexts.get(contextId);
                    if (context == null) {
                        throw new Exception("Context not found");
                    }
                    String text = context.detokenize(params);
                    JSObject result = new JSObject();
                    result.put("text", text);
                    return result;
                } catch (Exception e) {
                    Log.e(NAME, "Failed detokenize", e);
                    return null;
                }
            },
            executor
        );
    }

    public CompletableFuture<JSObject> getVocab(final JSObject params) {
        final int contextId = (int) params.getInteger("id", -1);
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    LlamaContext context = contexts.get(contextId);
                    if (context == null) {
                        throw new Exception("Context not found");
                    }
                    return context.getVocab();
                } catch (Exception e) {
                    Log.e(NAME, "Failed tokenize", e);
                    return null;
                }
            },
            executor
        );
    }

    // public void embedding(double id, final String text, final ReadableMap params, final Promise promise) {
    //   final int contextId = (int) id;
    //   AsyncTask task = new AsyncTask<Void, Void, WritableMap>() {
    //     private Exception exception;

    //     @Override
    //     protected WritableMap doInBackground(Void... voids) {
    //       try {
    //         LlamaContext context = contexts.get(contextId);
    //         if (context == null) {
    //           throw new Exception("Context not found");
    //         }
    //         return context.getEmbedding(text, params);
    //       } catch (Exception e) {
    //         exception = e;
    //       }
    //       return null;
    //     }

    //     @Override
    //     protected void onPostExecute(WritableMap result) {
    //       if (exception != null) {
    //         promise.reject(exception);
    //         return;
    //       }
    //       promise.resolve(result);
    //       tasks.remove(this);
    //     }
    //   }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    //   tasks.put(task, "embedding-" + contextId);
    // }

    // public void bench(double id, final double pp, final double tg, final double pl, final double nr, final Promise promise) {
    //   final int contextId = (int) id;
    //   AsyncTask task = new AsyncTask<Void, Void, String>() {
    //     private Exception exception;

    //     @Override
    //     protected String doInBackground(Void... voids) {
    //       try {
    //         LlamaContext context = contexts.get(contextId);
    //         if (context == null) {
    //           throw new Exception("Context not found");
    //         }
    //         return context.bench((int) pp, (int) tg, (int) pl, (int) nr);
    //       } catch (Exception e) {
    //         exception = e;
    //       }
    //       return null;
    //     }

    //     @Override
    //     protected void onPostExecute(String result) {
    //       if (exception != null) {
    //         promise.reject(exception);
    //         return;
    //       }
    //       promise.resolve(result);
    //       tasks.remove(this);
    //     }
    //   }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    //   tasks.put(task, "bench-" + contextId);
    // }

    // public void applyLoraAdapters(double id, final ReadableArray loraAdapters, final Promise promise) {
    //   final int contextId = (int) id;
    //   AsyncTask task = new AsyncTask<Void, Void, Void>() {
    //     private Exception exception;

    //     @Override
    //     protected Void doInBackground(Void... voids) {
    //       try {
    //         LlamaContext context = contexts.get(contextId);
    //         if (context == null) {
    //           throw new Exception("Context not found");
    //         }
    //         if (context.isPredicting()) {
    //           throw new Exception("Context is busy");
    //         }
    //         context.applyLoraAdapters(loraAdapters);
    //       } catch (Exception e) {
    //         exception = e;
    //       }
    //       return null;
    //     }

    //     @Override
    //     protected void onPostExecute(Void result) {
    //       if (exception != null) {
    //         promise.reject(exception);
    //         return;
    //       }
    //     }
    //   }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    //   tasks.put(task, "applyLoraAdapters-" + contextId);
    // }

    // public void removeLoraAdapters(double id, final Promise promise) {
    //   final int contextId = (int) id;
    //   AsyncTask task = new AsyncTask<Void, Void, Void>() {
    //     private Exception exception;

    //     @Override
    //     protected Void doInBackground(Void... voids) {
    //       try {
    //         LlamaContext context = contexts.get(contextId);
    //         if (context == null) {
    //           throw new Exception("Context not found");
    //         }
    //         if (context.isPredicting()) {
    //           throw new Exception("Context is busy");
    //         }
    //         context.removeLoraAdapters();
    //       } catch (Exception e) {
    //         exception = e;
    //       }
    //       return null;
    //     }

    //     @Override
    //     protected void onPostExecute(Void result) {
    //       if (exception != null) {
    //         promise.reject(exception);
    //         return;
    //       }
    //       promise.resolve(null);
    //     }
    //   }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    //   tasks.put(task, "removeLoraAdapters-" + contextId);
    // }

    // public void getLoadedLoraAdapters(double id, final Promise promise) {
    //   final int contextId = (int) id;
    //   AsyncTask task = new AsyncTask<Void, Void, ReadableArray>() {
    //     private Exception exception;

    //     @Override
    //     protected ReadableArray doInBackground(Void... voids) {
    //       try {
    //         LlamaContext context = contexts.get(contextId);
    //         if (context == null) {
    //           throw new Exception("Context not found");
    //         }
    //         return context.getLoadedLoraAdapters();
    //       } catch (Exception e) {
    //         exception = e;
    //       }
    //       return null;
    //     }

    //     @Override
    //     protected void onPostExecute(ReadableArray result) {
    //       if (exception != null) {
    //         promise.reject(exception);
    //         return;
    //       }
    //       promise.resolve(result);
    //     }
    //   }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    //   tasks.put(task, "getLoadedLoraAdapters-" + contextId);
    // }

    public CompletableFuture<Void> releaseContext(double id, final JSObject params) {
        final int contextId = (int) id;
        return CompletableFuture.runAsync(
            () -> {
                try {
                    LlamaContext context = contexts.get(contextId);
                    if (context == null) {
                        throw new Exception("Context " + id + " not found");
                    }
                    context.interruptLoad();
                    context.stopCompletion();
                    context.release();
                    contexts.remove(contextId);
                } catch (Exception e) {}
            },
            executor
        );
    }

    public CompletableFuture<Void> releaseAllContexts() {
        return CompletableFuture.runAsync(
            () -> {
                for (LlamaContext context : contexts.values()) {
                    context.stopCompletion();
                    context.release();
                }
                contexts.clear();
            },
            executor
        );
    }
    // @Override
    // public void onHostResume() {
    // }

    // @Override
    // public void onHostPause() {
    // }

    // @Override
    // public void onHostDestroy() {
    //   for (LlamaContext context : contexts.values()) {
    //     context.stopCompletion();
    //   }
    //   for (AsyncTask task : tasks.keySet()) {
    //     try {
    //       task.get();
    //     } catch (Exception e) {
    //       Log.e(NAME, "Failed to wait for task", e);
    //     }
    //   }
    //   tasks.clear();
    //   for (LlamaContext context : contexts.values()) {
    //     context.release();
    //   }
    //   contexts.clear();
    // }
}
