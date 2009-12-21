package org.python.pydev.customizations.app_engine.actions;

import org.eclipse.jface.action.IAction;

public class AppEngineUploadAction extends AbstractAppEngineAction{
    
    public void run(IAction action){
        AppEngineUpload upload = new AppEngineUpload();
        upload.executeInObject(sourceFolder);
    }
}
