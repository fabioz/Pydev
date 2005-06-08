/*
 * Created on May 9, 2005
 *
 * @author Fabio Zadrozny
 */
package com.python.pydev.refactoring.core;

import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;

/**
 * @author Fabio Zadrozny
 */
public class Refactorer extends  AbstractPyRefactoring{

    /**
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#extract(org.python.pydev.editor.PyEdit, int, int, int, int, java.lang.String, org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation)
     */
    public String extract(PyEdit editor, int beginLine, int beginCol, int endLine, int endCol, String name, Operation operation) {
        return null;
    }

    /**
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#rename(org.python.pydev.editor.PyEdit, int, int, java.lang.String, org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation)
     */
    public String rename(PyEdit editor, int beginLine, int beginCol, String name, Operation operation) {
        return null;
    }

    /**
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#findDefinition(org.python.pydev.editor.PyEdit, int, int, org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation)
     */
    public ItemPointer[] findDefinition(PyEdit editor, int beginLine, int beginCol, Operation operation) {
        return null;
    }

    /**
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#inlineLocalVariable(org.python.pydev.editor.PyEdit, int, int, org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation)
     */
    public String inlineLocalVariable(PyEdit editor, int beginLine, int beginCol, Operation operation) {
        return null;
    }

    /**
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#extractLocalVariable(org.python.pydev.editor.PyEdit, int, int, int, int, java.lang.String, org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation)
     */
    public String extractLocalVariable(PyEdit editor, int beginLine, int beginCol, int endLine, int endCol, String name, Operation operation) {
        return null;
    }

    /**
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#restartShell()
     */
    public void restartShell() {
        //no shell here
    }

    /**
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#killShell()
     */
    public void killShell() {
        //no shell here
    }

}
