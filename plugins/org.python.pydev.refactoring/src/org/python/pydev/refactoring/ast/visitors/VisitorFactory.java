/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
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

package org.python.pydev.refactoring.ast.visitors;

import java.io.File;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISourceModule;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.VisitorIF;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.refactoring.ast.PythonModuleManager;
import org.python.pydev.refactoring.ast.adapters.AbstractNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.context.AbstractContextVisitor;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionException;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionExtenderVisitor;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionValidationVisitor;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

public final class VisitorFactory {
    private VisitorFactory() {
    }

    public static ITextSelection createSelectionExtension(AbstractScopeNode<?> scope, ITextSelection selection) {
        SelectionExtenderVisitor visitor = null;
        try {
            visitor = new SelectionExtenderVisitor(scope.getModule(), selection);
            scope.getASTNode().accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor.getSelection();
    }

    public static void validateSelection(ModuleAdapter scope) throws SelectionException {
        SelectionValidationVisitor visitor = null;
        try {
            visitor = new SelectionValidationVisitor();
            scope.getASTNode().accept(visitor);
        } catch (SelectionException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends VisitorIF> T createVisitor(Class<T> visitorClass, String source,
            IGrammarVersionProvider versionProvider) throws Throwable {
        return createVisitor(visitorClass, getRootNodeFromString(source, versionProvider));
    }

    public static <T extends VisitorIF> T createVisitor(Class<T> visitorClass, SimpleNode root) {
        T visitor = null;
        try {
            visitor = visitorClass.newInstance();
            root.accept(visitor);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

    /**
     * Unchecked (because if doing Class.cast, it does not work in java 1.4)
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractContextVisitor> T createContextVisitor(Class<T> visitorClass, SimpleNode root,
            ModuleAdapter module, AbstractNodeAdapter parent) {
        try {
            T visitor = (T) visitorClass.getConstructors()[0].newInstance(new Object[] { module, parent });
            root.accept(visitor);
            return visitor;
        } catch (Exception e) {
            throw new CannotCreateContextRuntimeException(e);
        }
    }

    public static ModuleAdapter createModuleAdapter(PythonModuleManager pythonModuleManager, File file, IDocument doc,
            IPythonNature nature, IGrammarVersionProvider versionProvider) throws Throwable {
        if (file != null && file.exists()) {
            if (FileTypesPreferencesPage.isCythonFile(file.getName())) {
                versionProvider = new IGrammarVersionProvider() {

                    public int getGrammarVersion() throws MisconfigurationException {
                        return IPythonNature.GRAMMAR_PYTHON_VERSION_CYTHON;
                    }
                };
            }
            if (pythonModuleManager != null) {
                IModulesManager modulesManager = pythonModuleManager.getIModuleManager();
                if (modulesManager != null) {
                    String modName = modulesManager.resolveModule(FileUtils.getFileAbsolutePath(file));
                    if (modName != null) {
                        IModule module = modulesManager.getModule(modName, nature, true);
                        if (module instanceof ISourceModule) {
                            SourceModule iSourceModule = (SourceModule) module;
                            if (iSourceModule.parseError != null) {
                                throw iSourceModule.parseError;
                            }
                            return new ModuleAdapter(pythonModuleManager, ((ISourceModule) module), nature, doc);
                        }
                    }
                }
            }
        }
        return new ModuleAdapter(pythonModuleManager, file, doc, getRootNode(doc, versionProvider), nature);
    }

    public static SimpleNode getRootNodeFromString(String source, IGrammarVersionProvider versionProvider)
            throws ParseException, MisconfigurationException {
        return getRootNode(getDocumentFromString(source), versionProvider);
    }

    private static IDocument getDocumentFromString(String source) {
        return new Document(source);
    }

    public static Module getRootNode(IDocument doc, IGrammarVersionProvider versionProvider) throws ParseException,
            MisconfigurationException {
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(doc, versionProvider
                .getGrammarVersion()));
        Throwable exception = objects.error;

        if (exception != null) {
            /* We try to get rid of the 'Throwable' exception, if possible */
            if (exception instanceof ParseException) {
                throw (ParseException) exception;
            } else if (exception instanceof TokenMgrError) {
                /* Error from Lexer */
                throw new ParseException(exception.toString());
            } else {
                throw new RuntimeException(exception);
            }
        }

        if (objects.error != null) {
            throw new RuntimeException(objects.error);
        }
        return (Module) objects.ast;
    }

    /**
     * Provides a way to find duplicates of a given expression.
     */
    public static FindDuplicatesVisitor createDuplicatesVisitor(ITextSelection selection, SimpleNode nodeToVisit,
            exprType expression, AbstractScopeNode node, IDocument doc) {
        FindDuplicatesVisitor visitor = new FindDuplicatesVisitor(selection, expression, doc);
        try {
            nodeToVisit.accept(visitor);
            visitor.finish();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }
}
