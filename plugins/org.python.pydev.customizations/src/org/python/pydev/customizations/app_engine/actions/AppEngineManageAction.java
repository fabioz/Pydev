package org.python.pydev.customizations.app_engine.actions;

import org.eclipse.jface.action.IAction;

public class AppEngineManageAction extends AbstractAppEngineAction{
    
    public void run(IAction action){
        AppEngineManage manage = new AppEngineManage();
        manage.executeInObject(sourceFolder);
    }

}
