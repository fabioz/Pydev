package org.python.pydev.core.uiutils;

import org.eclipse.swt.widgets.Display;

public class RunInUiThread {

    public static void sync(Runnable r){
        if (Display.getCurrent() == null){
            Display.getDefault().syncExec(r);
        }else{
            r.run();
        }
    }
    
    public static void async(Runnable r){
        if (Display.getCurrent() == null){
            Display.getDefault().asyncExec(r);
        }else{
            r.run();
        }
    }
}
