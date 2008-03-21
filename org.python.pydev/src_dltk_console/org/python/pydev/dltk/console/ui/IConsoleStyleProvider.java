package org.python.pydev.dltk.console.ui;


/**
 * Interface that will create the style range for the contents entered in the console.
 */
public interface IConsoleStyleProvider {

    ScriptStyleRange createPromptStyle(String prompt, int offset);

    ScriptStyleRange createUserInputStyle(String content, int offset);

    ScriptStyleRange createInterpreterOutputStyle(String content, int offset);

    ScriptStyleRange createInterpreterErrorStyle(String content, int offset);
}
