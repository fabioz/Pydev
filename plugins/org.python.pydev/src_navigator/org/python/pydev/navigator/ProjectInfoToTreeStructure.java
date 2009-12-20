package org.python.pydev.navigator;

import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

public class ProjectInfoToTreeStructure {

    public static InterpreterInfoTreeNode<LabelAndImage> createFrom(IInterpreterInfo interpreterInfo) {
        ImageCache imageCache = PydevPlugin.getImageCache();
        
        InterpreterInfoTreeNode<LabelAndImage> root;
        if(interpreterInfo != null){
            root = new InterpreterInfoTreeNode<LabelAndImage>(
                    null,
                    new LabelAndImage(interpreterInfo.getNameForUI(), imageCache.get(UIConstants.PY_INTERPRETER_ICON))
            );
            
            InterpreterInfoTreeNode<LabelAndImage> systemLibs = new InterpreterInfoTreeNode<LabelAndImage>(
                    root, 
                    new LabelAndImage("System Libs", imageCache.get(UIConstants.LIB_SYSTEM_ROOT))
            );
            
            List<String> pythonPath = interpreterInfo.getPythonPath();
            for (String string : pythonPath) {
                new InterpreterInfoTreeNode<LabelAndImage>(
                        systemLibs, 
                        new LabelAndImage(string, imageCache.get(UIConstants.LIB_SYSTEM))
                );
            }
            
            InterpreterInfoTreeNode<LabelAndImage> forcedBuiltins = new InterpreterInfoTreeNode<LabelAndImage>(
                    root, 
                    new LabelAndImage("Forced builtins", imageCache.get(UIConstants.LIB_SYSTEM_ROOT))
            );
            
            for (Iterator<String> it=interpreterInfo.forcedLibsIterator();it.hasNext();) {
                String string = it.next();
                new InterpreterInfoTreeNode<LabelAndImage>(
                        forcedBuiltins, 
                        new LabelAndImage(string, imageCache.get(UIConstants.LIB_FORCED_BUILTIN))
                );
            }
            
        }else{
            root = new InterpreterInfoTreeNode<LabelAndImage>(
                    null,
                    new LabelAndImage("No Interpreter Configured", imageCache.get(UIConstants.PY_INTERPRETER_ICON))
            );
        }
        
        
        return root;
    }

}
