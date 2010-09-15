package org.python.pydev.editor;

import java.io.File;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.source.ISourceViewer;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.ui.ColorAndStyleCache;

/**
 * This is the interface needed for an editor that has syntax highlighting and code-completion
 * (used by the PyEdit and PyMergeViewer -- in the compare editor).
 */
public interface IPySyntaxHighlightingAndCodeCompletionEditor extends IAdaptable {

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
