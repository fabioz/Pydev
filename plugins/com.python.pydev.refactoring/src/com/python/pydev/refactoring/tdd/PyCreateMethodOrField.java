/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class PyCreateMethodOrField extends AbstractPyCreateClassOrMethodOrField {

    public static final int BOUND_METHOD = 0;
    public static final int CLASSMETHOD = 1;
    public static final int STATICMETHOD = 2;
    public static final int FIELD = 3;
    public static final int CONSTANT = 4;

    private String createInClass;
    private int createAs;

    @Override
    public String getCreationStr() {
        if (createAs == FIELD) {
            return "field";
        }
        if (createAs == CONSTANT) {
            return "constant";
        }
        return "method";
    }

    /**
     * Returns a proposal that can be used to generate the code.
     */
    @Override
    public ICompletionProposal createProposal(RefactoringInfo refactoringInfo, String actTok, int locationStrategy,
            List<String> parametersAfterCall) {
        PySelection pySelection = refactoringInfo.getPySelection();
        ModuleAdapter moduleAdapter = refactoringInfo.getModuleAdapter();
        String decorators = "";

        IClassDefAdapter targetClass = null;
        String body = "${pass}";
        if (createInClass != null) {
            List<IClassDefAdapter> classes = moduleAdapter.getClasses();
            for (IClassDefAdapter iClassDefAdapter : classes) {
                if (createInClass.equals(iClassDefAdapter.getName())) {
                    targetClass = iClassDefAdapter;
                    break;
                }
            }

            if (targetClass != null) {
                switch (createAs) {
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

                    case CONSTANT:
                        String indent = targetClass.getNodeBodyIndent();
                        Pass replacePassStatement = getLastPassFromNode(targetClass.getASTNode());

                        String constant = StringUtils.format("\n%s = ${None}${cursor}\n", actTok);
                        Tuple<Integer, String> offsetAndIndent;
                        offsetAndIndent = getLocationOffset(AbstractPyCreateAction.LOCATION_STRATEGY_FIRST_METHOD,
                                pySelection, moduleAdapter, targetClass);

                        return createProposal(pySelection, constant, offsetAndIndent, false, replacePassStatement);

                    case FIELD:

                        parametersAfterCall = checkFirst(parametersAfterCall, "self");
                        FunctionDefAdapter firstInit = targetClass.getFirstInit();
                        if (firstInit != null) {
                            FunctionDef astNode = firstInit.getASTNode();
                            replacePassStatement = getLastPassFromNode(astNode);

                            //Create the field as the last line in the __init__
                            int nodeLastLine = firstInit.getNodeLastLine() - 1;
                            indent = firstInit.getNodeBodyIndent();
                            String pattern;

                            if (replacePassStatement == null) {
                                pattern = StringUtils.format("\nself.%s = ${None}${cursor}", actTok);
                                try {
                                    IRegion region = pySelection.getDoc().getLineInformation(nodeLastLine);
                                    int offset = region.getOffset() + region.getLength();
                                    offsetAndIndent = new Tuple<Integer, String>(offset, indent);
                                } catch (BadLocationException e) {
                                    Log.log(e);
                                    return null;
                                }

                            } else {
                                pattern = StringUtils.format("self.%s = ${None}${cursor}", actTok);
                                offsetAndIndent = new Tuple<Integer, String>(-1, ""); //offset will be from the pass stmt
                            }
                            return createProposal(pySelection, pattern, offsetAndIndent, false, replacePassStatement);

                        } else {
                            //Create the __init__ with the field declaration!
                            body = StringUtils.format("self.%s = ${None}${cursor}", actTok);
                            actTok = "__init__";
                            locationStrategy = AbstractPyCreateAction.LOCATION_STRATEGY_FIRST_METHOD;
                        }

                        break;
                }
            } else {
                //We should create in a class and couldn't find it!
                return null;
            }
        }

        String params = "";
        String source;
        if (parametersAfterCall != null && parametersAfterCall.size() > 0) {
            params = createParametersList(parametersAfterCall).toString();
        }

        Tuple<Integer, String> offsetAndIndent;
        Pass replacePassStatement = null;
        if (targetClass != null) {
            replacePassStatement = getLastPassFromNode(targetClass.getASTNode());
            offsetAndIndent = getLocationOffset(locationStrategy, pySelection, moduleAdapter, targetClass);

        } else {
            offsetAndIndent = getLocationOffset(locationStrategy, pySelection, moduleAdapter);
        }

        source = StringUtils.format("" +
                "%sdef %s(%s):\n" +
                "%s%s${cursor}\n" +
                "\n" +
                "\n" +
                "", decorators, actTok,
                params, refactoringInfo.indentPrefs.getIndentationString(), body);

        return createProposal(pySelection, source, offsetAndIndent, true, replacePassStatement);
    }

    private Pass getLastPassFromNode(SimpleNode astNode) {
        stmtType[] body = NodeUtils.getBody(astNode);
        Pass replacePassStatement = null;
        if (body.length > 0) {
            SimpleNode lastNode = body[body.length - 1];
            if (lastNode instanceof Pass) {
                //Remove the pass and add the statement!
                replacePassStatement = (Pass) lastNode;
            }
        }
        return replacePassStatement;
    }

    private List<String> checkFirst(List<String> parametersAfterCall, String first) {
        if (parametersAfterCall == null) {
            parametersAfterCall = new ArrayList<String>();
        }
        if (parametersAfterCall.size() == 0) {
            parametersAfterCall.add(first);
        } else {
            String string = parametersAfterCall.get(0);
            if (!first.equals(string)) {
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
