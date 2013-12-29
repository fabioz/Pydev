/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;
import org.python.pydev.shared_core.structure.Tuple;

public class ScopeAnalysis {

    public static List<ASTEntry> getAttributeReferences(String occurencesFor, SimpleNode simpleNode) {
        //default is accepting all
        return getAttributeReferences(occurencesFor, simpleNode, AttributeReferencesVisitor.ACCEPT_ALL);
    }

    /**
     * @return the list of entries with the name parts of attributes (not taking into account its first
     * part) that are equal to the occurencesFor string. 
     */
    public static List<ASTEntry> getAttributeReferences(String occurencesFor, SimpleNode simpleNode, int accept) {
        List<ASTEntry> ret = new ArrayList<ASTEntry>();

        AttributeReferencesVisitor visitor = AttributeReferencesVisitor.create(simpleNode, accept);
        Iterator<ASTEntry> iterator = visitor.getNamesIterator();

        while (iterator.hasNext()) {
            ASTEntry entry = iterator.next();
            String rep = NodeUtils.getFullRepresentationString(entry.node);
            if (rep.equals(occurencesFor)) {
                ret.add(entry);
            }
        }
        return ret;
    }

    /**
     * @param occurencesFor the string we're looking for
     * @param module the module where we want to find the occurrences
     * @param scope the scope we're in
     * @return tuple with:
     * 1st element: the node where the local was found (may be null)
     * 2nd element: a list of entries with the occurrences
     */
    public static Tuple<SimpleNode, List<ASTEntry>> getLocalOccurrences(String occurencesFor, IModule module,
            ILocalScope scope) {
        SimpleNode simpleNode = null;

        if (scope.getScopeStack().size() > 0) {
            simpleNode = (SimpleNode) scope.getScopeStack().peek();

        } else if (module instanceof SourceModule) {
            SourceModule m = (SourceModule) module;
            simpleNode = m.getAst();
        }

        if (simpleNode == null) {
            return new Tuple<SimpleNode, List<ASTEntry>>(null, new ArrayList<ASTEntry>());
        }

        return new Tuple<SimpleNode, List<ASTEntry>>(simpleNode, ScopeAnalysis.getLocalOccurrences(occurencesFor,
                simpleNode));
    }

    /**
     * @param occurencesFor the string we're looking for
     * @param simpleNode we will want the occurences below this node
     * @return a list of entries with the occurrences
     */
    public static List<ASTEntry> getLocalOccurrences(String occurencesFor, SimpleNode simpleNode) {
        return ScopeAnalysis.getLocalOccurrences(occurencesFor, simpleNode, true);
    }

    /**
     * @return a list of ast entries that are found inside strings.
     */
    public static List<ASTEntry> getStringOccurrences(final String occurencesFor, SimpleNode simpleNode) {
        final List<ASTEntry> ret = new ArrayList<ASTEntry>();

        SequencialASTIteratorVisitor visitor = new SequencialASTIteratorVisitor() {
            @Override
            public Object visitStr(Str node) throws Exception {
                String str = NodeUtils.getStringToPrint(node);
                List<Name> names = checkSimpleNodeForTokenMatch(occurencesFor, new ArrayList<Name>(), node, str);
                for (Name name : names) {
                    ASTEntry astEntryToAdd = atomic(name);
                    astEntryToAdd.setAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION,
                            AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_IN_STRING);
                    ret.add(astEntryToAdd);
                }
                return super.visitStr(node);
            }

        };
        try {
            simpleNode.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    /**
     * @return a list of ast entries that are found inside comments.
     */
    public static List<ASTEntry> getCommentOccurrences(final String occurencesFor, SimpleNode simpleNode) {
        final List<ASTEntry> ret = new ArrayList<ASTEntry>();

        SequencialASTIteratorVisitor visitor = new SequencialASTIteratorVisitor() {
            @Override
            protected Object unhandled_node(SimpleNode node) throws Exception {
                Object r = super.unhandled_node(node);
                //now, we have to check it for occurrences in comments and strings too... (and create 
                //names for those)
                checkNode(occurencesFor, ret, node);
                return r;
            }

            @Override
            public Object visitClassDef(ClassDef node) throws Exception {
                Object r = super.visitClassDef(node);
                checkNode(occurencesFor, ret, node);
                return r;
            }

            @Override
            public Object visitFunctionDef(FunctionDef node) throws Exception {
                Object r = super.visitFunctionDef(node);
                checkNode(occurencesFor, ret, node);
                return r;
            }

            private void checkNode(final String occurencesFor, final List<ASTEntry> ret, SimpleNode node) {
                List<Name> names = checkComments(node.specialsBefore, occurencesFor);
                names.addAll(checkComments(node.specialsAfter, occurencesFor));
                for (Name name : names) {
                    ASTEntry astEntryToAdd = atomic(name);
                    astEntryToAdd.setAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION,
                            AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_IN_COMMENT);
                    ret.add(astEntryToAdd);
                }
            }

        };
        try {
            simpleNode.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    /**
     * @return a list of occurrences with the matches we're looking for.
     * Does only return the first name in attributes if onlyFirstAttribPart is true (otherwise will check all attribute parts)
     */
    public static List<ASTEntry> getLocalOccurrences(final String occurencesFor, SimpleNode simpleNode,
            final boolean onlyFirstAttribPart) {
        List<ASTEntry> ret = new ArrayList<ASTEntry>();

        SequencialASTIteratorVisitor visitor = new SequencialASTIteratorVisitor() {

            @Override
            public Object visitAttribute(Attribute node) throws Exception {
                if (onlyFirstAttribPart) {
                    //this will visit the attribute parts if call, subscript, etc.
                    AbstractScopeAnalyzerVisitor.visitNeededAttributeParts(node, this);

                    List<SimpleNode> attributeParts = NodeUtils.getAttributeParts(node);
                    atomic(attributeParts.get(0)); //an attribute should always have many parts
                    traverse(attributeParts.get(0));
                    return null;
                } else {
                    return super.visitAttribute(node);
                }
            }
        };
        if (simpleNode instanceof FunctionDef) {
            //all that because we don't want to visit the name of the function if we've started in a function scope
            FunctionDef d = (FunctionDef) simpleNode;
            try {
                //decorators
                if (d.decs != null) {
                    for (decoratorsType dec : d.decs) {
                        if (dec != null) {
                            dec.accept(visitor);
                        }
                    }
                }

                //don't do d.args directly because we don't want to check the 'defaults'
                if (d.args != null) {
                    if (d.args.args != null) {
                        for (exprType arg : d.args.args) {
                            arg.accept(visitor);
                        }
                    }
                    if (d.args.vararg != null) {
                        d.args.vararg.accept(visitor);
                    }
                    if (d.args.kwarg != null) {
                        d.args.kwarg.accept(visitor);
                    }
                    //visit keyword only args
                    if (d.args.kwonlyargs != null) {
                        for (exprType expr : d.args.kwonlyargs) {
                            expr.accept(visitor);
                        }
                    }

                }

                //and at last... the body
                if (d.body != null) {
                    for (stmtType exp : d.body) {
                        if (exp != null) {
                            exp.accept(visitor);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {

            try {
                simpleNode.accept(visitor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Iterator<ASTEntry> iterator = visitor.getNamesIterator();
        while (iterator.hasNext()) {
            ASTEntry entry = iterator.next();
            //SimpleNode nameNode = entry.getNameNode();
            //if(!occurencesFor.isParamRename){
            //    if(nameNode instanceof NameTok){
            //        NameTok name = (NameTok) nameNode;
            //        if(name.ctx == NameTok.KeywordName){
            //            continue;
            //        }
            //   }
            //}
            if (occurencesFor.equals(entry.getName())) {
                ret.add(entry);
            }
        }
        return ret;
    }

    /**
     * @param specials a list that may contain comments
     * @param match a string to match in the comments
     * @return a list with names matching the gives token
     */
    public static List<Name> checkComments(List<Object> specials, String match) {
        List<Name> r = new ArrayList<Name>();

        if (specials != null) {
            for (Object s : specials) {
                if (s instanceof commentType) {
                    commentType comment = (commentType) s;
                    checkSimpleNodeForTokenMatch(match, r, comment, comment.id);
                }
            }
        }
        return r;
    }

    /**
     * Looks for a match in the given string and fills the List<Name> with Names according to those positions.
     * @return the list of names (same as ret)
     */
    public static List<Name> checkSimpleNodeForTokenMatch(String match, List<Name> ret, SimpleNode node,
            String fullString) {
        try {
            ArrayList<Integer> offsets = TokenMatching.getMatchOffsets(match, fullString);
            List<Integer> lineStartOffsets = PySelection.getLineStartOffsets(fullString);

            for (Integer offset : offsets) {
                int line = 0;
                Name name = new Name(match, Name.Artificial, false);

                for (Integer lineStartOffset : lineStartOffsets) {
                    if (line == 0 && lineStartOffset > 0) {
                        line = 1;//because it starts with a new line
                    }
                    if (lineStartOffset <= offset) {
                        name.beginLine = node.beginLine + line;
                        if (line == 0) {
                            name.beginColumn = node.beginColumn + offset - lineStartOffset;
                        } else {
                            name.beginColumn = offset - lineStartOffset + 1;
                        }
                    } else {
                        break;
                    }
                    line++;
                }
                ret.add(name);
            }
        } catch (CoreException e) {
            Log.log(e);
        }
        return ret;
    }

}
