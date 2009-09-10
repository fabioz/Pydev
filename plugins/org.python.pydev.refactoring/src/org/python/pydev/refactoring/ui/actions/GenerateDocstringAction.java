/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

/*
 * @author Robin Stocker
 */
package org.python.pydev.refactoring.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.refactoring.PepticLog;
import org.python.pydev.refactoring.codegenerator.generatedocstring.GenerateDocstringOperation;

public class GenerateDocstringAction extends PyAction {

    public void run(IAction action) {
        GenerateDocstringOperation op = new GenerateDocstringOperation(getPyEdit());
        try{
            PydevPlugin.getWorkspace().run(op, new NullProgressMonitor());
        }catch(CoreException e){
            PepticLog.logError(e);
        }
    }
}
