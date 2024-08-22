package com.github.wohaopa.wrapper;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

import com.github.wohaopa.wrapper.transformer.ModsDirFixTransformer;
import com.github.wohaopa.wrapper.transformer.ReplaceClassFileTransformer;

public class Agent {

    public static void main(String[] args) throws IOException {

    }

    public static void premain(String agentArgs, Instrumentation inst) {
        Config.loadConfig();
        registerTransformer(inst);

        if (agentArgs != null && agentArgs.equals("debug")) {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        Config.loadConfig();
        registerTransformer(inst);
    }

    private static void registerTransformer(Instrumentation inst) {
        inst.addTransformer(new ReplaceClassFileTransformer());
        if (Config.getNeedTransform() != null) inst.addTransformer(new ModsDirFixTransformer());
    }
}
