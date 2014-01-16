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
        //Still working on it...
        //        AbstractAdditionalDependencyInfo info = null;
        //        if (manager instanceof SystemModulesManager) {
        //            SystemModulesManager systemModulesManager = (SystemModulesManager) manager;
        //            try {
        //                info = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(
        //                        ((SystemModulesManager) manager).getInterpreterManager(),
        //                        systemModulesManager.getInfo().getExecutableOrJar());
        //            } catch (MisconfigurationException e) {
        //                Log.log(e);
        //            }
        //
        //        } else if (manager instanceof ProjectModulesManager) {
        //            //info = (AbstractAdditionalDependencyInfo) AdditionalProjectInterpreterInfo
        //            //        .getAdditionalInfoForProject(manager.getNature());
        //            //
        //            return;
        //        } else {
        //            return;
        //        }
        //
        //        if (info == null) {
        //            return;
        //        }
        //        IToken[] globalTokens = module.getGlobalTokens();
        //        PyAstFactory astFactory = new PyAstFactory(new AdapterPrefs("\n", manager.getNature()));
        //
        //        List<stmtType> body = new ArrayList<>(globalTokens.length);
        //
        //        for (IToken token : globalTokens) {
        //            switch (token.getType()) {
        //
        //                case IToken.TYPE_CLASS:
        //                    body.add(astFactory.createClassDef(token.getRepresentation()));
        //                    break;
        //
        //                case IToken.TYPE_FUNCTION:
        //                    body.add(astFactory.createFunctionDef(token.getRepresentation()));
        //                    break;
        //
        //                default:
        //                    Name attr = astFactory.createName(token.getRepresentation());
        //                    body.add(astFactory.createAssign(attr, attr)); //assign to itself just for generation purposes.
        //                    break;
        //            }
        //        }
        //        //System.out.println("Creating info for: " + module.getName());
        //        info.removeInfoFromModule(module.getName(), true);
        //        info.addAstInfo(astFactory.createModule(body), new ModulesKey(module.getName(), module.getFile()), true);

    }

}
