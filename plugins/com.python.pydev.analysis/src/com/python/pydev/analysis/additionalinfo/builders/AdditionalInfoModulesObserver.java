/******************************************************************************
* Copyright (C) 2006-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
/*
 * Created on Sep 24, 2006
 * @author Fabio
 */
package com.python.pydev.analysis.additionalinfo.builders;

import org.python.pydev.core.IModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.IModulesObserver;

/**
 * Before this approach is finished, we have to check when we first parse the modules, so that
 * forced builtin modules don't generate any delta (and just after it, let's finish this approach)
 * 
 * @author Fabio
 */
public class AdditionalInfoModulesObserver implements IModulesObserver {

    public void notifyCompiledModuleCreated(CompiledModule module, IModulesManager manager) {
        //        IPythonNature nature = manager.getNature();
        //        AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        //        if(info == null){
        //            return;
        //        }
        //        IToken[] globalTokens = module.getGlobalTokens();
        //        for (IToken token : globalTokens) {
        //            switch (token.getType()) {
        //            
        //            case PyCodeCompletion.TYPE_CLASS:
        //                
        //                break;
        //                
        //            case PyCodeCompletion.TYPE_FUNCTION:
        //                
        //                break;
        //                
        //            case PyCodeCompletion.TYPE_ATTR:
        //                
        //                break;
        //
        //            default:
        //                break;
        //            }
        //        }
        //        info.addSourceModuleInfo(m, nature, true);
        //throw new RuntimeException("Still needs to be better tought.");

    }

}
