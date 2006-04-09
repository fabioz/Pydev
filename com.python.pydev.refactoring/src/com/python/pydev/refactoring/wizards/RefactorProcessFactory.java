/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import org.python.pydev.core.docutils.DocUtils;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

public class RefactorProcessFactory {

    public static PyRenameLocalProcess getProcess(Definition definition) {
        if(definition instanceof AssignDefinition){
            AssignDefinition d = (AssignDefinition) definition;
            if(DocUtils.isWord(d.target)){
                return new PyRenameLocalProcess(definition);
            }else{
                return new PyRenameLocalProcess(definition);
            }
        }
        return new PyRenameLocalProcess(definition);

    }

}
