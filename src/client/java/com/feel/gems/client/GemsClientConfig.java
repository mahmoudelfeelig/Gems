package com.feel.gems.client;


public final class GemsClientConfig {
    public ControlMode controlMode = ControlMode.CHORD;
    public boolean passivesEnabled = true;

    public enum ControlMode {
        CHORD,
        CUSTOM
    }
}
