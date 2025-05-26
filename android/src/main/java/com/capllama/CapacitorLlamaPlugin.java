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
        JSObject ret = implementation.initContext(id, call.getData());
        call.resolve(ret);
    }

    @PluginMethod
    public void completion(PluginCall call) {
        JSObject ret = implementation.completion(call.getData());
        call.resolve(ret);
    }

    @PluginMethod
    public void getFormattedChat(PluginCall call) {
        JSObject ret = implementation.getFormattedChat(call.getData());
        call.resolve(ret);
    }
}
