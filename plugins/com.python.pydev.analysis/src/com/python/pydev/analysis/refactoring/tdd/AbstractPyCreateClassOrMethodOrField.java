/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.refactoring.tdd;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.Template;
import org.python.pydev.ast.adapters.IClassDefAdapter;
import org.python.pydev.ast.adapters.ModuleAdapter;
import org.python.pydev.ast.adapters.offsetstrategy.BeginOffset;
import org.python.pydev.ast.adapters.offsetstrategy.EndOffset;
import org.python.pydev.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.ast.assist_assign.AssistAssign;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.templates.PyDocumentTemplateContext;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

public abstract class AbstractPyCreateClassOrMethodOrField extends AbstractPyCreateAction {

    public abstract String getCreationStr();

    public abstract String getDefaultActTok();

    protected TemplateInfo createProposal(PySelection pySelection, String source,
            Tuple<Integer, String> offsetAndIndent) {
        return createProposal(pySelection, source, offsetAndIndent, true, null);
    }

    protected TemplateInfo createProposal(PySelection pySelection, String source,
            Tuple<Integer, String> offsetAndIndent, boolean requireEmptyLines, Pass replacePassStatement) {
        int offset;
        int len;
        String indent = offsetAndIndent.o2;

        if (replacePassStatement == null) {
            len = 0;
            offset = offsetAndIndent.o1;
            if (requireEmptyLines) {
                int checkLine = pySelection.getLineOfOffset(offset);
                int lineOffset = pySelection.getLineOffset(checkLine);

                //Make sure we have 2 spaces from the last thing written.
                if (lineOffset == offset) {
                    //it'll be added to the start of the line, so, we have to analyze the previous line to know if we'll need
                    //to new lines at the start.
                    checkLine--;
                }

                if (checkLine >= 0) {
                    //It'll be added to the current line, so, check the current line and the previous line to know about spaces.
                    String line = pySelection.getLine(checkLine);
                    if (line.trim().length() >= 1) {
                        source = "\n\n" + source;
                    } else if (checkLine > 1) {
                        line = pySelection.getLine(checkLine - 1);
                        if (line.trim().length() > 0) {
                            source = "\n" + source;
                        }
                    }
                }

                //If we have a '\n', all is OK (all contents after a \n will be indented)
                if (!source.startsWith("\n")) {
                    try {
                        //Ok, it doesn't start with a \n, that means we have to check the line indentation where it'll
                        //be added and make sure things are correct (eventually adding a new line or just fixing the indent).
                        String lineContentsToCursor = pySelection.getLineContentsToCursor(offset);
                        if (lineContentsToCursor.length() > 0) {
                            source = "\n" + source;
                        } else {
                            source = indent + source;
                        }
                    } catch (BadLocationException e) {
                        source = "\n" + source;
                    }
                }
            }
        } else {
            offset = pySelection.getAbsoluteCursorOffset(replacePassStatement.beginLine - 1,
                    replacePassStatement.beginColumn - 1);
            len = 4; //pass.len

            if (requireEmptyLines) {
                source = "\n\n" + source;
            }
        }

        String creationStr = getCreationStr();
        Region region = new Region(offset, len);
        //Note: was using new PyContextType(), but when we had something as ${user} it
        //would end up replacing it with the actual name of the user, which is not what
        //we want!
        PyDocumentTemplateContext context;
        if (targetEditor != null) {
            context = PyDocumentTemplateContext.createContextWithCursor(targetEditor, region,
                    indent);
        } else {
            context = PyDocumentTemplateContext.createContextWithCursor(targetEditor, pySelection.getDoc(), region,
                    indent);
        }

        Template template = new Template("Create " + creationStr, "Create " + creationStr, "", source, true);
        return new TemplateInfo(template, context, region);
    }

    /**
     * @return the offset and the indent to be used.
     */
    protected Tuple<Integer, String> getLocationOffset(int locationStrategy, PySelection pySelection,
            ModuleAdapter moduleAdapter, IClassDefAdapter targetClass) {
        Assert.isNotNull(targetClass);

        int offset;
        IOffsetStrategy strategy;
        try {
            switch (locationStrategy) {
                case LOCATION_STRATEGY_BEFORE_CURRENT:
                    String currentLine = pySelection.getLine();
                    int firstCharPosition = PySelection.getFirstCharPosition(currentLine);

                    LineStartingScope scopeStart = pySelection.getPreviousLineThatStartsScope(
                            PySelection.CLASS_AND_FUNC_TOKENS, false, firstCharPosition);

                    if (scopeStart == null) {
                        Log.log("Could not get proper scope to create code inside class.");
                        ClassDef astNode = targetClass.getASTNode();
                        if (astNode.body.length > 0) {
                            offset = NodeUtils.getLineEnd(astNode.body[astNode.body.length - 1]);

                        } else {
                            offset = NodeUtils.getLineEnd(astNode);
                        }
                    } else {
                        int iLineStartingScope = scopeStart.iLineStartingScope;
                        String line = pySelection.getLine(iLineStartingScope);

                        if (PySelection.matchesFunctionLine(line) || PySelection.matchesClassLine(line)) {
                            //check for decorators...
                            if (iLineStartingScope > 0) {
                                int i = iLineStartingScope - 1;
                                while (pySelection.getLine(i).trim().startsWith("@")) {
                                    iLineStartingScope = i;
                                    i--;
                                }
                            }
                        }
                        offset = pySelection.getLineOffset(iLineStartingScope);
                    }

                    break;

                case LOCATION_STRATEGY_END:
                    strategy = new EndOffset(targetClass, pySelection.getDoc(), moduleAdapter.getAdapterPrefs());
                    offset = strategy.getOffset();

                    break;

                case LOCATION_STRATEGY_FIRST_METHOD:
                    strategy = new BeginOffset(targetClass, pySelection.getDoc(), moduleAdapter.getAdapterPrefs());
                    offset = strategy.getOffset();

                    break;

                default:
                    throw new AssertionError("Unknown location strategy: " + locationStrategy);
            }
            String nodeBodyIndent = targetClass.getNodeBodyIndent();
            return new Tuple<Integer, String>(offset, nodeBodyIndent);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    protected Tuple<Integer, String> getLocationOffset(int locationStrategy, PySelection pySelection,
            ModuleAdapter moduleAdapter) throws AssertionError {
        int offset;
        switch (locationStrategy) {
            case LOCATION_STRATEGY_BEFORE_CURRENT:
                int lastNodeFirstLineBefore = moduleAdapter.getLastNodeFirstLineBefore(pySelection.getCursorLine() + 1);
                lastNodeFirstLineBefore = lastNodeFirstLineBefore - 1; // From AST line to doc line

                int line = lastNodeFirstLineBefore;
                if (line > 0) {
                    try {
                        String trimmed = pySelection.getLine(line).trim();
                        if (trimmed.startsWith("class") || trimmed.startsWith("def")) {
                            //if we'll add to a class or def line, let's see if there are comments just above it
                            //(in this case, we'll go backwards in the file until the block comment ends)
                            //i.e.:
                            //#======================
                            //# Existing
                            //#======================
                            //class Existing(object): <-- currently at this line
                            //    passMyClass()
                            int curr = line;
                            while (curr >= 0) {
                                line = curr;
                                if (curr - 1 < 0) {
                                    break;
                                }
                                if (!pySelection.getLine(curr - 1).trim().startsWith("#")) {
                                    break;
                                }

                                curr--;
                            }
                        }
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }

                offset = pySelection.getLineOffset(line);
                break;

            case LOCATION_STRATEGY_END:
                offset = pySelection.getEndOfDocummentOffset();

                break;

            default:
                throw new AssertionError("Unknown location strategy: " + locationStrategy);
        }
        return new Tuple<Integer, String>(offset, "");
    }

    public static FastStringBuffer createParametersList(List<String> parametersAfterCall) {
        FastStringBuffer params = new FastStringBuffer(parametersAfterCall.size() * 10);
        AssistAssign assistAssign = new AssistAssign();
        for (int i = 0; i < parametersAfterCall.size(); i++) {
            String param = parametersAfterCall.get(i).trim();
            if (params.length() > 0) {
                params.append(", ");
            }
            String tok = null;
            if (param.indexOf('=') != -1) {
                List<String> split = StringUtils.split(param, '=');
                if (split.size() > 0) {
                    String part0 = split.get(0).trim();
                    if (PyStringUtils.isPythonIdentifier(part0)) {
                        tok = part0;
                    }
                }
            }
            if (tok == null) {
                if (PyStringUtils.isPythonIdentifier(param)) {
                    tok = param;
                } else {
                    tok = assistAssign.getTokToAssign(param);
                    if (tok == null || tok.length() == 0) {
                        tok = "param" + i;
                    }
                }
            }
            boolean addTag = !(i == 0 && (tok.equals("cls") || tok.equals("self")));
            if (addTag) {
                params.append("${");
            }
            params.append(tok);
            if (addTag) {
                params.append("}");
            }
        }
        return params;
    }
}
