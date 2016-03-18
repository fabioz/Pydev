/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.customizations.app_engine.launching.AppEngineConstants;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.ui.wizards.project.NewProjectNameAndLocationWizardPage;

public class AppEngineConfigWizardPageTestWorkbench extends AbstractWorkbenchTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AppEngineConfigWizardPage.selectLibraries = new ICallback<List<String>, List<String>>() {

            @Override
            public List<String> call(List<String> arg) {
                return arg;
            }
        };
    }

    @Override
    protected void tearDown() throws Exception {
        AppEngineConfigWizardPage.selectLibraries = null;
        super.tearDown();
    }

    public void testCreateLaunchAndDebugGoogleAppProject() throws Exception {

        final Display display = Display.getDefault();
        final Boolean[] executed = new Boolean[] { false };
        display.syncExec(new Runnable() {

            @Override
            public void run() {
                final Shell shell = new Shell(display);
                shell.setLayout(new FillLayout());
                Composite pageContainer = new Composite(shell, 0);
                AppEngineWizard appEngineWizard = new AppEngineWizard();
                appEngineWizard.setContainer(new IWizardContainer() {

                    @Override
                    public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
                            throws InvocationTargetException, InterruptedException {
                        runnable.run(new NullProgressMonitor());
                    }

                    @Override
                    public void updateWindowTitle() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void updateTitleBar() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void updateMessage() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void updateButtons() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void showPage(IWizardPage page) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public Shell getShell() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IWizardPage getCurrentPage() {
                        return null;
                    }
                });

                appEngineWizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
                appEngineWizard.addPages();
                appEngineWizard.createPageControls(pageContainer);

                IWizardPage[] pages = appEngineWizard.getPages();
                NewProjectNameAndLocationWizardPage nameAndLocation = (NewProjectNameAndLocationWizardPage) pages[0];
                AppEngineConfigWizardPage appEnginePage = (AppEngineConfigWizardPage) pages[1];

                assertFalse(nameAndLocation.isPageComplete());
                nameAndLocation.setProjectName("AppEngineTest");
                assertTrue(nameAndLocation.isPageComplete());

                assertFalse(appEnginePage.isPageComplete());
                appEnginePage.setAppEngineLocationFieldValue(TestDependent.GOOGLE_APP_ENGINE_LOCATION
                        + "invalid_path_xxx");
                assertFalse(appEnginePage.isPageComplete());
                appEnginePage.setAppEngineLocationFieldValue(TestDependent.GOOGLE_APP_ENGINE_LOCATION);
                assertTrue(appEnginePage.isPageComplete());

                assertTrue(appEngineWizard.performFinish());

                IProject createdProject = appEngineWizard.getCreatedProject();
                PythonNature nature = PythonNature.getPythonNature(createdProject);
                Map<String, String> expected = new HashMap<String, String>();
                expected.put(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE, new File(
                        TestDependent.GOOGLE_APP_ENGINE_LOCATION).getAbsolutePath());
                IPythonPathNature pythonPathNature = nature.getPythonPathNature();
                try {
                    assertEquals(expected, pythonPathNature.getVariableSubstitution());

                    String projectExternalSourcePath = pythonPathNature.getProjectExternalSourcePath(false);
                    assertTrue(projectExternalSourcePath.indexOf(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE) != -1);
                    projectExternalSourcePath = pythonPathNature.getProjectExternalSourcePath(true);
                    assertTrue(projectExternalSourcePath.indexOf(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE) == -1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                //                goToManual();

                executed[0] = true;
            }
        });
        assertTrue(executed[0]);
    }
}
