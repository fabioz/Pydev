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
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.core.base.RefactoringInfo;

public class PyCreateMethod extends PyCreateClassOrMethod{

    public static final int BOUND_METHOD = 0;
    public static final int CLASSMETHOD = 1;
    public static final int STATICMETHOD = 2;
    
    private String createInClass;
    private int createAs;


    public String getCreationStr(){
        return "method";
    }

    
    /**
     * Returns a proposal that can be used to generate the code.
     */
    public ICompletionProposal createProposal(
            RefactoringInfo refactoringInfo, String actTok, int locationStrategy, List<String> parametersAfterCall) {
        PySelection pySelection = refactoringInfo.getPySelection();
        ModuleAdapter moduleAdapter = refactoringInfo.getModuleAdapter();
        String decorators = "";
        
        IClassDefAdapter targetClass = null;
        if(createInClass != null){
            List<IClassDefAdapter> classes = moduleAdapter.getClasses();
            for (IClassDefAdapter iClassDefAdapter : classes) {
                if(createInClass.equals(iClassDefAdapter.getName())){
                    targetClass = iClassDefAdapter;
                    break;
                }
            }
            
            if(targetClass != null){
                switch(createAs){
                    case BOUND_METHOD:
                        parametersAfterCall = checkFirst(parametersAfterCall, "self");
                        break;
                    case CLASSMETHOD:
                        parametersAfterCall = checkFirst(parametersAfterCall, "cls");
                        decorators = "@classmethod\n";
                        break;
                    case STATICMETHOD:
                        decorators = "@staticmethod\n";
                        break;
                }
            }
        }

        String source;
        if(parametersAfterCall == null || parametersAfterCall.size()== 0){
            source = "" +
            "${decorators}" +
            "def "+actTok+"():\n" +
            "    ${pass}${cursor}\n" +
            "\n" +
            "\n" +
            "";
        }else{
            FastStringBuffer params = createParametersList(parametersAfterCall);

            source = "" +
            "${decorators}" +
            "def "+actTok+"("+params+"):\n" +
            "    ${pass}${cursor}\n" +
            "\n" +
            "\n" +
            "";
        }
        source = StringUtils.replaceAll(source, "${decorators}", decorators);

        
        Tuple<Integer, String> offsetAndIndent;
        if(targetClass != null){
            offsetAndIndent = getLocationOffset(locationStrategy, pySelection, moduleAdapter, targetClass);
            
        }else{
            offsetAndIndent = getLocationOffset(locationStrategy, pySelection, moduleAdapter);
        }
        
        return createProposal(pySelection, source, offsetAndIndent);
    }



    private List<String> checkFirst(List<String> parametersAfterCall, String first) {
        if(parametersAfterCall.size() == 0){
            parametersAfterCall.add(first);
        }else{
            String string = parametersAfterCall.get(0);
            if(!first.equals(string)){
                parametersAfterCall.add(0, first);
            }
        }
        return parametersAfterCall;
    }


    public void setCreateInClass(String createInClass) {
       this.createInClass = createInClass;
    }


    public void setCreateAs(int createAs) {
        this.createAs = createAs;
    }
}

