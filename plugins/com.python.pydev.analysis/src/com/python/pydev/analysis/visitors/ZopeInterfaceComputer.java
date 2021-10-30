package com.python.pydev.analysis.visitors;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.python.pydev.ast.codecompletion.revisited.CompletionState;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.structure.Tuple3;

public class ZopeInterfaceComputer {

    public final ClassDef classDef;
    private final IModule module;
    private final ICompletionCache completionCache;

    private Boolean isZopeInterface;

    public ZopeInterfaceComputer(ClassDef classDef, IModule module,
            ICompletionCache completionCache) {
        this.classDef = classDef;
        this.module = module;
        this.isZopeInterface = null;
        this.completionCache = completionCache;
    }

    private static class NodeAndModule {

        public final SimpleNode node;
        public final IModule module;
        public final int beginLine;
        public final int beginColumn;

        public NodeAndModule(SimpleNode n, IModule module) {
            this.node = n;
            this.module = module;
            this.beginLine = n.beginLine;
            this.beginColumn = n.beginColumn;
        }

        public String getRep() {
            return NodeUtils.getFullRepresentationString(node);
        }

    }

    public boolean isZopeInterface() {
        if (isZopeInterface != null) {
            return isZopeInterface;
        }

        Set<Tuple3<String, Integer, Integer>> whereWePassed = new HashSet<Tuple3<String, Integer, Integer>>();
        LinkedList<NodeAndModule> verify = new LinkedList<>();
        for (SimpleNode n : classDef.bases) {
            verify.add(new NodeAndModule(n, module));
        }

        while (!verify.isEmpty()) {
            NodeAndModule nodeAndModule = verify.removeFirst();
            IDefinition[] definitions = findDefinitionsForNodeAndModule(nodeAndModule);
            for (IDefinition iDefinition : definitions) {
                Definition definition = (Definition) iDefinition;
                if (isDefinitionZopeInterface(definition)) {
                    isZopeInterface = true;
                    return isZopeInterface;
                }

                Tuple3<String, Integer, Integer> tup = PyRefactoringFindDefinition.getTupFromDefinition(definition);
                if (whereWePassed.contains(tup)) {
                    continue;
                }
                whereWePassed.add(tup);

                // It's not a zope definition, still, we can keep on following definitions
                // to know if it's still possible that this will be the case...
                if (definition.ast instanceof ClassDef && definition.module != null) {
                    ClassDef classDef2 = (ClassDef) definition.ast;
                    for (exprType base : classDef2.bases) {
                        verify.add(new NodeAndModule(base, definition.module));
                    }
                }
            }
        }
        isZopeInterface = false;
        return isZopeInterface;
    }

    private IDefinition[] findDefinitionsForNodeAndModule(NodeAndModule nodeAndModule) {
        ICompletionState state = new CompletionState(nodeAndModule.beginLine, nodeAndModule.beginColumn,
                nodeAndModule.getRep(), nodeAndModule.module.getNature(), "", completionCache);
        try {
            return nodeAndModule.module.findDefinition(state, state.getLine() + 1, state.getCol() + 1,
                    nodeAndModule.module.getNature());
        } catch (Exception e) {
            Log.log(e);
        }
        return new IDefinition[0];
    }

    private boolean isDefinitionZopeInterface(Definition definition) {
        if (definition.module != null) {
            String moduleName = definition.module.getName();
            if ("zope.interface.__init__".equals(moduleName) && "Interface".equals(definition.value)) {
                return true;
            }
            if ("zope.interface.interface".equals(moduleName)
                    && "Interface".equals(definition.value)) {
                return true;
            }
        }
        return false;
    }

}
