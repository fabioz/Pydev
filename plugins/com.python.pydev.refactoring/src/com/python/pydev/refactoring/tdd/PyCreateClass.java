/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.core.base.RefactoringInfo;

/**
 * This class should be used to generate code for creating a new class. 
 */
public class PyCreateClass extends PyCreateClassOrMethod{

    public String getCreationStr(){
        return "class";
    }
    
    
    /**
     * Returns a proposal that can be used to generate the code.
     */
    public ICompletionProposal createProposal(
            RefactoringInfo refactoringInfo, String actTok, int locationStrategy, List<String> parametersAfterCall) {
        PySelection pySelection = refactoringInfo.getPySelection();
        ModuleAdapter moduleAdapter = refactoringInfo.getModuleAdapter();

        String source;
        if(parametersAfterCall == null || parametersAfterCall.size()== 0){
            source = "" +
            "class "+actTok+"(${object}):\n" +
            "    ${pass}${cursor}\n" +
            "\n" +
            "\n" +
            "";
        }else{
            FastStringBuffer params = createParametersList(parametersAfterCall);
            
            source = "" +
            "class "+actTok+"(${object}):\n" +
            "    \n" +
            "    def __init__(self, "+params+"):\n" +
            "        ${pass}${cursor}\n" +
            "\n" +
            "\n" +
            "";
        }
        
        Tuple<Integer, String> offsetAndIndent = getLocationOffset(locationStrategy, pySelection, moduleAdapter);
        
        return createProposal(pySelection, source, offsetAndIndent);
    }




}
