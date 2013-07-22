/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.wizards.project;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * The second page in the New Project wizard must implement this interface.
 */
public interface IWizardNewProjectExistingSourcesPage extends IWizardPage {
    public List<IPath> getExistingSourceFolders();
}
