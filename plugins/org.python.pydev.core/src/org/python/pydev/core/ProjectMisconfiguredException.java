package org.python.pydev.core;


public class ProjectMisconfiguredException extends MisconfigurationException{

    private static final long serialVersionUID = -1861437669380301862L;

    public ProjectMisconfiguredException(Throwable e) {
        super(e);
    }

    public ProjectMisconfiguredException(String msg) {
        super(msg);
    }

}
