/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.io.File;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.source.ISourceViewer;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.ui.ColorAndStyleCache;

/**
 * This is the interface needed for an editor that has syntax highlighting and code-completion
 * (used by the PyEdit and PyMergeViewer -- in the compare editor).
 */
public interface IPySyntaxHighlightingAndCodeCompletionEditor extends IAdaptable, IGrammarVersionProvider {

    IIndentPrefs getIndentPrefs();

    ISourceViewer getEditorSourceViewer();

    void resetForceTabs();

    ColorAndStyleCache getColorCache();

    PyEditConfigurationWithoutEditor getEditConfiguration();

    PySelection createPySelection();

    IPythonNature getPythonNature() throws MisconfigurationException;

    File getEditorFile();

    void resetIndentPrefixes();

}
