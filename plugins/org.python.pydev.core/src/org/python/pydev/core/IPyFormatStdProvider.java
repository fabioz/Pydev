package org.python.pydev.core;

public interface IPyFormatStdProvider {

    Object /*FormatStd*/getFormatStd();

    IPythonNature getPythonNature() throws MisconfigurationException;

    IGrammarVersionProvider getGrammarVersionProvider();

    IIndentPrefs getIndentPrefs();

}
