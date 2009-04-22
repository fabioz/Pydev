package org.python.pydev.core;


public class ProjectMisconfiguredException extends Exception{

    public ProjectMisconfiguredException(Exception e) {
        super(e);
    }

    public ProjectMisconfiguredException(String msg) {
        super(msg);
    }

}
