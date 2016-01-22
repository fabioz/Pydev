/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
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

package org.python.pydev.refactoring.ui.core;

import java.util.LinkedList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.refactoring.PepticPlugin;
import org.python.pydev.refactoring.core.base.AbstractPythonRefactoring;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;

public class PythonRefactoringWizard extends RefactoringWizard {
    protected AbstractPythonRefactoring refactoring;
    private ITextEditor targetEditor;
    private LinkedList<IWizardPage> pages;

    public PythonRefactoringWizard(AbstractPythonRefactoring refactoring, ITextEditor targetEditor, IWizardPage page,
            int flags) {
        super(refactoring, flags);

        ImageDescriptor wizardImg = PepticPlugin.imageDescriptorFromPlugin(PepticPlugin.PLUGIN_ID, Messages.imagePath
                + Messages.imgLogo);

        this.targetEditor = targetEditor;
        this.refactoring = refactoring;
        this.setDefaultPageImageDescriptor(wizardImg);
        this.setWindowTitle(refactoring.getName());
        this.setDefaultPageTitle(refactoring.getName());
        this.pages = new LinkedListWarningOnSlowOperations<IWizardPage>();

        this.pages.add(page);

    }

    @Override
    protected void addUserInputPages() {
        this.getShell().setMinimumSize(640, 480);
        for (IWizardPage page : pages) {
            addPage(page);
        }
    }

    public void run() {
        try {
            RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(this);

            op.run(getShell(), refactoring.getName());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Looks for an usable shell
     */
    @Override
    public Shell getShell() {
        return targetEditor != null ? targetEditor.getSite().getShell() : PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getShell();
    }
}
