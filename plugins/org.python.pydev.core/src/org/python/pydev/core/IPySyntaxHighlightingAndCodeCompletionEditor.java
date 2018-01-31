/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.File;

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.docutils.PySelection;

/**
 * This is the interface needed for an editor that has syntax highlighting and code-completion
 * (used by the PyEdit and PyMergeViewer -- in the compare editor).
 */
public interface IPySyntaxHighlightingAndCodeCompletionEditor extends IAdaptable, IGrammarVersionProvider {

    IIndentPrefs getIndentPrefs();

    /* ISourceViewer*/ Object getEditorSourceViewer();

    void resetForceTabs();

    IColorCache /* ColorAndStyleCache*/ getColorCache();

    IPyEditConfigurationWithoutEditor /*PyEditConfigurationWithoutEditor*/ getEditConfiguration();

    PySelection createPySelection();

    IPythonNature getPythonNature() throws MisconfigurationException;

    File getEditorFile();

    void resetIndentPrefixes();

}
