package com.capllama;

import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.concurrent.CompletableFuture;

@CapacitorPlugin(name = "CapacitorLlama")
public class CapacitorLlamaPlugin extends Plugin {

    private CapacitorLlama implementation = new CapacitorLlama();

    @PluginMethod
    public void initContext(PluginCall call) {
        Integer id = call.getInt("id", -1);
        CompletableFuture<JSObject> future = implementation.initContext(id, call.getData());
        future.thenAccept(call::resolve);
    }

    private class PartialCompletionCallbackWithListeners implements PartialCompletionCallback {

        int contextId;
        boolean emitNeeded;

        public PartialCompletionCallbackWithListeners(int contextId, boolean emitNeeded) {
            this.contextId = contextId;
            this.emitNeeded = emitNeeded;
        }

        public void onPartialCompletion(JSObject tokenResult) {
            if (this.emitNeeded) {
                JSObject ret = new JSObject();
                ret.put("contextId", this.contextId);
                ret.put("tokenResult", tokenResult);
                notifyListeners("onToken", ret);
            }
        }
    }

    @PluginMethod
    public void completion(PluginCall call) {
        JSObject params = call.getData();
        JSObject innerParams = params.getJSObject("params");
        CompletableFuture<JSObject> future = implementation.completion(
            params,
            new PartialCompletionCallbackWithListeners(params.optInt("id", 0), innerParams.optBoolean("emit_partial_completion", false))
        );
        future.thenAccept(call::resolve);
    }

    @PluginMethod
    public void getFormattedChat(PluginCall call) {
        CompletableFuture<JSObject> future = implementation.getFormattedChat(call.getData());
        future.thenAccept(call::resolve);
    }

    @PluginMethod
    public void releaseAllContexts(PluginCall call) {
        CompletableFuture<Void> future = implementation.releaseAllContexts();
        future.thenAccept((v) -> call.resolve());
    }

    @PluginMethod
    public void releaseContext(PluginCall call) {
        Integer id = call.getInt("id", -1);
        CompletableFuture<Void> future = implementation.releaseContext(id, call.getData());
        future.thenAccept((v) -> call.resolve());
    }

    @PluginMethod
    public void stopCompletion(PluginCall call) {
        Integer id = call.getInt("id", -1);
        CompletableFuture<Void> future = implementation.stopCompletion(id);
        future.thenAccept((v) -> call.resolve());
    }

    @PluginMethod
    public void detokenize(PluginCall call) {
        CompletableFuture<JSObject> future = implementation.detokenize(call.getData());
        future.thenAccept(call::resolve);
    }

    @PluginMethod
    public void tokenize(PluginCall call) {
        CompletableFuture<JSObject> future = implementation.tokenize(call.getData());
        future.thenAccept(call::resolve);
    }

    @PluginMethod
    public void getVocab(PluginCall call) {
        CompletableFuture<JSObject> future = implementation.getVocab(call.getData());
        future.thenAccept(call::resolve);
    }
}
