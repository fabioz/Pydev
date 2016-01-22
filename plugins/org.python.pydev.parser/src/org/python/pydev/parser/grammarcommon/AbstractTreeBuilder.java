/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;

import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Break;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Continue;
import org.python.pydev.parser.jython.ast.Delete;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.DictComp;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.ExtSlice;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Set;
import org.python.pydev.parser.jython.ast.SetComp;
import org.python.pydev.parser.jython.ast.Starred;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.StrJoin;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.UnaryOp;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.WithItem;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.sliceType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Provides the basic behavior for a tree builder (opening and closing node scopes).
 * 
 * Subclasses must provide actions where it's not common.
 * 
 * @author Fabio
 */
public abstract class AbstractTreeBuilder extends AbstractTreeBuilderHelpers {

    /**
     * Keeps the last opened node.
     */
    private SimpleNode lastOpened;

    /**
     * @return the last opened node.
     */
    public final SimpleNode getLastOpened() {
        return lastOpened;
    }

    private final FastStringBuffer tempBuffer = new FastStringBuffer(20);

    /**
     * Constructor
     */
    public AbstractTreeBuilder(JJTPythonGrammarState stack) {
        super(stack);
        if (!stack.getGrammar().generateTree) {
            throw new AssertionError("Should not create a tree builder if the grammar won't generate the AST.");
        }
    }

    /**
     * Subclasses must implement this method to deal with any node that's not properly handled in this base.
     * @param n the node that should be closed
     * @param arity the current number of nodes in the stack (found after the context was opened)
     * @return a new node representing the node that's having it's context closed.
     * @throws Exception
     */
    protected abstract SimpleNode onCloseNode(SimpleNode n, int arity) throws Exception;

    /**
     * Opens a new scope and returns a node to be used in this scope. This same node will later be called
     * in {@link #closeNode(SimpleNode, int)} to have its scope closed (and at that time it may be changed
     * for a new node that represents the scope more accurately.
     */
    public final SimpleNode openNode(final int id) {
        SimpleNode ret;

        switch (id) {

            case JJTFILE_INPUT:
                ret = new Module(null);
                break;

            case JJTFALSE:
                ret = new Name("False", Name.Load, true);
                break;

            case JJTTRUE:
                ret = new Name("True", Name.Load, true);
                break;

            case JJTNONE:
                ret = new Name("None", Name.Load, true);
                break;

            case JJTNAME:
            case JJTDOTTED_NAME:
                //the actual name will be set during the parsing (token image) -- see Name construct
                ret = new Name(null, Name.Load, false);
                break;

            case JJTNUM://the actual number will be set during the parsing (token image) -- see Num construct
                ret = new Num(null, -1, null);
                break;

            case JJTSTRING:
            case JJTUNICODE:
            case JJTBINARY:
                //the actual number will be set during the parsing (token image) -- see Num construct
                ret = new Str(null, -1, false, false, false);
                break;

            case JJTFOR_STMT:
                ret = new For(null, null, null, null);
                break;

            case JJTEXEC_STMT:
                ret = new Exec(null, null, null);
                break;

            case JJTPASS_STMT:
                ret = new Pass();
                break;

            case JJTBREAK_STMT:
                ret = new Break();
                break;

            case JJTCONTINUE_STMT:
                ret = new Continue();
                break;

            case JJTBEGIN_DECORATOR:
                ret = new decoratorsType(null, null, null, null, null, false);
                break;

            case JJTIF_STMT:
                ret = new If(null, null, null);
                break;

            case JJTAUG_PLUS:
                ret = new AugAssign(null, AugAssign.Add, null);
                break;
            case JJTAUG_MINUS:
                ret = new AugAssign(null, AugAssign.Sub, null);
                break;
            case JJTAUG_MULTIPLY:
                ret = new AugAssign(null, AugAssign.Mult, null);
                break;
            case JJTAUG_DOT:
                ret = new AugAssign(null, AugAssign.Dot, null);
                break;
            case JJTAUG_DIVIDE:
                ret = new AugAssign(null, AugAssign.Div, null);
                break;
            case JJTAUG_MODULO:
                ret = new AugAssign(null, AugAssign.Mod, null);
                break;
            case JJTAUG_AND:
                ret = new AugAssign(null, AugAssign.BitAnd, null);
                break;
            case JJTAUG_OR:
                ret = new AugAssign(null, AugAssign.BitOr, null);
                break;
            case JJTAUG_XOR:
                ret = new AugAssign(null, AugAssign.BitXor, null);
                break;
            case JJTAUG_LSHIFT:
                ret = new AugAssign(null, AugAssign.LShift, null);
                break;
            case JJTAUG_RSHIFT:
                ret = new AugAssign(null, AugAssign.RShift, null);
                break;
            case JJTAUG_POWER:
                ret = new AugAssign(null, AugAssign.Pow, null);
                break;
            case JJTAUG_FLOORDIVIDE:
                ret = new AugAssign(null, AugAssign.FloorDiv, null);
                break;

            case JJTOR_2OP:
                ret = new BinOp(null, BinOp.BitOr, null);
                break;
            case JJTXOR_2OP:
                ret = new BinOp(null, BinOp.BitXor, null);
                break;
            case JJTAND_2OP:
                ret = new BinOp(null, BinOp.BitAnd, null);
                break;
            case JJTLSHIFT_2OP:
                ret = new BinOp(null, BinOp.LShift, null);
                break;
            case JJTRSHIFT_2OP:
                ret = new BinOp(null, BinOp.RShift, null);
                break;
            case JJTADD_2OP:
                ret = new BinOp(null, BinOp.Add, null);
                break;
            case JJTSUB_2OP:
                ret = new BinOp(null, BinOp.Sub, null);
                break;
            case JJTMUL_2OP:
                ret = new BinOp(null, BinOp.Mult, null);
                break;
            case JJTDOT_2OP:
                ret = new BinOp(null, BinOp.Dot, null);
                break;
            case JJTDIV_2OP:
                ret = new BinOp(null, BinOp.Div, null);
                break;
            case JJTMOD_2OP:
                ret = new BinOp(null, BinOp.Mod, null);
                break;
            case JJTPOW_2OP:
                ret = new BinOp(null, BinOp.Pow, null);
                break;
            case JJTFLOORDIV_2OP:
                ret = new BinOp(null, BinOp.FloorDiv, null);
                break;

            case JJTPOS_1OP:
                ret = new UnaryOp(UnaryOp.UAdd, null);
                break;
            case JJTNEG_1OP:
                ret = new UnaryOp(UnaryOp.USub, null);
                break;
            case JJTINVERT_1OP:
                ret = new UnaryOp(UnaryOp.Invert, null);
                break;
            case JJTNOT_1OP:
                ret = new UnaryOp(UnaryOp.Not, null);
                break;

            case JJTIMPORT:
                ret = new Import(null);
                break;
            case JJTDOT_OP:
                ret = new Attribute(null, null, Attribute.Load);
                break;
            case JJTSTAR_EXPR:
                ret = new Starred(null, Starred.Store);
                break;

            default:
                ret = new IdentityNode(id);
                break;
        }
        ret.setId(id);
        lastOpened = ret;
        return ret;
    }

    /**
     * Subclasses must implement this method to deal with any node that's not properly handled in this base.
     * @param n the node that should be closed
     * @param arity the current number of nodes in the stack (found after the context was opened)
     * @return a new node representing the node that's having it's context closed.
     * @throws Exception
     */
    public final SimpleNode closeNode(final SimpleNode n, final int arity) throws Exception {
        exprType value;
        suiteType orelseSuite;
        stmtType[] body;
        exprType iter;
        exprType target;

        if (DEBUG_TREE_BUILDER) {
            System.out.println("\n\n\n---------------------------");
            System.out.println("Closing node scope: " + n);
            System.out.println("Arity: " + arity);
            if (arity > 0) {
                System.out.println("Nodes in scope: ");
                for (int i = 0; i < arity; i++) {
                    System.out.println(stack.peekNode(i));
                }
            }
        }

        exprType[] exprs;
        switch (n.getId()) {
            case -1:
                throw new ParseException("Illegal node found: " + n, n);

            case JJTFILE_INPUT:
                Module m = (Module) n;
                m.body = makeStmts(arity);
                return m;

            case JJTFALSE:
            case JJTTRUE:
            case JJTNONE:
            case JJTNAME:
            case JJTNUM:
            case JJTPASS_STMT:
            case JJTBREAK_STMT:
            case JJTCONTINUE_STMT:
            case JJTSTRING:
            case JJTUNICODE:
            case JJTBINARY:
            case JJTBEGIN_DECORATOR:
            case JJTCOMMA:
            case JJTCOLON:
                return n; //it's already the correct node (and it's value is already properly set)

            case JJTSUITE:
                stmtType[] stmts = new stmtType[arity];
                for (int i = arity - 1; i >= 0; i--) {
                    SimpleNode yield_or_stmt = stack.popNode();
                    if (yield_or_stmt instanceof Yield) {
                        stmts[i] = new Expr((Yield) yield_or_stmt);

                    } else {
                        try {
                            stmts[i] = (stmtType) yield_or_stmt;
                        } catch (ClassCastException e) {
                            recoverFromClassCastException(yield_or_stmt, e);
                            stmts[i] = new Pass(); //recover from it with a valid node!
                        }
                    }
                }
                return new Suite(stmts);

            case JJTFOR_STMT:
                orelseSuite = null;
                if (stack.nodeArity() == 5) {
                    orelseSuite = popSuiteAndSuiteType();
                }

                body = popSuite();
                iter = (exprType) stack.popNode();
                target = (exprType) stack.popNode();
                ctx.setStore(target);

                For forStmt = (For) n;
                forStmt.target = target;
                forStmt.iter = iter;
                forStmt.body = body;
                forStmt.orelse = orelseSuite;
                return forStmt;

            case JJTBEGIN_ELIF_STMT:
                return new If(null, null, null);

            case JJTIF_STMT:
                return handleIfConstruct(n, arity);

            case JJTEXEC_STMT:
                exprType locals = arity >= 3 ? ((exprType) stack.popNode()) : null;
                exprType globals = arity >= 2 ? ((exprType) stack.popNode()) : null;
                value = (exprType) stack.popNode();
                Exec exec = (Exec) n;
                exec.body = value;
                exec.locals = locals;
                exec.globals = globals;
                return exec;

            case JJTDECORATORS:
                ArrayList<SimpleNode> list2 = new ArrayList<SimpleNode>();
                ArrayList<SimpleNode> listArgs = new ArrayList<SimpleNode>();
                while (stack.nodeArity() > 0) {
                    SimpleNode node = stack.popNode();
                    while (!(node instanceof decoratorsType)) {
                        if (node instanceof comprehensionType) {
                            listArgs.add(node);
                            listArgs.add(stack.popNode()); //target
                        } else if (node instanceof ComprehensionCollection) {
                            listArgs.add(((ComprehensionCollection) node).getGenerators()[0]);
                            listArgs.add(stack.popNode()); //target

                        } else {
                            listArgs.add(node);
                        }
                        node = stack.popNode();
                    }
                    listArgs.add(node);//the decoratorsType
                    list2.add(0, makeDecorator(listArgs));
                    listArgs.clear();
                }
                return new Decorators(list2.toArray(new decoratorsType[0]), JJTDECORATORS);

            case JJTSUBSCRIPTLIST:
                sliceType[] dims = new sliceType[arity];
                for (int i = arity - 1; i >= 0; i--) {
                    SimpleNode sliceNode = stack.popNode();
                    if (sliceNode instanceof sliceType) {
                        dims[i] = (sliceType) sliceNode;

                    } else if (sliceNode instanceof IdentityNode) {
                        //this should be ignored...
                        //this happens when parsing something like a[1,], whereas a[1,2] would not have this.

                    } else {
                        throw new RuntimeException("Expected a sliceType or an IdentityNode. Received :"
                                + sliceNode.getClass());
                    }
                }
                return new ExtSlice(dims);

            case JJTAUG_PLUS:
            case JJTAUG_MINUS:
            case JJTAUG_MULTIPLY:
            case JJTAUG_DOT:
            case JJTAUG_DIVIDE:
            case JJTAUG_MODULO:
            case JJTAUG_AND:
            case JJTAUG_OR:
            case JJTAUG_XOR:
            case JJTAUG_LSHIFT:
            case JJTAUG_RSHIFT:
            case JJTAUG_POWER:
            case JJTAUG_FLOORDIVIDE:
                AugAssign augAssign = (AugAssign) n;
                exprType value1 = (exprType) stack.popNode();
                exprType target1 = (exprType) stack.popNode();
                ctx.setAugStore(target1);
                augAssign.target = target1;
                augAssign.value = value1;
                return n;

            case JJTOR_BOOLEAN:
                return new BoolOp(BoolOp.Or, makeExprs());
            case JJTAND_BOOLEAN:
                return new BoolOp(BoolOp.And, makeExprs());
            case JJTCOMPARISION:
                if (arity <= 2) {
                    throw new ParseException("Internal error: To make a compare, at least 3 nodes are needed.", n);
                }
                int l = arity / 2;
                exprType[] comparators = new exprType[l];
                int[] ops = new int[l];
                for (int i = l - 1; i >= 0; i--) {
                    comparators[i] = (exprType) stack.popNode();
                    SimpleNode op = stack.popNode();
                    switch (op.getId()) {
                        case JJTLESS_CMP:
                            ops[i] = Compare.Lt;
                            break;
                        case JJTGREATER_CMP:
                            ops[i] = Compare.Gt;
                            break;
                        case JJTEQUAL_CMP:
                            ops[i] = Compare.Eq;
                            break;
                        case JJTGREATER_EQUAL_CMP:
                            ops[i] = Compare.GtE;
                            break;
                        case JJTLESS_EQUAL_CMP:
                            ops[i] = Compare.LtE;
                            break;
                        case JJTNOTEQUAL_CMP:
                            ops[i] = Compare.NotEq;
                            break;
                        case JJTIN_CMP:
                            ops[i] = Compare.In;
                            break;
                        case JJTNOT_IN_CMP:
                            ops[i] = Compare.NotIn;
                            break;
                        case JJTIS_NOT_CMP:
                            ops[i] = Compare.IsNot;
                            break;
                        case JJTIS_CMP:
                            ops[i] = Compare.Is;
                            break;
                        default:
                            throw new RuntimeException("Unknown cmp op:" + op.getId());
                    }
                }
                return new Compare(((exprType) stack.popNode()), ops, comparators);
            case JJTLESS_CMP:
            case JJTGREATER_CMP:
            case JJTEQUAL_CMP:
            case JJTGREATER_EQUAL_CMP:
            case JJTLESS_EQUAL_CMP:
            case JJTNOTEQUAL_CMP:
            case JJTIN_CMP:
            case JJTNOT_IN_CMP:
            case JJTIS_NOT_CMP:
            case JJTIS_CMP:
                return n;

            case JJTOR_2OP:
            case JJTXOR_2OP:
            case JJTAND_2OP:
            case JJTLSHIFT_2OP:
            case JJTRSHIFT_2OP:
            case JJTADD_2OP:
            case JJTSUB_2OP:
            case JJTMUL_2OP:
            case JJTDOT_2OP:
            case JJTDIV_2OP:
            case JJTMOD_2OP:
            case JJTPOW_2OP:
            case JJTFLOORDIV_2OP:
                BinOp op = (BinOp) n;
                exprType right = (exprType) stack.popNode();
                exprType left = (exprType) stack.popNode();
                op.right = right;
                op.left = left;
                return n;

            case JJTPOS_1OP:
            case JJTNEG_1OP:
            case JJTINVERT_1OP:
            case JJTNOT_1OP:
                ((UnaryOp) n).operand = ((exprType) stack.popNode());
                return n;

            case JJTIMPORT:
                ((Import) n).names = makeAliases(arity);
                return n;

            case JJTDOT_OP:
                NameTok attr = makeName(NameTok.Attrib);
                value = (exprType) stack.popNode();
                Attribute attribute = (Attribute) n;
                attribute.value = value;
                attribute.attr = attr;
                return n;

            case JJTBEGIN_DEL_STMT:
                return new Delete(null);

            case JJTDEL_STMT:
                exprs = makeExprs(arity - 1);
                ctx.setDelete(exprs);
                Delete d = (Delete) stack.popNode();
                d.targets = exprs;
                return d;

            case JJTDOTTED_NAME:
                Name name = (Name) n;
                FastStringBuffer sb = tempBuffer.clear();
                for (int i = 0; i < arity; i++) {
                    if (i > 0) {
                        sb.insert(0, '.');
                    }
                    Name name0 = (Name) stack.popNode();
                    sb.insert(0, name0.id);
                    addSpecials(name0, name);
                    //we have to set that, because if we later add things to the previous Name, we will now want it to be added to
                    //the new name (comments will only appear later and may be added to the previous name -- so, we replace the previous
                    //name specials list).
                    name0.specialsBefore = name.getSpecialsBefore();
                    name0.specialsAfter = name.getSpecialsAfter();
                }
                name.id = sb.toString();
                return name;

            case JJTDOTTED_AS_NAME:
                NameTok asname = null;
                if (arity > 1) {
                    asname = makeName(NameTok.ImportName);
                }
                return new aliasType(makeName(NameTok.ImportName), asname);

            case JJTIMPORT_AS_NAME:
                asname = null;
                if (arity > 1) {
                    asname = makeName(NameTok.ImportName);
                }
                return new aliasType(makeName(NameTok.ImportName), asname);

            case JJTSTAR_EXPR:
                Starred s = (Starred) n;
                s.value = (exprType) this.stack.popNode();
                ctx.setStore(s);
                return s;

            case JJTSTRJOIN:
                Str str2 = (Str) stack.popNode();
                Object o = stack.popNode();
                StrJoin ret;
                if (o instanceof Str) {
                    Str str1 = (Str) o;
                    ret = new StrJoin(new exprType[] { str1, str2 });
                } else {
                    StrJoin strJ = (StrJoin) o;
                    exprType[] newStrs = new exprType[strJ.strs.length + 1];
                    System.arraycopy(strJ.strs, 0, newStrs, 0, strJ.strs.length);
                    newStrs[strJ.strs.length] = str2;
                    strJ.strs = newStrs;
                    ret = strJ;
                }
                ret.beginLine = ret.strs[0].beginLine;
                ret.beginColumn = ret.strs[0].beginColumn;
                return ret;

        }

        //if we found a node not expected in the base, let's give subclasses an opportunity for dealing with it.
        return onCloseNode(n, arity);
    }

    /**
     * Handles a found if construct.
     * 
     * @param n the node that opened the if scope.
     * @param arity the current number of nodes in the stack.
     * @return the If node that should close this context.
     */
    private final SimpleNode handleIfConstruct(final SimpleNode n, int arity) {
        stmtType[] body;
        exprType test;

        suiteType orelse = null;
        if (arity % 3 == 1) {
            arity -= 2;
            orelse = this.popSuiteAndSuiteType();
        }

        //make the suite
        Suite suite = (Suite) stack.popNode();
        arity--;
        body = suite.body;
        test = (exprType) stack.popNode();
        arity--;

        //make the if
        If last;
        if (arity == 0) {
            //last If found
            last = (If) n;
        } else {
            last = (If) stack.popNode();
            arity--;
        }
        last.test = test;
        last.body = body;
        last.orelse = orelse;
        addSpecialsAndClearOriginal(suite, last);

        while (arity > 0) {
            suite = (Suite) stack.popNode();
            arity--;

            body = suite.body;
            test = (exprType) stack.popNode();
            arity--;

            suiteType newOrElse = new Suite(new stmtType[] { last });
            if (arity == 0) {
                //last If found
                last = (If) n;
            } else {
                last = (If) stack.popNode();
                arity--;
            }
            last.test = test;
            last.body = body;
            last.orelse = newOrElse;
            addSpecialsAndClearOriginal(suite, last);
        }
        return last;
    }

    protected final SimpleNode makeImportFrom25Onwards(int arity) throws ParseException {
        ArrayList<aliasType> aliastL = new ArrayList<aliasType>();
        while (arity > 0 && stack.peekNode() instanceof aliasType) {
            aliastL.add(0, (aliasType) stack.popNode());
            arity--;
        }
        NameTok nT;
        if (arity > 0) {
            nT = makeName(NameTok.ImportModule);
        } else {
            nT = new NameTok("", NameTok.ImportModule);
            Object temporaryTok = this.stack.getGrammar().temporaryToken;
            ISpecialStr temporaryToken;
            if (temporaryTok instanceof ISpecialStr) {
                temporaryToken = (ISpecialStr) temporaryTok;
            } else {
                //must be a Token
                temporaryToken = ((Token) temporaryTok).asSpecialStr();
            }
            if (temporaryToken.toString().equals("from")) {
                nT.beginColumn = temporaryToken.getBeginCol();
                nT.beginLine = temporaryToken.getBeginLine();
            } else {
                Log.log("Expected to find 'from' token as the current temporary token (begin col/line can be wrong)!");
            }
        }
        return new ImportFrom(nT, aliastL.toArray(new aliasType[0]), 0);
    }

    protected final ComprehensionCollection makeCompFor(int arity) throws Exception {
        ComprehensionCollection col = null;
        if (stack.peekNode() instanceof ComprehensionCollection) {
            col = (ComprehensionCollection) stack.popNode();
            arity--;
        } else {
            col = new ComprehensionCollection();
        }

        ArrayList<exprType> ifs = new ArrayList<exprType>();
        for (int i = arity - 3; i >= 0; i--) {
            SimpleNode ifsNode = stack.popNode();
            ifs.add((exprType) ifsNode);
        }
        exprType iter = (exprType) stack.popNode();
        exprType target = (exprType) stack.popNode();
        ctx.setStore(target);
        col.added.add(new Comprehension(target, iter, ifs.toArray(new exprType[0])));
        return col;
    }

    protected final SimpleNode makeDictionaryOrSet(int arity) {
        if (arity == 0) {
            return new Dict(new exprType[0], new exprType[0]);
        }

        SimpleNode dictNode0 = stack.popNode();

        if (dictNode0 instanceof Set) {
            Set set = (Set) dictNode0;
            exprType[] elts = new exprType[arity - 1]; //-1 because the set was already taken from there
            for (int i = arity - 2; i >= 0; i--) { //same thing here
                elts[i] = (exprType) stack.popNode();
            }
            set.elts = elts;
            return set;
        }

        if (dictNode0 instanceof ComprehensionCollection) {
            if (arity == 2) {
                ComprehensionCollection comp = (ComprehensionCollection) dictNode0;
                return new SetComp((exprType) stack.popNode(), comp.getGenerators());

            } else if (arity == 3) {
                SimpleNode dictNode1 = stack.popNode(); //we must inverse things here...
                ComprehensionCollection comp = (ComprehensionCollection) dictNode0;
                return new DictComp((exprType) stack.popNode(), (exprType) dictNode1, comp.getGenerators());
            }
        }

        boolean isDictComplete = arity % 2 == 0;

        int l = arity / 2;
        exprType[] keys;
        if (isDictComplete) {
            keys = new exprType[l];
        } else {
            keys = new exprType[l + 1]; //we have 1 additional entry in the keys (parse error actually, but let's recover at this point!)
        }
        boolean node0Used = false;
        exprType[] vals = new exprType[l];
        for (int i = l - 1; i >= 0; i--) {
            if (!node0Used) {
                node0Used = true;
                vals[i] = (exprType) dictNode0;
                keys[i] = (exprType) stack.popNode();

            } else {
                vals[i] = (exprType) stack.popNode();
                keys[i] = (exprType) stack.popNode();
            }
        }
        if (!isDictComplete) {
            if (node0Used) {
                keys[keys.length - 1] = (exprType) stack.popNode();
            } else {
                keys[keys.length - 1] = (exprType) dictNode0;
            }
        }

        return new Dict(keys, vals);
    }

    protected final SimpleNode makeWithItem(int arity) throws Exception {
        exprType expr = (exprType) stack.popNode(); //expr
        arity--;

        exprType asExpr = null;
        if (arity > 0) {
            asExpr = expr;
            expr = (exprType) stack.popNode();
            ctx.setStore(asExpr);
        }
        return new WithItem(expr, asExpr);
    }

    protected final SimpleNode makeWithStmt(int arity) {
        Suite suite = (Suite) stack.popNode();
        arity--;

        WithItem[] items = new WithItem[arity];
        while (arity > 0) {
            items[arity - 1] = (WithItem) stack.popNode();
            arity--;
        }

        suiteType s = new Suite(suite.body);
        addSpecialsAndClearOriginal(suite, s);

        return new With(items, s);
    }

}
