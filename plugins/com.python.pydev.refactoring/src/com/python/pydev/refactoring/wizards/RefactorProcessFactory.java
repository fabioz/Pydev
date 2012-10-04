/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.KeywordParameterDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.visitors.NodeUtils;

import com.python.pydev.refactoring.wizards.rename.PyRenameAnyLocalProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameAttributeProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameClassProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameFunctionProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameGlobalProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameImportProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameLocalProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameParameterProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameSelfAttributeProcess;

public class RefactorProcessFactory {

    /**
     * Decides which process should take care of the request.
     * @param request 
     */
    public static IRefactorRenameProcess getProcess(Definition definition, RefactoringRequest request) {
        if (definition instanceof AssignDefinition) {
            AssignDefinition d = (AssignDefinition) definition;
            if (d.target.indexOf('.') != -1) {
                if (d.target.startsWith("self.")) {
                    //ok, it is a member and not a local
                    return new PyRenameSelfAttributeProcess(definition, d.target);
                } else {
                    return new PyRenameAttributeProcess(definition, d.target);
                }

            } else {
                if (definition.scope != null) {
                    //classvar
                    if (definition.scope.isLastClassDef()) {
                        return new PyRenameAttributeProcess(definition, d.target);
                    }
                    FastStack scopeStack = definition.scope.getScopeStack();
                    if (request.moduleName.equals(definition.module.getName())) {
                        if (!scopeStack.empty()) {
                            Object peek = scopeStack.peek();
                            if (peek instanceof FunctionDef) {
                                return new PyRenameLocalProcess(definition);
                            }
                        }
                    }
                }
                return new PyRenameGlobalProcess(definition);
            }
        }
        if (definition.ast != null) {
            if (definition.ast instanceof ClassDef) {
                return new PyRenameClassProcess(definition);
            }

            if (definition.ast instanceof Name) {
                Name n = (Name) definition.ast;
                if (n.ctx == Name.Param || n.ctx == Attribute.KwOnlyParam) {
                    return new PyRenameParameterProcess(definition);
                }
            }

            if (definition instanceof KeywordParameterDefinition) {
                return new PyRenameParameterProcess((KeywordParameterDefinition) definition, request.nature);
            }

            if (definition.ast instanceof FunctionDef) {
                return new PyRenameFunctionProcess(definition);
            }
            if (NodeUtils.isImport(definition.ast)) {
                //this means that we found an import and we cannot actually map that import to a definition
                //(so, it is an unresolved import)
                return new PyRenameImportProcess(definition);
            }
        } else {
            //the definition ast is null. This should mean that it was actually an import
            //and pointed to some module
            return new PyRenameImportProcess(definition);

        }
        if (definition.scope != null) {
            //classvar
            if (definition.scope.isLastClassDef()) {
                return new PyRenameAttributeProcess(definition, definition.value);
            }

            FastStack scopeStack = definition.scope.getScopeStack();
            if (request.moduleName.equals(definition.module.getName())) {
                if (!scopeStack.empty()) {
                    Object peek = scopeStack.peek();
                    if (peek instanceof FunctionDef) {
                        return new PyRenameLocalProcess(definition);
                    }
                }
            }

        }
        return new PyRenameGlobalProcess(definition);
    }

    public static IRefactorRenameProcess getRenameAnyProcess() {
        return new PyRenameAnyLocalProcess();
    }

}
