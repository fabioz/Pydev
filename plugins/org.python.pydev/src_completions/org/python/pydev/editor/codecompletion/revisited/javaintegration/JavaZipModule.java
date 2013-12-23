/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModuleForZip;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This is the module wrapper around java files or java packages.
 * 
 * Don't know how to make the completions for java correctly... check: 
 * http://www.eclipse.org/newsportal/article.php?id=68521&group=eclipse.platform#68521
 * 
 * @author Fabio
 */
public class JavaZipModule extends AbstractJavaClassModule {

    public static final boolean DEBUG_JARS = false;

    private File file;

    /**
     * If true, this represents a .class file in a zip, otherwise, it's a module representation.
     */
    private boolean isFileInZip;

    @Override
    public File getFile() {
        return file;
    }

    /**
     * If it's not a file in a zip, it's a folder (in which case it's a package).
     */
    @Override
    public boolean isPackage() {
        return !this.isFileInZip;
    }

    private static HashMap<String, IClasspathEntry[]> classpathEntries = new HashMap<String, IClasspathEntry[]>();

    /**
     * @return the classpath entries that should be used to make the code-completion for this class.
     */
    protected synchronized IClasspathEntry[] getClasspathEntries() {
        return getClasspathEntries(this.file.getAbsolutePath());
    }

    /**
     * @return the classpath entries used in this class
     */
    private static synchronized IClasspathEntry[] getClasspathEntries(String path) {
        IClasspathEntry[] entry = classpathEntries.get(path);
        if (entry == null) {
            entry = new IClasspathEntry[] { JavaCore.newLibraryEntry(Path.fromOSString(path), null, null, true) };
            classpathEntries.put(path, entry);
        }
        return entry;
    }

    /**
     * Creates a java class module from a .class in a jar.
     */
    public JavaZipModule(EmptyModuleForZip emptyModuleForZip) {
        super(emptyModuleForZip.getName());
        this.file = emptyModuleForZip.f;
        this.isFileInZip = emptyModuleForZip.isFile;

        if (DEBUG_JARS) {
            System.out.println("Created JavaZipClassModule: " + name);
        }
        if (isFileInZip) {
            //we only have tokens for a class 
            this.tokens = createTokens(name);
        } else {
            //otherwise, it's a folder (which is treated as a module without any tokens -- as an empty __init__.py file)
            this.tokens = EMPTY_ITOKEN;
        }

    }

    /**
     * @return the java element corresponding to the passed module.
     */
    @Override
    protected IJavaElement findJavaElement(String javaClassModuleName) throws Exception {
        String contents = "import %s.;";
        contents = StringUtils.format(contents, FullRepIterable.getWithoutLastPart(javaClassModuleName));
        final String lookingForClass = FullRepIterable.getLastPart(javaClassModuleName);
        List<Tuple<IJavaElement, CompletionProposal>> javaCompletionProposals = getJavaCompletionProposals(contents,
                contents.length() - 1, lookingForClass);
        if (javaCompletionProposals.size() > 0) {
            return javaCompletionProposals.get(0).o1;
        }
        return null;
    }

    /**
     * Gets tuples with the java element and the corresponding completion proposal for that element.
     * 
     * @param completeClassDesc the name of the class from where we should get the tokens. E.g. java.lang.Class, javax.swing.JFrame
     * @param filterCompletionName if specified, only return matches from elements that have the name passed (otherwise it should be null)
     * @return a list of tuples corresponding to the element and the proposal for the gotten elements
     * @throws JavaModelException
     */
    @Override
    protected List<Tuple<IJavaElement, CompletionProposal>> getJavaCompletionProposals(String completeClassDesc,
            final String filterCompletionName) throws JavaModelException {
        String contents;
        if (filterCompletionName != null) {
            //pre-filter it a bit if we already know the completion name
            contents = "class CompletionClass {void main(){new %s().%s}}";
            contents = StringUtils.format(contents, completeClassDesc, filterCompletionName);

        } else {
            contents = "class CompletionClass {void main(){new %s().}}";
            contents = StringUtils.format(contents, completeClassDesc);
        }

        List<Tuple<IJavaElement, CompletionProposal>> javaCompletionProposals = getJavaCompletionProposals(contents,
                contents.length() - 2, filterCompletionName);
        if (javaCompletionProposals.size() == 0) {
            //Handle static access (notice that we don't create an instance.)
            if (filterCompletionName != null) {
                //pre-filter it a bit if we already know the completion name
                contents = "class CompletionClass {void main(){%s.%s}}";
                contents = StringUtils.format(contents, completeClassDesc, filterCompletionName);

            } else {
                contents = "class CompletionClass {void main(){%s.}}";
                contents = StringUtils.format(contents, completeClassDesc);
            }
            javaCompletionProposals = getJavaCompletionProposals(contents, contents.length() - 2, filterCompletionName);

        }
        return javaCompletionProposals;
    }

    /**
     * Gets tuples with the java element and the corresponding completion proposal for that element.
     * 
     * @param contents the contents that should be set for doing the code-completion
     * @param completionOffset the offset where the code completion should be requested
     * @param filterCompletionName if specified, only return matches from elements that have the name passed (otherwise it should be null)
     * @return a list of tuples corresponding to the element and the proposal for the gotten elements
     * @throws JavaModelException
     */
    @Override
    protected List<Tuple<IJavaElement, CompletionProposal>> getJavaCompletionProposals(String contents,
            int completionOffset, final String filterCompletionName) throws JavaModelException {

        final List<Tuple<IJavaElement, CompletionProposal>> ret = new ArrayList<Tuple<IJavaElement, CompletionProposal>>();

        IClasspathEntry entries[] = getClasspathEntries();
        //Using old version for compatibility with eclipse 3.2
        ICompilationUnit unit = new WorkingCopyOwner() {
        }.newWorkingCopy(name, entries, null, new NullProgressMonitor());
        unit.getBuffer().setContents(contents);
        CompletionProposalCollector collector = createCollector(filterCompletionName, ret, unit);

        unit.codeComplete(completionOffset, collector); //fill the completions while searching it
        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JavaZipModule)) {
            return false;
        }
        JavaZipModule m = (JavaZipModule) obj;

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

        if (isFileInZip != m.isFileInZip) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 23;
        if (file != null) {
            hash += file.hashCode();
        }
        if (name != null) {
            hash += name.hashCode();
        }
        if (isFileInZip) {
            hash *= 2;
        }
        return hash;
    }
}
