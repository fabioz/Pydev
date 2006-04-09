/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

public class RefactorProcessFactory {

    public static IRefactorProcess getProcess(Definition definition) {
        if(definition instanceof AssignDefinition){
            AssignDefinition d = (AssignDefinition) definition;
            if(d.target.indexOf('.') != -1){
                //ok, it is a member and not a local
                return null;
                
            }else{
                return new PyRenameLocalProcess(definition);
            }
        }
        return new PyRenameLocalProcess(definition);

    }

}
