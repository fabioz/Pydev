/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.codecoverage;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbench;
import org.python.pydev.core.TestCaseUtils;
import org.python.pydev.debug.ui.launching.LaunchShortcut;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.StringUtils;

public class PyCodeCoverageTestWorkbench extends AbstractWorkbenchTestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite(PyCodeCoverageTestWorkbench.class.getName());

        suite.addTestSuite(PyCodeCoverageTestWorkbench.class);

        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }

    private IFolder sourceFolder;
    private IFile modCov;

    /* (non-Javadoc)
     * @see org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        closeWelcomeView();
        configureInterpreters();

        IProgressMonitor monitor = new NullProgressMonitor();
        IProject project = createProject(monitor, "coverage_test_project");
        sourceFolder = createSourceFolder(monitor, project, true, false);
        IFile initFile = createPackageStructure(sourceFolder, "pack_cov", monitor);
        modCov = initFile.getParent().getFile(new Path("mod_cov.py"));
        setFileContents(modCov, getModCovContents());

        PythonNature nature = PythonNature.getPythonNature(project);
        waitForNatureToBeRecreated(nature);
    }

    /**
     * @return
     */
    private String getModCovContents() {
        return "" +
                "import unittest\n" +
                "\n" +
                "class TestCase(unittest.TestCase):\n" +
                "    \n"
                +
                "    def testCovered1(self):\n" +
                "        print('t1')\n" +
                "        print('t2')\n"
                +
                "        print('t3')\n" +
                "    \n" +
                "    def testCovered2(self):\n" +
                "        print('t1')\n"
                +
                "        print('t2')\n" +
                "        print('t3')\n" +
                "        \n" +
                "    def testNotCovered(self):\n"
                +
                "        if False:\n" +
                "            print('t1')\n" +
                "            print('t2')\n"
                +
                "            print('t3')\n" +
                "        \n" +
                "if __name__ == '__main__':\n" +
                "    unittest.main()\n"
                +
                "";
    }

    public void testPyCodeCoverageView() throws Exception {

        final PyCodeCoverageView view = PyCodeCoverageView.getView(true);
        //At this point it should have no folder selected and the option to run things in coverage should be
        //set to false.
        assertTrue(!PyCoveragePreferences.getAllRunsDoCoverage());
        assertTrue(PyCodeCoverageView.getChosenDir() == null);

        assertTrue(!view.allRunsGoThroughCoverage.getSelection());
        assertTrue(!PyCoveragePreferences.getInternalAllRunsDoCoverage());
        view.allRunsGoThroughCoverage.setSelection(true);
        view.allRunsGoThroughCoverage.notifyListeners(SWT.Selection, new Event());

        assertTrue(PyCoveragePreferences.getInternalAllRunsDoCoverage());
        assertTrue(!PyCoveragePreferences.getAllRunsDoCoverage());

        view.setSelectedContainer(sourceFolder);
        TreeViewer treeViewer = view.getTreeViewer();
        ITreeContentProvider cp = (ITreeContentProvider) treeViewer.getContentProvider();
        Object[] elements = cp.getElements(treeViewer.getInput());
        assertEquals(1, elements.length);
        ILabelProvider labelProvider = (ILabelProvider) treeViewer.getLabelProvider();
        assertEquals("pack_cov", labelProvider.getText(elements[0]));

        TestCaseUtils.assertContentsEqual(getInitialCoverageText(), view.getCoverageText());

        Object[] expandedElements = treeViewer.getExpandedElements();
        assertEquals(0, expandedElements.length);
        treeViewer.expandAll();
        expandedElements = treeViewer.getExpandedElements();
        assertEquals(1, expandedElements.length);

        view.executeRefreshAction(new NullProgressMonitor());
        expandedElements = treeViewer.getExpandedElements();
        assertEquals(1, expandedElements.length);

        assertTrue(PyCoveragePreferences.getAllRunsDoCoverage());

        final IWorkbench workBench = PydevPlugin.getDefault().getWorkbench();
        Display display = workBench.getDisplay();

        // Make sure to run the UI thread.
        final PyEdit modCovEditor = (PyEdit) PyOpenEditor.doOpenEditor(modCov);
        try {
            display.syncExec(new Runnable() {
                public void run() {
                    LaunchShortcut launchShortcut = new LaunchShortcut();
                    launchShortcut.launch(modCovEditor, "run");
                }
            });

            final String modCovCoverageText = StringUtils.replaceNewLines(getModCovCoverageText(), "\n");
            //Should be enough time for the refresh to happen!
            goToManual(10000, new ICallback<Boolean, Object>() {

                public Boolean call(Object arg) {
                    return modCovCoverageText.equals(StringUtils.replaceNewLines(view.getCoverageText(), "\n"));
                }
            });

            TestCaseUtils.assertContentsEqual(modCovCoverageText, view.getCoverageText());

            //goToManual();
        } finally {
            try {
                modCovEditor.close(false);
            } catch (Exception e) {
                //ignore anything here
            }
        }

    }

    private String getModCovCoverageText() {
        return "" +
                "Name                                      Stmts     Miss      Cover  Missing\n"
                +
                "-----------------------------------------------------------------------------\n"
                +
                "__init__.py                                   0        0         -   \n"
                +
                "mod_cov.py                                   17        3      82,4%  17-19\n"
                +
                "-----------------------------------------------------------------------------\n"
                +
                "TOTAL                                        17        3      82,4%  \n" +
                "";
    }

    private String getInitialCoverageText() {
        return "" +
                "Name                                      Stmts     Miss      Cover  Missing\n"
                +
                "-----------------------------------------------------------------------------\n"
                +
                "__init__.py                                   0        0         -   \n"
                +
                "mod_cov.py                                   17       17         0%  1-22\n"
                +
                "-----------------------------------------------------------------------------\n"
                +
                "TOTAL                                        17       17         0%  \n" +
                "";
    }
}
