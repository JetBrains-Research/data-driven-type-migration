package org.jetbrains.research.ddtm.ide.migration.collectors;

public abstract class SwitchableCollector {
    protected boolean shouldCollect;

    public void on() {
        shouldCollect = true;
    }

    public void off() {
        shouldCollect = false;
    }

}
