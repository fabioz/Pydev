/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractmethod.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;

public class ParameterReturnDeduce {
    private List<String> parameters;
    private Collection<String> returns;
    private AbstractScopeNode<?> scopeAdapter;
    private ITextSelection selection;
    private ModuleAdapter moduleAdapter;

    public ParameterReturnDeduce(AbstractScopeNode<?> scope, ITextSelection selection, ModuleAdapter moduleAdapter) {
        this.scopeAdapter = scope;
        this.selection = selection;
        this.parameters = new ArrayList<String>();
        this.returns = new LinkedHashSet<String>(); //maintain order.
        this.moduleAdapter = moduleAdapter;
        deduce();
    }

    private void deduce() {
        ModuleAdapter module = scopeAdapter.getModule();
        List<SimpleAdapter> selected = module.getWithinSelection(selection, scopeAdapter.getUsedVariables());

        List<SimpleAdapter> before = new ArrayList<SimpleAdapter>();
        List<SimpleAdapter> after = new ArrayList<SimpleAdapter>();
        extractBeforeAfterVariables(selected, before, after);

        deduceParameters(before, selected);
        deduceReturns(after, selected);

    }

    private void deduceParameters(List<SimpleAdapter> before, List<SimpleAdapter> selected) {
        Set<String> globalVariableNames = new HashSet<String>(moduleAdapter.getGlobalVariableNames());

        for (SimpleAdapter adapter : before) {
            SimpleNode astNode = adapter.getASTNode();
            String id;
            if (astNode instanceof Name) {
                Name variable = (Name) astNode;
                id = variable.id;
            } else if (astNode instanceof NameTok) {
                NameTok variable = (NameTok) astNode;
                id = variable.id;
            } else {
                continue;
            }
            if (globalVariableNames.contains(id) && !isStored(id, before)) {
                // It's a global variable and there's no assignment
                // shadowing it in the local scope, so don't add it as a
                // parameter.
                continue;
            }
            if (id.equals("True") || id.equals("False") || id.equals("None")) {
                // The user most likely doesn't want them to be passed.
                continue;
            }
            if (isUsed(id, selected)) {
                if (!parameters.contains(id)) {
                    parameters.add(id);
                }
            }
        }
    }

    private void deduceReturns(List<SimpleAdapter> after, List<SimpleAdapter> selected) {
        for (SimpleAdapter adapter : after) {
            SimpleNode astNode = adapter.getASTNode();
            String id;
            if (astNode instanceof Name) {
                Name variable = (Name) astNode;
                id = variable.id;
            } else if (astNode instanceof NameTok) {
                NameTok variable = (NameTok) astNode;
                id = variable.id;
            } else {
                continue;
            }
            if (isStored(id, selected)) {
                returns.add(id);
            }
        }
    }

    private void extractBeforeAfterVariables(List<SimpleAdapter> selectedVariables, List<SimpleAdapter> before,
            List<SimpleAdapter> after) {
        List<SimpleAdapter> scopeVariables = scopeAdapter.getUsedVariables();

        if (selectedVariables.isEmpty()) {
            return;
        }

        SimpleAdapter firstSelectedVariable = selectedVariables.get(0);
        SimpleAdapter lastSelectedVariable = selectedVariables.get(selectedVariables.size() - 1);

        for (SimpleAdapter adapter : scopeVariables) {
            if (isBeforeSelectedLine(firstSelectedVariable, adapter)
                    || isBeforeOnSameLine(firstSelectedVariable, adapter)) {
                before.add(adapter);

            } else if (isAfterSelectedLine(lastSelectedVariable, adapter)
                    || isAfterOnSameLine(lastSelectedVariable, adapter)) {
                after.add(adapter);
            }
        }
    }

    private boolean isAfterOnSameLine(SimpleAdapter lastSelectedVariable, SimpleAdapter adapter) {
        return adapter.getNodeFirstLine(false) == lastSelectedVariable.getNodeFirstLine(false)
                && (adapter.getNodeIndent() > lastSelectedVariable.getNodeIndent());
    }

    private boolean isAfterSelectedLine(SimpleAdapter lastSelectedVariable, SimpleAdapter adapter) {
        return adapter.getNodeFirstLine(false) > lastSelectedVariable.getNodeFirstLine(false);
    }

    private boolean isBeforeOnSameLine(SimpleAdapter firstSelectedVariable, SimpleAdapter adapter) {
        return adapter.getNodeFirstLine(false) == firstSelectedVariable.getNodeFirstLine(false)
                && (adapter.getNodeIndent() < firstSelectedVariable.getNodeIndent());
    }

    private boolean isBeforeSelectedLine(SimpleAdapter firstSelectedVariable, SimpleAdapter adapter) {
        return adapter.getNodeFirstLine(false) < firstSelectedVariable.getNodeFirstLine(false);
    }

    /**
     * Fix (fabioz): to check if it is used, it must be in a load context
     */
    private boolean isUsed(String var, List<SimpleAdapter> scopeVariables) {
        for (SimpleAdapter adapter : scopeVariables) {
            SimpleNode astNode = adapter.getASTNode();
            if (astNode instanceof Name) {
                Name scopeVar = (Name) astNode;
                if ((scopeVar.ctx == Name.Load || scopeVar.ctx == Name.AugLoad) && scopeVar.id.equals(var)) {
                    return true;
                }
            }
            //Note: NameTok are always only in store context.
        }
        return false;
    }

    private boolean isStored(String var, List<SimpleAdapter> scopeVariables) {
        boolean isStored = false;
        // must traverse all variables, because a
        // variable may be used in other context!
        for (SimpleAdapter adapter : scopeVariables) {
            SimpleNode astNode = adapter.getASTNode();
            if (astNode instanceof Name) {
                Name scopeVar = (Name) astNode;
                if (scopeVar.id.equals(var)) {
                    isStored = (scopeVar.ctx != Name.Load && scopeVar.ctx != Name.AugLoad);
                }
            } else if (astNode instanceof NameTok) {
                NameTok scopeVar = (NameTok) astNode;
                if (scopeVar.id.equals(var)) {
                    isStored = true; //NameTok are always store contexts.
                }

            } else if (astNode instanceof Import) {
                Import importNode = (Import) astNode;
                isStored = checkNames(var, importNode.names);

            } else if (astNode instanceof ImportFrom) {
                ImportFrom importFrom = (ImportFrom) astNode;
                isStored = checkNames(var, importFrom.names);
            }

            if (isStored) {
                break;
            }
        }
        return isStored;
    }

    private boolean checkNames(String var, aliasType[] names) {
        boolean isStored = false;
        if (names != null) {
            for (aliasType alias : names) {
                if (alias.asname != null) {
                    isStored = nameMatches(var, alias.asname);
                } else if (alias.name != null) {
                    isStored = nameMatches(var, alias.name);

                }
            }
        }
        return isStored;
    }

    private boolean nameMatches(String var, NameTokType asname) {
        return ((NameTok) asname).id.equals(var);
    }

    public List<String> getParameters() {
        return this.parameters;
    }

    public List<String> getReturns() {
        return new ArrayList<String>(this.returns);
    }

}
