package org.python.pydev.ast.codecompletion.revisited;

public class DefaultSyncSystemModulesManagerScheduler {

    public static SyncSystemModulesManagerScheduler syncSystemModulesManagerScheduler = new SyncSystemModulesManagerScheduler();

    public static SyncSystemModulesManagerScheduler get() {
        return syncSystemModulesManagerScheduler;
    }
}
