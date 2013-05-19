/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.wizards.files;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.ui.IEditorPart;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.templates.PyDocumentTemplateContext;
import org.python.pydev.editor.codecompletion.templates.PyTemplateCompletionProcessor;
import org.python.pydev.editor.templates.PyContextType;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * Python module creation wizard
 * 
 * TODO: Create initial file content from a comment templates
 * 
 * @author Mikko Ohtamaa
 * 
 */
public class PythonModuleWizard extends AbstractPythonWizard {

    public PythonModuleWizard() {
        super("Create a new Python module");
    }

    public static final String WIZARD_ID = "org.python.pydev.ui.wizards.files.PythonModuleWizard";

    @Override
    protected AbstractPythonWizardPage createPathPage() {
        return new AbstractPythonWizardPage(this.description, selection) {

            @Override
            protected boolean shouldCreatePackageSelect() {
                return true;
            }

        };
    }

    /**
     * We will create a new module (file) here given the source folder and the package specified (which
     * are currently validated in the page) 
     * @param monitor 
     * @throws CoreException 
     */
    @Override
    protected IFile doCreateNew(IProgressMonitor monitor) throws CoreException {
        IContainer validatedSourceFolder = filePage.getValidatedSourceFolder();
        if (validatedSourceFolder == null) {
            return null;
        }
        IContainer validatedPackage = filePage.getValidatedPackage();
        if (validatedPackage == null) {
            String packageText = filePage.getPackageText();
            if (packageText == null) {
                Log.log("Package text not available");
                return null;
            }
            IFile packageInit = PythonPackageWizard.createPackage(monitor, validatedSourceFolder, packageText);
            if (packageInit == null) {
                Log.log("Package not created");
                return null;
            }
            validatedPackage = packageInit.getParent();
        }
        String validatedName = filePage.getValidatedName() + FileTypesPreferencesPage.getDefaultDottedPythonExtension();

        IFile file = validatedPackage.getFile(new Path(validatedName));
        if (!file.exists()) {
            file.create(new ByteArrayInputStream(new byte[0]), true, monitor);
        }

        return file;
    }

    /**
     * Applies the template if one was specified.
     */
    @Override
    protected void afterEditorCreated(IEditorPart openEditor) {
        if (!(openEditor instanceof PyEdit)) {
            return; //only works for PyEdit...
        }

        TemplatePersistenceData selectedTemplate = filePage.getSelectedTemplate();
        if (selectedTemplate == null) {
            return; //no template selected, nothing to apply!
        }

        Template template = selectedTemplate.getTemplate();

        PyEdit pyEdit = (PyEdit) openEditor;
        Region region = new Region(0, 0);
        PyDocumentTemplateContext context = PyTemplateCompletionProcessor.createContext(new PyContextType(),
                pyEdit.getPySourceViewer(), region);

        TemplateProposal templateProposal = new TemplateProposal(template, context, region, null);
        templateProposal.apply(pyEdit.getPySourceViewer(), '\n', 0, 0);
    }

}
