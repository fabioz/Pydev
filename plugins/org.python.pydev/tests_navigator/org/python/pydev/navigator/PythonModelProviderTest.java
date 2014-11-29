/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.navigator.elements.PythonFolder;
import org.python.pydev.navigator.elements.PythonProjectSourceFolder;
import org.python.pydev.navigator.elements.PythonSourceFolder;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.resource_stubs.FileStub;
import org.python.pydev.shared_core.resource_stubs.FolderStub;
import org.python.pydev.shared_core.resource_stubs.ProjectStub;
import org.python.pydev.shared_core.resource_stubs.WorkingSetStub;
import org.python.pydev.shared_core.resource_stubs.WorkspaceRootStub;

@SuppressWarnings("unchecked")
public class PythonModelProviderTest extends TestCase {

    public static void main(String[] args) {
        try {
            PythonModelProviderTest test = new PythonModelProviderTest();
            test.setUp();
            test.testFolderToSourceFolder();
            test.tearDown();
            System.out.println("OK");

            junit.textui.TestRunner.run(PythonModelProviderTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PythonNature.IN_TESTS = true;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        PythonNature.IN_TESTS = false;
    }

    private ProjectStub project;
    private FileStub file;
    private PythonModelProvider provider;

    /**
     * Test if intercepting an add deep within the pythonpath structure will correctly return an object
     * from the python model. 
     */
    public void testInterceptAdd() throws Exception {
        PythonNature nature = createNature(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source/python");

        project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"), nature);
        file = new FileStub(project, new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC
                + "projroot/source/python/pack1/pack2/mod2.py"));
        provider = new PythonModelProvider();

        HashSet<Object> files = new HashSet<Object>();
        files.add(file);
        files.add(null);
        files.add("string");
        provider.interceptAdd(new PipelinedShapeModification(file.getParent(), files));
        assertEquals(2, files.size());
        for (Object wrappedResource : files) {
            assertTrue((wrappedResource instanceof IWrappedResource && ((IWrappedResource) wrappedResource)
                    .getActualObject() == file) || wrappedResource.equals("string"));
        }
    }

    /**
     * Test if intercepting an object that does not have a parent works. 
     */
    public void testInterceptRefresh() throws Exception {
        PythonNature nature = createNature(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source/python");

        project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"), nature);
        provider = new PythonModelProvider();

        PipelinedViewerUpdate update = new PipelinedViewerUpdate();
        Set<Object> refreshTargets = update.getRefreshTargets();
        refreshTargets.add(project);
        refreshTargets.add(null);
        refreshTargets.add("string");
        provider.interceptRefresh(update);
        assertEquals(2, refreshTargets.size());
        for (Object wrappedResource : refreshTargets) {
            assertTrue(wrappedResource == project || wrappedResource.equals("string"));
        }
    }

    /**
     * Test if setting the project root as a source folder will return an object from the python model.
     */
    public void testProjectIsRoot2() throws Exception {
        String pythonpathLoc = TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot";
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(pythonpathLoc);

        PythonNature nature = createNature(pythonPathSet);

        WorkspaceRootStub workspaceRootStub = new WorkspaceRootStub();
        project = new ProjectStub(new File(pythonpathLoc), nature);
        provider = new PythonModelProvider();
        FolderStub folder = new FolderStub(project,
                new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source"));

        workspaceRootStub.addChild(project);
        project.setParent(workspaceRootStub);

        HashSet<Object> folders = new HashSet<Object>();
        folders.add(folder);
        PipelinedShapeModification addModification = new PipelinedShapeModification(project, folders);
        addModification.setParent(project);
        provider.interceptAdd(addModification);

        assertEquals(1, addModification.getChildren().size());
        //it should've been wrapped
        assertTrue(addModification.getChildren().iterator().next() instanceof IWrappedResource);
    }

    /**
     * Test if setting the project root as a source folder will return an object from the python model.
     */
    public void testProjectIsRoot() throws Exception {
        String pythonpathLoc = TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot";
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(pythonpathLoc);

        PythonNature nature = createNature(pythonPathSet);

        WorkspaceRootStub workspaceRootStub = new WorkspaceRootStub();
        project = new ProjectStub(new File(pythonpathLoc), nature);
        provider = new PythonModelProvider();

        workspaceRootStub.addChild(project);
        workspaceRootStub.addChild(null);
        workspaceRootStub.addChild("other");
        project.setParent(workspaceRootStub);

        Object[] children1 = provider.getChildren(workspaceRootStub);
        assertEquals(2, children1.length);
        int stringsFound = 0;
        int projectSourceFoldersFound = 0;
        for (Object c : children1) {
            if (c instanceof String) {
                stringsFound += 1;

            } else if (c instanceof PythonProjectSourceFolder) {
                projectSourceFoldersFound += 1;

            } else {
                fail("Expecting source folder or string. Received: " + c.getClass().getName());
            }
        }
        assertEquals(1, stringsFound);
        assertEquals(1, projectSourceFoldersFound);

        //now, let's go and change the pythonpath location to a folder within the project and see if it changes...
        pythonPathSet.clear();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source/python");
        IResource refreshObject = provider.internalDoNotifyPythonPathRebuilt(project, new ArrayList<String>(
                pythonPathSet));
        assertTrue("Expecting the refresh object to be the root and not the project",
                refreshObject instanceof IWorkspaceRoot);

        children1 = provider.getChildren(workspaceRootStub);
        assertEquals(2, children1.length);
        stringsFound = 0;
        int projectsFound = 0;
        for (Object c : children1) {
            if (c instanceof String) {
                stringsFound += 1;

            } else if (c instanceof IProject) {
                projectsFound += 1;

            } else {
                fail("Expecting source folder or string. Received: " + c.getClass().getName());
            }
        }
        assertEquals(1, stringsFound);
        assertEquals(1, projectsFound);

        //set to be the root again
        pythonPathSet.clear();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot");
        refreshObject = provider.internalDoNotifyPythonPathRebuilt(project, new ArrayList<String>(pythonPathSet));
        assertTrue("Expecting the refresh object to be the root and not the project",
                refreshObject instanceof IWorkspaceRoot);
    }

    /**
     * Creates a nature that has the passed pythonpath location in its pythonpath.
     */
    private PythonNature createNature(String pythonpathLoc) {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(pythonpathLoc);
        return createNature(pythonPathSet);
    }

    /**
     * Creates a nature that has the given set as its underlying pythonpath paths. The reference
     * is kept inside as a reference, so, changing that reference will affect the pythonpath
     * that is set in the nature.
     */
    private PythonNature createNature(final HashSet<String> pythonPathSet) {

        PythonNature nature = new PythonNature() {
            @Override
            public IPythonPathNature getPythonPathNature() {
                HashSet<String> hashSet = new HashSet<>();
                IPath base = Path.fromOSString(TestDependent.TEST_PYSRC_NAVIGATOR_LOC);
                for (String s : pythonPathSet) {
                    if (s.equals("invalid")) {
                        hashSet.add(s);
                    } else {
                        IPath p = Path.fromOSString(s);
                        Assert.isTrue(base.isPrefixOf(p), "Expected: " + base + " to be prefix of: " + p);
                        hashSet.add(p.makeRelativeTo(base).toString());
                    }
                }
                return new PythonPathNatureStub(hashSet);
            }
        };
        return nature;
    }

    /**
     * Test if changing the pythonpath has the desired effects in the python model.
     */
    public void testPythonpathChanges() throws Exception {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source");
        pythonPathSet.add("invalid");
        PythonNature nature = createNature(pythonPathSet);

        project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"), nature, true);
        provider = new PythonModelProvider();
        Object[] children1 = provider.getChildren(project);
        assertTrue(children1[0] instanceof PythonSourceFolder);

        //no changes in the pythonpath
        provider.internalDoNotifyPythonPathRebuilt(project, new ArrayList<String>(pythonPathSet));//still the same

        Object[] children2 = provider.getChildren(project);
        assertEquals(1, children1.length);
        assertEquals(1, children2.length);
        assertSame(children1[0], children2[0]);

        //changed pythonpath (source folders should be removed)
        pythonPathSet.clear();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source/python");
        provider.internalDoNotifyPythonPathRebuilt(project, new ArrayList<String>(pythonPathSet));
        Object[] children3 = provider.getChildren(project);
        assertFalse(children3[0] instanceof PythonSourceFolder);

        //restore initial
        pythonPathSet.clear();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source");
        Object[] children4 = provider.getChildren(project);
        assertTrue(children4[0] instanceof PythonSourceFolder);
        assertNotSame(children1[0], children4[0]); //because it was removed
    }

    public void testDontRemoveOtherPluginElements() throws Exception {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source");
        PythonNature nature = createNature(pythonPathSet);

        project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"), nature);
        provider = new PythonModelProvider();

        HashSet<Object> currentChildren = new HashSet<Object>();
        currentChildren.add("Test");
        provider.getPipelinedChildren(project, currentChildren);

        assertEquals(1, currentChildren.size());
        assertEquals("Test", currentChildren.iterator().next());

        Object[] children = provider.getChildren(project);
        currentChildren.addAll(Arrays.asList(children));
        provider.getPipelinedChildren(project, currentChildren);

        assertEquals(2, currentChildren.size()); //Test + source folder
        boolean found = false;
        for (Object o : currentChildren) {
            if ("Test".equals(o)) {
                found = true;
            } else {
                assertTrue(o instanceof PythonSourceFolder);
            }
        }
        if (!found) {
            fail("Could not find generated child");
        }
    }

    public void testCreateChildrenInWrappedResource() throws Exception {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"); //root is the source
        PythonNature nature = createNature(pythonPathSet);

        WorkspaceRootStub workspaceRootStub = new WorkspaceRootStub();
        ArrayList<Object> additionalChildren = new ArrayList<Object>();
        additionalChildren.add("string");
        project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"), nature, true,
                additionalChildren);
        workspaceRootStub.addChild(project);
        project.setParent(workspaceRootStub);

        provider = new PythonModelProvider();

        HashSet<Object> currentChildren = new HashSet<Object>();
        currentChildren.add(project);
        currentChildren.add(null);
        provider.getPipelinedChildren(workspaceRootStub, currentChildren);

        assertEquals(1, currentChildren.size());
        PythonProjectSourceFolder projectSourceFolder = (PythonProjectSourceFolder) currentChildren.iterator().next();

        currentChildren = new HashSet<Object>();
        currentChildren.add(null);
        provider.getPipelinedChildren(projectSourceFolder, currentChildren);

        assertEquals(2, currentChildren.size());
    }

    public void testNullElements() throws Exception {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"); //root is the source
        PythonNature nature = createNature(pythonPathSet);

        WorkspaceRootStub workspaceRootStub = new WorkspaceRootStub();
        project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"), nature);
        workspaceRootStub.addChild(project);
        project.setParent(workspaceRootStub);

        provider = new PythonModelProvider();

        HashSet<Object> currentChildren = new HashSet<Object>();
        currentChildren.add(project);
        currentChildren.add(null);
        provider.getPipelinedChildren(workspaceRootStub, currentChildren);

        assertEquals(1, currentChildren.size());
        PythonProjectSourceFolder projectSourceFolder = (PythonProjectSourceFolder) currentChildren.iterator().next();

        currentChildren = new HashSet<Object>();
        currentChildren.add(null);
        currentChildren.add(null);
        provider.getPipelinedChildren(projectSourceFolder, currentChildren);

        assertEquals(1, currentChildren.size());
    }

    public void testAddSourceFolderToSourceFolder() throws Exception {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source");
        String source2Folder = TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source2";

        File f = new File(source2Folder);
        if (f.exists()) {
            f.delete();
        }

        pythonPathSet.add(source2Folder); //still not created!
        PythonNature nature = createNature(pythonPathSet);

        project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"), nature, false);
        provider = new PythonModelProvider();
        Object[] children1 = provider.getChildren(project);
        assertEquals(1, children1.length);
        assertTrue(children1[0] instanceof PythonSourceFolder);

        Set set = new HashSet();

        f.mkdir();
        try {
            FolderStub source2FolderFile = new FolderStub(project, f);
            set.add(source2FolderFile);
            provider.interceptAdd(new PipelinedShapeModification(project, set));

            assertEquals(1, set.size());
            assertTrue(set.iterator().next() instanceof PythonSourceFolder);
        } finally {
            f.delete();
        }
    }

    public void testFolderToSourceFolder() throws Exception {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source");
        String source2Folder = TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source2";

        File f = new File(source2Folder);
        File f1 = new File(f, "childFolder");

        if (f1.exists()) {
            f1.delete();
        }
        if (f.exists()) {
            f.delete();
        }

        pythonPathSet.add(source2Folder); //still not created!
        PythonNature nature = createNature(pythonPathSet);

        project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"), nature, false);
        provider = new PythonModelProvider();
        Object[] children1 = provider.getChildren(project);
        assertEquals(1, children1.length);
        assertTrue("Found: " + children1[0], children1[0] instanceof PythonSourceFolder);

        f.mkdir();
        f1.mkdir();
        try {
            FolderStub source2FolderFile = new FolderStub(project, f);
            FolderStub source2FolderChild = new FolderStub(project, source2FolderFile, f1);

            Set set = new HashSet();
            set.add(source2FolderChild);
            provider.interceptAdd(new PipelinedShapeModification(source2FolderFile, set));

            assertEquals(1, set.size());
            PythonFolder c = (PythonFolder) set.iterator().next();
            PythonSourceFolder sourceFolder = c.getSourceFolder();
            assertTrue(sourceFolder instanceof PythonSourceFolder);

            set.clear();
            set.add(source2FolderChild);
            provider.interceptAdd(new PipelinedShapeModification(source2FolderFile, set));
        } finally {
            f1.delete();
            f.delete();
        }
    }

    public void testFolderToSourceFolder2() throws Exception {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source");
        String source2Folder = TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot/source2";

        File f = new File(source2Folder);
        File f1 = new File(f, "childFolder");
        File f2 = new File(f1, "rechildFolder");

        if (f2.exists()) {
            f2.delete();
        }

        if (f1.exists()) {
            f1.delete();
        }
        if (f.exists()) {
            f.delete();
        }

        pythonPathSet.add(source2Folder); //still not created!
        PythonNature nature = createNature(pythonPathSet);

        project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"), nature, false);
        provider = new PythonModelProvider();
        Object[] children1 = provider.getChildren(project);
        assertEquals(1, children1.length);
        assertTrue("Expected source folder. Received: " + children1[0], children1[0] instanceof PythonSourceFolder);

        f.mkdir();
        f1.mkdir();
        f2.mkdir();
        try {
            FolderStub source2FolderFile = new FolderStub(project, f);
            FolderStub source2FolderChild = new FolderStub(project, source2FolderFile, f1);
            FolderStub source2FolderReChild = new FolderStub(project, source2FolderChild, f2);

            Set set = new HashSet();
            set.add(source2FolderReChild);
            provider.interceptAdd(new PipelinedShapeModification(source2FolderChild, set));

            assertEquals(1, set.size());
            PythonFolder c = (PythonFolder) set.iterator().next();
            PythonSourceFolder sourceFolder = c.getSourceFolder();
            assertTrue(sourceFolder instanceof PythonSourceFolder);

            set.clear();
            set.add(source2FolderChild);
            provider.interceptRemove(new PipelinedShapeModification(source2FolderFile, set));
            assertTrue(set.iterator().next() instanceof PythonFolder);
            //            System.out.println(set);

            set.clear();
            set.add(source2FolderReChild);
            provider.interceptAdd(new PipelinedShapeModification(source2FolderChild, set));
            assertTrue(set.iterator().next() instanceof PythonFolder);
            //            System.out.println(set);

            set.clear();
            set.add(source2FolderChild);
            provider.interceptRemove(new PipelinedShapeModification(source2FolderFile, set));
            assertTrue(set.iterator().next() instanceof PythonFolder);
            //            System.out.println(set);

            set.clear();
            set.add(source2FolderReChild);
            provider.interceptAdd(new PipelinedShapeModification(source2FolderChild, set));
            assertTrue(set.iterator().next() instanceof PythonFolder);
            //            System.out.println(set);

        } finally {
            f2.delete();
            f1.delete();
            f.delete();
        }
    }

    public void testWorkingSetsTopLevel() throws Exception {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"); //root is the source
        PythonNature nature = createNature(pythonPathSet);

        WorkspaceRootStub workspaceRootStub = new WorkspaceRootStub();
        project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC + "projroot"), nature);
        workspaceRootStub.addChild(project);
        project.setParent(workspaceRootStub);

        ICallback<List<IWorkingSet>, IWorkspaceRoot> original = PythonModelProvider.getWorkingSetsCallback;
        try {
            final WorkingSetStub workingSetStub = new WorkingSetStub();
            PythonModelProvider.getWorkingSetsCallback = new ICallback<List<IWorkingSet>, IWorkspaceRoot>() {

                public List<IWorkingSet> call(IWorkspaceRoot arg) {
                    ArrayList<IWorkingSet> ret = new ArrayList<IWorkingSet>();
                    ret.add(workingSetStub);
                    return ret;
                }
            };

            provider = new PythonModelProvider();
            provider.topLevelChoice.rootMode = TopLevelProjectsOrWorkingSetChoice.WORKING_SETS;

            //--- check children for the workspace (projects changed for working sets)
            HashSet<Object> currentChildren = new HashSet<Object>();
            currentChildren.add(project); //the project is changed for the workspace.
            provider.getPipelinedChildren(workspaceRootStub, currentChildren);

            HashSet<Object> expectedChildren = new HashSet<Object>();
            expectedChildren.add(workingSetStub);
            assertEquals(expectedChildren, currentChildren);

            //--- now, check if we're able to get the children of the working set.
            workingSetStub.addElement(project);

            currentChildren = new HashSet<Object>();
            provider.getPipelinedChildren(workingSetStub, currentChildren);

            expectedChildren = new HashSet<Object>();
            expectedChildren.add(project);
            assertEquals(expectedChildren, currentChildren);

            //--- and at last, do it the other way around: from the children of a working set we must be able to
            //get the working set
            currentChildren = new HashSet<Object>();
            //the project has the workspace root as its 'default' parent. Let's change it for the working set
            //just a note: working sets can have many elements as their direct children (such as folders and files)
            //so, it's not just a matter of getting the parent if it's a project)!!!
            assertEquals(workingSetStub, provider.getPipelinedParent(project, workspaceRootStub));

            assertEquals(workingSetStub, provider.getParent(project));

        } finally {
            PythonModelProvider.getWorkingSetsCallback = original;
        }
    }

}
