package org.python.pydev.ast.codecompletion.revisited.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.python.pydev.ast.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.ast.codecompletion.revisited.visitors.GlobalModelVisitor;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;

public final class ClassDefTokensExtractor {
    private final ClassDef classDef;
    private final String classRep;
    private final SourceModule module;
    private final ICompletionState initialState;

    public ClassDefTokensExtractor(ClassDef classDef, SourceModule module, ICompletionState initialState) {
        this.classDef = classDef;
        this.classRep = NodeUtils.getFullRepresentationString(classDef);
        this.module = module;
        this.initialState = initialState;
    }

    /**
     * @param manager
     * @return TokensList
     */
    public TokensList getTokens(ICodeCompletionASTManager manager) {
        List<IToken> globalModelVisitorTokens = GlobalModelVisitor.getTokens(classDef, GlobalModelVisitor.INNER_DEFS,
                this.module.getName(), initialState, false, this.module.getNature(), this.module);
        TokensList classTokens = new TokensList(globalModelVisitorTokens);
        try {

            initialState.pushSkipObjectBaseCompletions();
            try {
                for (int j = 0; j < classDef.bases.length; j++) {
                    classTokens.addAll(getCompletionsForBase(manager, classDef.bases[j]));
                }
            } finally {
                initialState.popSkipObjectBaseCompletions();
            }

            // i.e.: Always derived from object by default...
            classTokens.addAll(getCompletionsForBase(manager, new Name("object", NameTok.ClassName, false)));

            classTokens.addAll(getAssignmentTokens());
        } catch (CompletionRecursionException e) {
            // let's return what we have so far...
        }
        classTokens.setLookingFor(initialState.getLookingFor());
        return classTokens;
    }

    public TokensList getCompletionsForBase(ICodeCompletionASTManager manager, SimpleNode baseNode)
            throws CompletionRecursionException {
        ICompletionState state;
        if (baseNode instanceof Name) {
            Name n = (Name) baseNode;
            String base = n.id;
            if (classRep != null && classRep.equals(base)) {
                return null;
            }
            if (initialState.getSkipObjectBaseCompletions()) {
                if ("object".equals(base)) {
                    return null;
                }
            }
            //An error in the programming might result in an error.
            //
            //e.g. The case below results in a loop.
            //
            //class A(B):
            //
            //    def a(self):
            //        pass
            //
            //class B(A):
            //
            //    def b(self):
            //        pass
            state = initialState.getCopy();
            state.setActivationToken(base);

            state.checkMemory(module, base);

            return manager.getCompletionsForModule(module, state);
        } else if (baseNode instanceof Attribute) {
            Attribute attr = (Attribute) baseNode;
            String s = NodeUtils.getFullRepresentationString(attr);
            if (classRep != null && classRep.equals(s)) {
                return null;
            }

            state = initialState.getCopy();
            state.setActivationToken(s);
            return manager.getCompletionsForModule(module, state);
        } else if (baseNode instanceof Subscript) {
            TokensList tokens = new TokensList();
            Subscript subscript = (Subscript) baseNode;
            String subscriptValue = NodeUtils.getFullRepresentationString(subscript.value);
            if (classRep == null || !classRep.equals(subscriptValue)) {
                tokens.addAll(getCompletionsForValue(manager, initialState, subscriptValue));
            }
            if (subscript.slice instanceof Index) {
                Index index = (Index) subscript.slice;
                String subscriptSlice = NodeUtils.getFullRepresentationString(index.value);
                if (classRep == null || !classRep.equals(subscriptSlice)) {
                    tokens.addAll(getCompletionsForValue(manager, initialState, subscriptSlice));
                }
            }
            if (tokens.size() > 0) {
                return tokens;
            }
        }
        return null;
    }

    private TokensList getCompletionsForValue(ICodeCompletionASTManager manager, ICompletionState state,
            String value) throws CompletionRecursionException {
        if (value != null && !value.isEmpty()) {
            ICompletionState copiedState = state.getCopy();
            state.checkMemory(this.module, value);
            copiedState.setActivationToken(value);
            return manager.getCompletionsForModule(this.module, copiedState);
        }
        return null;
    }

    private TokensList getAssignmentTokens() {
        if (classRep == null) {
            return null;
        }

        stmtType[] body;
        if (classDef.parent != null) {
            body = NodeUtils.getBody(classDef.parent);
        } else {
            Module moduleNode = (Module) this.module.getAst();
            body = moduleNode.body;
        }

        return extractTokensFromBody(body);
    }

    private TokensList extractTokensFromBody(stmtType[] body) {
        TokensList ret = new TokensList();
        List<IToken> tokensList = new ArrayList<IToken>();
        for (stmtType node : body) {
            if (node instanceof Assign) {
                Assign assign = (Assign) node;
                Optional<IToken> optionalToken = extractTokenFromAssign(assign);
                if (optionalToken.isPresent()) {
                    IToken token = optionalToken.get();
                    tokensList.add(token);
                }
            }
        }
        ret.addAll(new TokensList(tokensList));
        return ret;
    }

    private Optional<IToken> extractTokenFromAssign(Assign assign) {
        for (exprType target : assign.targets) {
            if (target instanceof Attribute) {
                Attribute attribute = (Attribute) target;
                Optional<SimpleNode> optionalNode = extractNodeFromAttribute(attribute);
                if (optionalNode.isPresent()) {
                    SimpleNode node = optionalNode.get();
                    String nodeRep = NodeUtils.getFullRepresentationString(node);
                    if (nodeRep != null) {

                        SourceToken token = AbstractVisitor.makeToken(node, nodeRep, this.module.getName(),
                                this.module.getNature(), this.module);

                        return Optional.of(token);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<SimpleNode> extractNodeFromAttribute(Attribute attribute) {
        List<SimpleNode> attributeParts = NodeUtils.getAttributeParts(attribute);
        if (attributeParts.size() > 1) {
            SimpleNode first = attributeParts.get(0);
            String firstRep = NodeUtils.getFullRepresentationString(first);
            if (classRep != null && classRep.equals(firstRep)) {
                return Optional.of(attributeParts.get(1));
            }
        }
        return Optional.empty();
    }

}