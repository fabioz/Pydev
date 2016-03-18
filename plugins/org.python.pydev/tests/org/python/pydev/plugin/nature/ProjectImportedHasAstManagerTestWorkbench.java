/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.nature;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;

public class ProjectImportedHasAstManagerTestWorkbench extends AbstractWorkbenchTestCase {

    @Override
    protected void setUp() throws Exception {
        //no setup (because we won't have the nature in this test)
        closeWelcomeView();
    }

    public void testEditWithNoNature() throws Exception {
        NullProgressMonitor monitor = new NullProgressMonitor();

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IPath workspaceLoc = root.getRawLocation();
        IProject project = root.getProject("pydev_nature_pre_configured");
        if (project.exists()) {
            project.delete(true, monitor);
        }

        //let's create its structure
        IPath projectLoc = workspaceLoc.append("pydev_nature_pre_configured");
        projectLoc.toFile().mkdir();

        writeProjectFile(projectLoc.append(".project"));

        writePydevProjectFile(projectLoc.append(".pydevproject"));
        File srcLocFile = projectLoc.append("src").toFile();
        srcLocFile.mkdir();

        assertTrue(!project.exists());
        project.create(monitor);
        project.open(monitor);
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        IJobManager jobManager = Job.getJobManager();
        jobManager.resume();
        final PythonNature nature = (PythonNature) PythonNature.addNature(project, null, null, null, null, null, null);
        assertTrue(nature != null);

        //Let's give it some time to run the jobs that restore the nature
        goToIdleLoopUntilCondition(new ICallback<Boolean, Object>() {

            @Override
            public Boolean call(Object arg) {
                if (nature != null) {
                    if (nature.getAstManager() != null) {
                        return true;
                    }
                }
                return false;
            }
        });

        assertTrue(nature.getAstManager() != null);
        PythonPathHelper pythonPathHelper = (PythonPathHelper) nature.getAstManager().getModulesManager()
                .getPythonPathHelper();
        List<String> lst = new ArrayList<String>();
        lst.add(FileUtils.getFileAbsolutePath(srcLocFile));
        assertEquals(lst, pythonPathHelper.getPythonpath());

    }

    private void writePydevProjectFile(IPath path) {
        String str = "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                +
                "<?eclipse-pydev version=\"1.0\"?>\n" +
                "<pydev_project>\n"
                +
                "<pydev_pathproperty name=\"org.python.pydev.PROJECT_SOURCE_PATH\">\n"
                +
                "<path>/pydev_nature_pre_configured/src</path>\n" +
                "</pydev_pathproperty>\n"
                +
                "<pydev_property name=\"org.python.pydev.PYTHON_PROJECT_VERSION\">python 2.4</pydev_property>\n"
                +
                "</pydev_project>\n" +
                "";

        FileUtils.writeStrToFile(str, path.toFile());
    }

    private void writeProjectFile(IPath path) {
        String str = "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<projectDescription>\n"
                +
                "    <name>test_python</name>\n" +
                "    <comment></comment>\n" +
                "    <projects>\n"
                +
                "        <project>4DLM</project>\n" +
                "    </projects>\n" +
                "    <buildSpec>\n"
                +
                "        <buildCommand>\n" +
                "            <name>org.python.pydev.PyDevBuilder</name>\n"
                +
                "            <arguments>\n" +
                "            </arguments>\n" +
                "        </buildCommand>\n"
                +
                "    </buildSpec>\n" +
                "    <natures>\n" +
                "        <nature>org.python.pydev.pythonNature</nature>\n"
                +
                "    </natures>\n" +
                "</projectDescription>\n" +
                "";

        FileUtils.writeStrToFile(str, path.toFile());
    }

}
