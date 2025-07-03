package com.capllama;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "CapacitorLlama")
public class CapacitorLlamaPlugin extends Plugin {

    private CapacitorLlama implementation = new CapacitorLlama();

    @PluginMethod
    public void initContext(PluginCall call) {
        Integer id = call.getInt("id", -1);
        JSObject res = implementation.initContext(id, call.getData());
        call.resolve(res);
    }

    @PluginMethod
    public void completion(PluginCall call) {
        JSObject res = implementation.completion(call.getData());
        call.resolve(res);
    }

    @PluginMethod
    public void getFormattedChat(PluginCall call) {
        JSObject res = implementation.getFormattedChat(call.getData());
        call.resolve(res);
    }

    @PluginMethod
    public void releaseAllContexts(PluginCall call) {
        implementation.releaseAllContexts();
        call.resolve();
    }
    
    @PluginMethod
    public void releaseContext(PluginCall call) {
        Integer id = call.getInt("id", -1);
        implementation.releaseContext(id, call.getData());
        call.resolve();
    }

    @PluginMethod
    public void stopCompletion(PluginCall call) {
        Integer id = call.getInt("id", -1);
        implementation.stopCompletion(id);
        call.resolve();
    }

    @PluginMethod
    public void detokenize(PluginCall call) {
        JSObject res = implementation.detokenize(call.getData());
        call.resolve(res);
    }

    @PluginMethod
    public void tokenize(PluginCall call) {
        JSObject res = implementation.tokenize(call.getData());
        call.resolve(res);
    }
}
