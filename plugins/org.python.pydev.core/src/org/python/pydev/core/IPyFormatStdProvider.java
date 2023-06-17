package org.python.pydev.core;

import java.io.File;

import org.eclipse.core.runtime.IAdaptable;

public interface IPyFormatStdProvider extends IAdaptable {

    Object /*FormatStd*/ getFormatStd();

    IPythonNature getPythonNature() throws MisconfigurationException;

    IGrammarVersionProvider getGrammarVersionProvider();

    IIndentPrefs getIndentPrefs();

    /**
     * The editor file is needed because different file extensions may
     * have different formatting applied (i.e.: .py, .pyi).
     *
     * Note that it may return null (especially in tests).
     */
    File getEditorFile();
}
