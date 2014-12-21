/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.parser.fastparser.FastDefinitionsParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class PyUnitTestResult {

    public final String status;
    public final String location;
    public final String test;
    public final String capturedOutput;
    public final String errorContents;
    public final String time;
    private WeakReference<PyUnitTestRun> testRun;

    public final String STATUS_OK = "ok";
    public final String STATUS_SKIP = "skip";
    public final String STATUS_FAIL = "fail";
    public final String STATUS_ERROR = "error";
    public final String index;

    public PyUnitTestResult(PyUnitTestRun testRun, String status, String location, String test, String capturedOutput,
            String errorContents, String time) {
        //note that the parent has a strong reference to the children.
        this.testRun = new WeakReference<PyUnitTestRun>(testRun);
        this.status = status;
        this.location = location;
        this.test = test;
        this.capturedOutput = capturedOutput;
        this.errorContents = errorContents;
        this.time = time;
        this.index = testRun.getNextTestIndex();
    }

    public PyUnitTestRun getTestRun() {
        return this.testRun.get();
    }

    public boolean isOk() {
        return STATUS_OK.equals(this.status);
    }

    public boolean isSkip() {
        return STATUS_SKIP.equals(this.status);
    }

    /**
     * Note that this string is used for the tooltip in the tree (so, be careful when changing it, as the information
     * presentation is based on its format to add a different formatting).
     */
    @Override
    public String toString() {
        int fixedContentsLen = 50;
        FastStringBuffer buf = new FastStringBuffer(this.test.length() + this.status.length() + this.time.length()
                + this.location.length() + this.errorContents.length() + this.capturedOutput.length()
                + fixedContentsLen);

        return buf.append(this.test).append(" Status: ").append(this.status).append(" Time: ").append(this.time)
                .append("\n\n").append("File: ").append(this.location).append("\n\n").append(this.errorContents)
                .append("\n\n").append(this.capturedOutput).append("\n\n").toString();
    }

    public void open() {
        File file = new File(this.location);
        if (file.exists()) {
            PyOpenAction openAction = new PyOpenAction();
            String fileContents = FileUtils.getFileContents(file);
            String thisTest = this.test;
            int i = thisTest.indexOf('['); // This happens when parameterizing pytest tests.
            if (i != -1) {
                thisTest = thisTest.substring(0, i);
            }
            ItemPointer itemPointer = getItemPointer(file, fileContents, thisTest);
            openAction.run(itemPointer);
        }
    }

    public static ItemPointer getItemPointer(File file, String fileContents, String testPath) {
        SimpleNode testNode = null;
        if (fileContents != null) {
            SimpleNode node = FastDefinitionsParser.parse(fileContents, "");
            if (testPath != null && testPath.length() > 0) {
                testNode = NodeUtils.getNodeFromPath(node, testPath);
            }
        }

        ItemPointer itemPointer;
        if (testNode != null) {
            itemPointer = new ItemPointer(file, testNode);
        } else {
            //Ok, it's not defined directly here (it's probably in a superclass), so, let's go on and 
            //do an actual (more costly) find definition.
            try {
                PySourceLocatorBase locator = new PySourceLocatorBase();
                IFile workspaceFile = locator.getWorkspaceFile(file, null);
                if (workspaceFile != null && workspaceFile.exists()) {
                    IProject project = workspaceFile.getProject();
                    if (project != null && project.exists()) {
                        PythonNature nature = PythonNature.getPythonNature(project);
                        String moduleName = nature.resolveModule(file);
                        if (moduleName != null) {
                            IModule mod = nature.getAstManager().getModule(moduleName, nature, true);
                            if (mod != null) {
                                ICompletionCache completionCache = new CompletionCache();
                                IDefinition[] definitions = mod.findDefinition(CompletionStateFactory
                                        .getEmptyCompletionState(testPath, nature, completionCache), -1, -1, nature);

                                if (definitions != null && definitions.length > 0) {
                                    List<ItemPointer> pointers = new ArrayList<ItemPointer>();
                                    PyRefactoringFindDefinition.getAsPointers(pointers, definitions);
                                    if (pointers.size() > 0) {
                                        return pointers.get(0);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
            //if we couldn't actually get the definition line, at least open the file we had (although that may not really
            //be the place where it's defined if it's a test in a superclass).
            itemPointer = new ItemPointer(file);
        }
        return itemPointer;
    }

}
