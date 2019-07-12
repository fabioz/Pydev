package org.python.pydev.core;

public interface IPyEditOfflineActionListener {

    public void addOfflineActionListener(String key, /* IAction */ Object action, String description,
            boolean needsEnter);

}
