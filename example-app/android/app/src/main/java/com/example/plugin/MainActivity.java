package com.example.plugin;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import com.capllama.CapacitorLlamaPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(CapacitorLlamaPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
