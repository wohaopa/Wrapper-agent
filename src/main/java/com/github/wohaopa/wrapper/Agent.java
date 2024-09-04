package com.github.wohaopa.wrapper;

import java.lang.instrument.Instrumentation;

import com.github.wohaopa.wrapper.transformer.WrapperTransformer;
import com.github.wohaopa.wrapper.window.MainGUI;

public class Agent {

    public static void main(String[] args) {
        Config.loadConfig();
        MainGUI.main(args);
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        if (agentArgs != null && agentArgs.equals("debug")) {
            Config.DEBUG = true;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Config.loadConfig();
        MainGUI.main(null);
        registerTransformer(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        Config.loadConfig();
        registerTransformer(inst);
    }

    private static void registerTransformer(Instrumentation inst) {
        inst.addTransformer(new WrapperTransformer());
    }
}
