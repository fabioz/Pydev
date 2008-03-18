package org.python.pydev.dltk.console.ui;

import org.eclipse.swt.custom.StyleRange;

/**
 * Interface that will create the style range for the contents entered in the console.
 */
public interface IConsoleStyleProvider {

    StyleRange createPromptStyle(String prompt, int offset);

    StyleRange createUserInputStyle(String content, int offset);

    StyleRange createInterpreterOutputStyle(String content, int offset);

    StyleRange createInterpreterErrorStyle(String content, int offset);
}
