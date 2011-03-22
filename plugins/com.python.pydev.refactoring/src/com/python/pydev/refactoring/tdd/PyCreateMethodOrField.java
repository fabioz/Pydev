/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.core.base.RefactoringInfo;

public class PyCreateMethodOrField extends AbstractPyCreateClassOrMethodOrField{

    public static final int BOUND_METHOD = 0;
    public static final int CLASSMETHOD = 1;
    public static final int STATICMETHOD = 2;
    public static final int FIELD = 3;
    
    private String createInClass;
    private int createAs;


    public String getCreationStr(){
        if(createAs != FIELD){
            return "method";
        }
        return "field";
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
        String body = "${pass}";
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
                    case FIELD:
                        
                        parametersAfterCall = checkFirst(parametersAfterCall, "self");
                        FunctionDefAdapter firstInit = targetClass.getFirstInit();
                        if(firstInit != null){
                            FunctionDef astNode = firstInit.getASTNode();
                            
                            Pass replacePassStatement = null;
                            if(astNode.body.length > 0){
                                 SimpleNode lastNode = astNode.body[astNode.body.length-1];
                                 if(lastNode instanceof Pass){
                                     //Remove the pass and add the statement!
                                     replacePassStatement = (Pass) lastNode;
                                 }
                            }
                            
                            //Create the field as the last line in the __init__
                            int nodeLastLine = firstInit.getNodeLastLine();
                            Tuple<Integer, String> offsetAndIndent;
                            int nodeBodyIndent = firstInit.getNodeBodyIndent();
                            String indent = new FastStringBuffer(nodeBodyIndent).appendN(' ', nodeBodyIndent).toString();
                            int offset = pySelection.getLineOffset(nodeLastLine);
                            offsetAndIndent = new Tuple<Integer, String>(offset, "");
                            boolean isReplace = replacePassStatement!=null;
                            return createProposal(
                                    pySelection, 
                                    StringUtils.format("%sself.%s = ${None}%s", isReplace?"":indent, actTok, isReplace?"":"\n"), 
                                    offsetAndIndent, 
                                    false,
                                    replacePassStatement);

                        }else{
                            //Create the __init__ with the field declaration!
                            body = StringUtils.format("self.%s = ${None}", actTok);
                            actTok = "__init__";
                            locationStrategy = AbstractPyCreateAction.LOCATION_STRATEGY_FIRST_METHOD;
                        }
                        
                        
                        break;
                }
            }else{
                //We should create in a class and couldn't find it!
                return null;
            }
        }

        String params = "";
        String source;
        if(parametersAfterCall != null && parametersAfterCall.size() > 0){
            params = createParametersList(parametersAfterCall).toString();
        }
        
        source = StringUtils.format("" +
                "%sdef %s(%s):\n" +
                "    %s${cursor}\n" +
                "\n" +
                "\n" +
                "", decorators, actTok, params, body);

        
        Tuple<Integer, String> offsetAndIndent;
        if(targetClass != null){
            offsetAndIndent = getLocationOffset(locationStrategy, pySelection, moduleAdapter, targetClass);
            
        }else{
            offsetAndIndent = getLocationOffset(locationStrategy, pySelection, moduleAdapter);
        }
        
        return createProposal(pySelection, source, offsetAndIndent);
    }



    private List<String> checkFirst(List<String> parametersAfterCall, String first) {
        if(parametersAfterCall == null){
            parametersAfterCall = new ArrayList<String>();
        }
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

