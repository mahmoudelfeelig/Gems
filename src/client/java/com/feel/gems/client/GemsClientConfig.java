package com.feel.gems.client;


public final class GemsClientConfig {
    public ControlMode controlMode = ControlMode.CHORD;

    public enum ControlMode {
        CHORD,
        CUSTOM
    }
}
