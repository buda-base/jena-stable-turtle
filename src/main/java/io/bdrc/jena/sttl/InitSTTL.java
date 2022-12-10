package io.bdrc.jena.sttl;

import org.apache.jena.sys.JenaSubsystemLifecycle;

public class InitSTTL implements JenaSubsystemLifecycle {
    @Override
    public void start() {
        STTLWriter.registerWriter();
        STriGWriter.registerWriter();
    }

    @Override
    public void stop() {
        // cannot be undone
    }
}
