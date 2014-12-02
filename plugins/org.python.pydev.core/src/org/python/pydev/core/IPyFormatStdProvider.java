package org.python.pydev.core;

import org.eclipse.core.runtime.IAdaptable;

public interface IPyFormatStdProvider extends IAdaptable {

    Object /*FormatStd*/getFormatStd();

    IPythonNature getPythonNature() throws MisconfigurationException;

    IGrammarVersionProvider getGrammarVersionProvider();

    IIndentPrefs getIndentPrefs();

}
