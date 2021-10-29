package org.python.pydev.ast.codecompletion.revisited.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;

public class ClassTokensExtractor {
    private ClassDef classDef;
    private String classRep;
    private SourceModule module;
    private ICompletionState initialState;

    public ClassTokensExtractor(ClassDef classDef, SourceModule module, ICompletionState initialState) {
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
                this.module.getName(), initialState, false, this.module.getNature());

        TokensList classTokens = new TokensList(globalModelVisitorTokens);
        try {
            for (int j = 0; j < classDef.bases.length; j++) {
                TokensList completions = getCompletionsForBase(manager, j);
                classTokens.addAll(completions);
            }
            TokensList assignmentClassTokens = getAssignmentTokens();
            classTokens.addAll(assignmentClassTokens);
        } catch (CompletionRecursionException e) {
            // let's return what we have so far...
        }
        classTokens.setLookingFor(initialState.getLookingFor());
        return classTokens;
    }

    public TokensList getCompletionsForBase(ICodeCompletionASTManager manager, int baseIndex)
            throws CompletionRecursionException {
        ICompletionState state;
        if (classDef.bases[baseIndex] instanceof Name) {
            Name n = (Name) classDef.bases[baseIndex];
            String base = n.id;
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
        } else if (classDef.bases[baseIndex] instanceof Attribute) {
            Attribute attr = (Attribute) classDef.bases[baseIndex];
            String s = NodeUtils.getFullRepresentationString(attr);

            state = initialState.getCopy();
            state.setActivationToken(s);
            return manager.getCompletionsForModule(module, state);
        } else if (classDef.bases[baseIndex] instanceof Subscript) {
            TokensList tokens = new TokensList();
            Subscript subscript = (Subscript) classDef.bases[baseIndex];
            String subscriptValue = NodeUtils.getFullRepresentationString(subscript.value);
            tokens.addAll(getCompletionsForValue(manager, initialState, subscriptValue));
            if (subscript.slice instanceof Index) {
                Index index = (Index) subscript.slice;
                String subscriptSlice = NodeUtils.getFullRepresentationString(index.value);
                tokens.addAll(getCompletionsForValue(manager, initialState, subscriptSlice));
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
            TokensList tokens = manager.getCompletionsForModule(this.module, copiedState);
            if (tokens != null) {
                return tokens;
            }
        }
        return new TokensList();
    }

    private TokensList getAssignmentTokens() {
        if (classRep == null) {
            return new TokensList();
        }

        stmtType[] body = NodeUtils.getBody(classDef.parent);
        if (body == null || body.length == 0) {
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
                        SourceToken token = new SourceToken(node, nodeRep, null, null, this.module.getName(),
                                this.module.getNature());

                        Assign createdAssign = createAssignForToken(node, assign);
                        token.setFoundInAssign(createdAssign);

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
            if (classRep.equals(firstRep)) {
                return Optional.of(attributeParts.get(1));
            }
        }
        return Optional.empty();
    }

    private Assign createAssignForToken(SimpleNode node, Assign originalAssign) {
        String nodeRep = NodeUtils.getFullRepresentationString(node);

        Name name = new Name(nodeRep, Name.Store, false);
        name.beginLine = node.beginLine;
        name.beginColumn = node.beginColumn;

        return new Assign(new exprType[] { name }, originalAssign.value, originalAssign.type);
    }
}