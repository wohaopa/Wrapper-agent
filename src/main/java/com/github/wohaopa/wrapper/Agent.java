package com.github.wohaopa.wrapper;

import java.lang.instrument.Instrumentation;

import com.github.wohaopa.wrapper.mc.transformer.WrapperTransformer;
import com.github.wohaopa.wrapper.ui.window.MainGUI;

public class Agent {

    public static Instrumentation inst;

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
        MainGUI.display(Config.DEBUG);
        registerTransformer(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        Config.loadConfig();
        MainGUI.display(false);
        registerTransformer(inst);
    }

    private static void registerTransformer(Instrumentation inst) {
        inst.addTransformer(new WrapperTransformer());
    }
}
