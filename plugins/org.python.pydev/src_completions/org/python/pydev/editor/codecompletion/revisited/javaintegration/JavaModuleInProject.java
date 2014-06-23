/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.python.pydev.core.IToken;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This class defines a module that represents a given java class or package within a java project 
 * (that's referenced from a jython project).
 *
 * @author Fabio
 */
public class JavaModuleInProject extends AbstractJavaClassModule {

    private static final boolean DEBUG_CLASS_MODULE_IN_PROJECT = false;

    private static final int UNKNOWN = -1;
    private static final int IS_PACKAGE = 0;
    private static final int IS_CLASS = 1;

    private IJavaProject javaProject;

    private int moduleType = UNKNOWN;

    /**
     * @param name that's the name of the module for jython
     * @param javaProject that's the project where it exists.
     */
    protected JavaModuleInProject(String name, IJavaProject javaProject) {
        super(name);
        this.javaProject = javaProject;

        if (DEBUG_CLASS_MODULE_IN_PROJECT) {
            System.out.println("Created JavaClassModuleInProject: " + name);
        }

        this.tokens = createTokens(name);
        if (DEBUG_CLASS_MODULE_IN_PROJECT) {
            System.out.println("JavaClassModuleInProject tokens:");
            if (this.tokens == null) {
                System.out.println("null tokens");

            } else {
                for (IToken t : this.tokens) {
                    System.out.println(t.getRepresentation());
                }
            }
        }

    }

    /**
     * @return whether it's a package or a class.
     */
    @Override
    public boolean isPackage() {
        if (this.moduleType == UNKNOWN) {
            throw new RuntimeException("Still can't determine whether it's a package or not.");
        }
        return this.moduleType == IS_PACKAGE;
    }

    private File file;

    @Override
    public File getFile() {
        return file;
    }

    /**
     * @see AbstractJavaClassModule#getJavaCompletionProposals(String, String)
     */
    @Override
    protected List<Tuple<IJavaElement, CompletionProposal>> getJavaCompletionProposals(String completeClassDesc,
            String filterCompletionName) throws Exception {
        String contents;
        if (filterCompletionName != null) {
            //pre-filter it a bit if we already know the completion name
            contents = "new %s().%s";
            contents = StringUtils.format(contents, completeClassDesc, completeClassDesc, filterCompletionName);

        } else {
            contents = "new %s().";
            contents = StringUtils.format(contents, completeClassDesc, completeClassDesc);
        }

        List<Tuple<IJavaElement, CompletionProposal>> javaCompletionProposals = getJavaCompletionProposals(contents,
                contents.length(), filterCompletionName);
        if (javaCompletionProposals.size() == 0) {
            //Handle static access (notice that we don't create an instance.)
            if (filterCompletionName != null) {
                //pre-filter it a bit if we already know the completion name
                contents = "%s.%s";
                contents = StringUtils.format(contents, completeClassDesc, completeClassDesc, filterCompletionName);

            } else {
                contents = "%s.";
                contents = StringUtils.format(contents, completeClassDesc, completeClassDesc);
            }
            javaCompletionProposals = getJavaCompletionProposals(contents, contents.length() - 2, filterCompletionName);

        }
        return javaCompletionProposals;
    }

    /**
     * @see AbstractJavaClassModule#getJavaCompletionProposals(String, int, String)
     * 
     * @note: the completionOffset is ignored (we find the type and go for the completions on that type).
     */
    @Override
    protected List<Tuple<IJavaElement, CompletionProposal>> getJavaCompletionProposals(String contents,
            int completionOffset, String filterCompletionName) throws Exception {
        try {
            IType type = this.javaProject.findType(name);

            final List<Tuple<IJavaElement, CompletionProposal>> ret = new ArrayList<Tuple<IJavaElement, CompletionProposal>>();

            //we only get actual completions on a class (otherwise, what we have is a package -- which is treated
            //as if it was an empty __init__.py file -- without any tokens).
            if (type != null) {
                getCompletionsForType(contents, filterCompletionName, type, ret);
            }

            if (this.moduleType == UNKNOWN) {
                //if we found the type, it's a class (otherwise it's a package).
                if (type == null) {
                    this.moduleType = IS_PACKAGE;
                } else {
                    this.moduleType = IS_CLASS;
                    this.file = new File(PydevPlugin.getIResourceOSString(type.getResource()));
                }
            }

            return ret;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected IJavaElement findJavaElement(String javaClassModuleName) throws Exception {
        return this.javaProject.findType(javaClassModuleName);
    }

    /**
     * Tries to get completions for a given element.
     */
    private void getCompletionsForType(String contents, String filterCompletionName, IType type,
            final List<Tuple<IJavaElement, CompletionProposal>> ret) throws JavaModelException {
        ICompilationUnit unit = type.getCompilationUnit();
        if (unit == null) {
            return;
        }
        CompletionProposalCollector collector = createCollector(filterCompletionName, ret, unit);
        type.codeComplete(StringUtils.format(contents, name).toCharArray(), -1, 0, new char[0][0], new char[0][0],
                new int[0], false, collector);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JavaModuleInProject)) {
            return false;
        }
        JavaModuleInProject m = (JavaModuleInProject) obj;

        if (name == null || m.name == null) {
            if (name != m.name) {
                return false;
            }
            //both null at this point
        } else if (!name.equals(m.name)) {
            return false;
        }

        if (file == null || m.file == null) {
            if (file != m.file) {
                return false;
            }
            //both null at this point
        } else if (!file.equals(m.file)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 31;
        if (file != null) {
            hash += file.hashCode();
        }
        if (name != null) {
            hash += name.hashCode();
        }
        return hash;
    }
}
