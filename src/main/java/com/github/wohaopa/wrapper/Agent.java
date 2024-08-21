package com.github.wohaopa.wrapper;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class Agent {

    public static Logger log = Logger.getLogger(Tags.Name);

    public static void premain(String agentArgs, Instrumentation inst) {
        log.info(agentArgs);

    }
}
