/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.revisited.modules;

import org.python.pydev.ast.codecompletion.revisited.AbstractToken;
import org.python.pydev.ast.codecompletion.revisited.CompletionState;
import org.python.pydev.ast.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.core.BaseModuleRequest;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.callbacks.ICallback;

/**
 * @author Fabio Zadrozny
 */
public class SourceToken extends AbstractToken {

    private static final long serialVersionUID = 1L;

    private static final ICallback<String, IToken> DEFAULT_COMPUTE_DOCSTRING = (token) -> {
        if (token instanceof SourceToken) {
            SourceToken sourceToken = (SourceToken) token;
            if (sourceToken.getAst() != null) {
                return NodeUtils.printAst(null, sourceToken.getAst());
            }
        }
        return "";
    };

    private static final ThreadLocal<Boolean> threadLocalPreventRecursion = new ThreadLocal<>();

    private static final ICallback<String, IToken> DEFAULT_COMPUTE_DOCSTRING_PREDEFINED = (token) -> {
        if (token instanceof SourceToken) {
            Boolean curr = threadLocalPreventRecursion.get();
            if (curr == null || curr == false) {
                threadLocalPreventRecursion.set(true);
            } else {
                Log.log("Prevented recursion when getting source token docs.");
                return "";
            }
            try {
                SourceToken sourceToken = (SourceToken) token;
                IPythonNature nature = sourceToken.getNature();
                BaseModuleRequest request = new BaseModuleRequest(false);
                SimpleNode ast = sourceToken.getAst();
                if (ast != null) {
                    String signatureDoc = NodeUtils.printAst(null, ast);
                    if (nature != null) {
                        String qualifier = NodeUtils.getRepresentationString(ast);
                        if (qualifier != null) {
                            ICodeCompletionASTManager astManager = nature.getAstManager();
                            IModule realModule = astManager.getModule(sourceToken.parentPackage, nature, false,
                                    request);
                            if (realModule != null && realModule != sourceToken.module) {
                                String actTok = "";
                                if (ast.parent != null) {
                                    actTok = NodeUtils.getFullMethodName(ast.parent);
                                }
                                CompletionState state = new CompletionState(ast.beginLine - 1, ast.beginColumn - 1,
                                        actTok, nature, "");
                                try {
                                    TokensList completionsForModule = astManager.getCompletionsForModule(realModule,
                                            state);
                                    IToken found = completionsForModule.find(qualifier);
                                    if (found != null) {
                                        if (found.equals(token)) {
                                            // i.e.: we don't want to recurse...
                                            return signatureDoc;
                                        }
                                        String docStr = found.getDocStr();
                                        if (docStr == null || docStr.trim().length() == 0) {
                                            return signatureDoc;
                                        }
                                        return docStr + "\n\n" + signatureDoc;
                                    }
                                } catch (CompletionRecursionException e) {
                                    Log.log(e);
                                }
                            }
                        }
                    }
                    return signatureDoc;
                }
            } finally {
                threadLocalPreventRecursion.set(false);
            }
        }
        return "";
    };

    /**
     * The AST that generated this SourceToken
     */
    private SimpleNode ast;

    /**
     * If this token ended up being an alias to a function def, this is the original def.
     */
    private FunctionDef aliased;

    /**
     * Note: may be null.
     */
    public final IModule module;

    public SourceToken(SimpleNode node, String rep, String args, String doc, String parentPackage,
            IPythonNature nature, IModule module) {
        super(rep, doc, args, parentPackage, getType(node), nature);
        this.ast = node;
        this.module = module;
        setDefaultComputeDocstring();
    }

    /**
     * @param node
     */
    public SourceToken(SimpleNode node, String rep, String args, String doc, String parentPackage, int type,
            IPythonNature nature, IModule module) {
        super(rep, doc, args, parentPackage, type, nature);
        this.ast = node;
        this.module = module;
        setDefaultComputeDocstring();
    }

    /**
     * @param node
     */
    public SourceToken(SimpleNode node, String rep, String doc, String args, String parentPackage, String originalRep,
            boolean originalHasRep, IPythonNature nature, IModule module) {
        super(rep, doc, args, parentPackage, getType(node), originalRep, originalHasRep, nature);
        this.ast = node;
        this.module = module;
        setDefaultComputeDocstring();
    }

    private void setDefaultComputeDocstring() {
        if (this.module instanceof PredefinedSourceModule) {
            computeDocstring = DEFAULT_COMPUTE_DOCSTRING_PREDEFINED;

        } else if (ast instanceof FunctionDef || ast instanceof ClassDef) {
            computeDocstring = DEFAULT_COMPUTE_DOCSTRING;
        }
    }

    /**
     *
     * @return the completion type depending on the syntax tree.
     */
    public static int getType(SimpleNode ast) {
        if (ast instanceof ClassDef) {
            return IToken.TYPE_CLASS;

        } else if (ast instanceof FunctionDef) {
            return IToken.TYPE_FUNCTION;

        } else if (ast instanceof Name) {
            return IToken.TYPE_ATTR;

        } else if (ast instanceof Import || ast instanceof ImportFrom) {
            return IToken.TYPE_IMPORT;

        } else if (ast instanceof keywordType || ast instanceof Attribute || ast instanceof NameTok) {
            return IToken.TYPE_ATTR;
        }

        return IToken.TYPE_UNKNOWN;
    }

    public void setAst(SimpleNode ast) {
        this.ast = ast;
    }

    public SimpleNode getAst() {
        return ast;
    }

    /**
     * @return line starting at 1
     */
    @Override
    public int getLineDefinition() {
        return NodeUtils.getLineDefinition(getRepresentationNode());
    }

    private SimpleNode getRepresentationNode() {
        if (ast instanceof Attribute) {
            Attribute attr = (Attribute) ast;
            while (attr != null) {
                String r = NodeUtils.getRepresentationString(attr);
                if (r != null && r.equals(rep)) {
                    return attr;
                }
                if (attr.value instanceof Attribute) {
                    attr = (Attribute) attr.value;
                } else {
                    r = NodeUtils.getRepresentationString(attr.value);
                    if (r != null && r.equals(rep)) {
                        return attr.value;
                    }
                    break;
                }
            }
        }
        return ast;
    }

    /**
     * @return col starting at 1
     */
    @Override
    public int getColDefinition() {
        return NodeUtils.getColDefinition(ast);
    }

    int[] colLineEndToFirstDot;
    int[] colLineEndComplete;

    public int getLineEnd(boolean getOnlyToFirstDot) {
        if (getOnlyToFirstDot) {
            if (colLineEndToFirstDot == null) {
                colLineEndToFirstDot = NodeUtils.getColLineEnd(getRepresentationNode(), getOnlyToFirstDot);
            }
            return colLineEndToFirstDot[0];

        } else {
            if (colLineEndComplete == null) {
                colLineEndComplete = NodeUtils.getColLineEnd(getRepresentationNode(), getOnlyToFirstDot);
            }
            return colLineEndComplete[0];
        }
    }

    public int getColEnd(boolean getOnlyToFirstDot) {
        if (getOnlyToFirstDot) {
            if (colLineEndToFirstDot == null) {
                colLineEndToFirstDot = NodeUtils.getColLineEnd(getRepresentationNode(), getOnlyToFirstDot);
            }
            return colLineEndToFirstDot[1];

        } else {
            if (colLineEndComplete == null) {
                colLineEndComplete = NodeUtils.getColLineEnd(getRepresentationNode(), getOnlyToFirstDot);
            }
            return colLineEndComplete[1];
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SourceToken)) {
            return false;
        }

        SourceToken s = (SourceToken) obj;

        if (!s.getRepresentation().equals(getRepresentation())) {
            return false;
        }
        if (s.getLineDefinition() != getLineDefinition()) {
            return false;
        }
        if (s.getColDefinition() != getColDefinition()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return 7 * getLineDefinition() * getColDefinition();
    }

    @Override
    public boolean isImport() {
        if (ast instanceof Import || ast instanceof ImportFrom) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isImportFrom() {
        return ast instanceof ImportFrom;
    }

    @Override
    public boolean isWildImport() {
        return ast instanceof ImportFrom && AbstractVisitor.isWildImport((ImportFrom) ast);
    }

    @Override
    public boolean isString() {
        return AbstractVisitor.isString(ast);
    }

    /**
     * This representation may not be accurate depending on which tokens we are dealing with.
     */
    @Override
    public int[] getLineColEnd() {
        if (ast instanceof NameTok || ast instanceof Name) {
            //those are the ones that we can be certain of...
            return new int[] { getLineDefinition(), getColDefinition() + getRepresentation().length() };
        }
        throw new RuntimeException("Unable to get the lenght of the token:" + ast.getClass().getName());
    }

    /**
     * Updates the parameter, type and docstring based on another token (used for aliases).
     */
    public void updateAliasToken(SourceToken methodTok) {
        this.args = methodTok.getArgs();
        this.type = methodTok.getType();
        this.doc = methodTok.getDocStr();
        SimpleNode localAst = methodTok.getAst();
        if (localAst instanceof FunctionDef) {
            this.aliased = (FunctionDef) localAst;
        } else {
            this.aliased = methodTok.getAliased();
        }
    }

    /**
     * @return the function def to which this token is an alias to (or null if it's not an alias).
     */
    public FunctionDef getAliased() {
        return aliased;
    }

    private Definition definition;

    private Assign foundInAssign;

    private Assign dummyAssignFromParam;

    private Call foundInCall;

    public void setDefinition(Definition d) {
        this.definition = d;
    }

    public Definition getDefinition() {
        return this.definition;
    }

    public void setFoundInAssign(Assign foundInAssign) {
        this.foundInAssign = foundInAssign;
    }

    public Assign getFoundInAssign() {
        return foundInAssign;
    }

    public void setFoundInCall(Call foundInCall) {
        this.foundInCall = foundInCall;
    }

    public Call getFoundInCall() {
        return foundInCall;
    }

    public void setDummyAssignFromParam(Assign dummyAssignFromParam) {
        this.dummyAssignFromParam = dummyAssignFromParam;
    }

    public Assign getDummyAssignFromParam() {
        return dummyAssignFromParam;
    }

}
