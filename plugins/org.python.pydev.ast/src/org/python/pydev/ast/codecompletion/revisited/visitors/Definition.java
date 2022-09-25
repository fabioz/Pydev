/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.revisited.visitors;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.ast.codecompletion.revisited.CompletionState;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.TokensList;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.FullRepIterable;

/**
 * @author Fabio Zadrozny
 */
public class Definition implements IDefinition {

    /**
     * Line of the definition. Starts at 1
     */
    public final int line;

    /**
     * Column of the definition. Starts at 1
     */
    public final int col;

    /**
     * Name of the token.
     *
     * e.g.
     * tok = ClassA()
     * or
     * tok:type = ClassA()
     *
     * the value equals ClassA
     */
    public final String value;

    /**
     * Type of the token.
     *
     * e.g.
     * tok: type
     * or
     * tok: type = ClassA()
     *
     * the type equals type
     */
    public final String type;

    public final exprType nodeType;

    /**
     * This is the module where the definition is.
     */
    public final IModule module;

    /**
     * Assign ast.
     */
    public final SimpleNode ast;

    /**
     * Node with the path of classes / funcs to get to an assign.
     */
    public ILocalScope scope;

    /**
     * Determines whether this definition was found as a local.
     */
    public final boolean foundAsLocal;

    private ITypeInfo generatorType;

    /**
     * The line and col are defined starting at 1 (and not 0)
     */
    public Definition(int line, int col, String value, SimpleNode ast, ILocalScope scope, IModule module) {
        this(line, col, value, ast, scope, module, false);
    }

    /**
     * The line and col are defined starting at 1
     */
    public Definition(int line, int col, String value, String type, SimpleNode ast, ILocalScope scope, IModule module) {
        this(line, col, value, type, ast, scope, module, false);
    }

    /**
     * The line and col are defined starting at 1
     */
    public Definition(int line, int col, String value, String type, exprType nodeType, SimpleNode ast,
            ILocalScope scope, IModule module) {
        this(line, col, value, type, nodeType, ast, scope, module, false);
    }

    /**
     * The ast and scope may be null if the definition points to the module (and not some token defined
     * within it).
     *
     * The line and col are defined starting at 1 (and not 0)
     */
    public Definition(int line, int col, String value, SimpleNode ast, ILocalScope scope, IModule module,
            boolean foundAsLocal) {
        Assert.isNotNull(value, "Invalid value.");
        Assert.isNotNull(module, "Invalid Module.");

        if (scope == null) {
            scope = new LocalScope(module.getNature(), module);
        }

        this.line = line;
        this.col = col;
        this.value = value;
        this.type = "";
        if (ast instanceof Assign) {
            this.nodeType = ((Assign) ast).type;
        } else {
            this.nodeType = null;
        }
        this.ast = ast;
        this.scope = scope;
        this.module = module;
        this.foundAsLocal = foundAsLocal;
    }

    public Definition(int line, int col, String value, String type, SimpleNode ast, ILocalScope scope, IModule module,
            boolean foundAsLocal) {
        Assert.isNotNull(value, "Invalid value.");
        Assert.isNotNull(module, "Invalid Module.");

        if (scope == null) {
            scope = new LocalScope(module.getNature(), module);
        }

        this.line = line;
        this.col = col;
        this.value = value;
        this.type = type;
        if (ast instanceof Assign) {
            this.nodeType = ((Assign) ast).type;
        } else {
            this.nodeType = null;
        }
        this.ast = ast;
        this.scope = scope;
        this.module = module;
        this.foundAsLocal = foundAsLocal;
    }

    public Definition(int line, int col, String value, String type, exprType nodeType, SimpleNode ast,
            ILocalScope scope, IModule module,
            boolean foundAsLocal) {
        Assert.isNotNull(value, "Invalid value.");
        Assert.isNotNull(module, "Invalid Module.");

        if (scope == null) {
            scope = new LocalScope(module.getNature(), module);
        }

        this.line = line;
        this.col = col;
        this.value = value;
        this.type = type;
        this.nodeType = nodeType;
        this.ast = ast;
        this.scope = scope;
        this.module = module;
        this.foundAsLocal = foundAsLocal;
    }

    public Definition(org.python.pydev.core.IToken tok, ILocalScope scope, IModule module) {
        this(tok, scope, module, false);
    }

    public Definition(org.python.pydev.core.IToken tok, ILocalScope scope, IModule module, boolean foundAsLocal) {
        Assert.isNotNull(tok, "Invalid value.");
        Assert.isNotNull(module, "Invalid Module.");

        this.line = tok.getLineDefinition();
        this.col = tok.getColDefinition();
        this.value = tok.getRepresentation();
        this.type = "";
        if (tok instanceof SourceToken) {
            this.ast = ((SourceToken) tok).getAst();
        } else {
            this.ast = null;
        }

        if (ast instanceof Assign) {
            this.nodeType = ((Assign) ast).type;
        } else {
            this.nodeType = null;
        }
        if (scope == null) {
            scope = new LocalScope(module.getNature(), module);
        }

        this.scope = scope;
        this.module = module;
        this.foundAsLocal = foundAsLocal;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer("Definition=", 30 + value.length());
        buffer.append(value);
        buffer.append(" line=");
        buffer.append(line);
        buffer.append(" col=");
        buffer.append(col);
        buffer.append(" module=");
        if (module != null) {
            buffer.appendObject(module.getName());
        } else {
            buffer.append("null");
        }
        return buffer.toString();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Definition)) {
            return false;
        }

        Definition d = (Definition) obj;

        if (!value.equals(d.value)) {
            return false;
        }

        if (col != d.col) {
            return false;
        }

        if (line != d.line) {
            return false;
        }

        if (scope == d.scope) {
            return true;
        }
        if (scope == null || d.scope == null) {
            return false;
        }

        if (!scope.equals(d.scope)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return value.hashCode() + col + line;
    }

    @Override
    public IModule getModule() {
        return module;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getCol() {
        return col;
    }

    @Override
    public String getDocstring(IPythonNature nature, ICompletionCache cache) {
        if (this.ast != null) {
            return NodeUtils.getNodeDocString(this.ast);
        } else {
            if (this.value == null || this.value.trim().length() == 0) {
                return this.module.getDocString();
            } else if (nature != null) {
                ICodeCompletionASTManager manager = nature.getAstManager();
                //It's the identification for some token in a module, let's try to find it
                String[] headAndTail = FullRepIterable.headAndTail(value);
                String actToken = headAndTail[0];
                String qualifier = headAndTail[1];

                TokensList globalTokens = this.module.getGlobalTokens(new CompletionState(line, col, actToken, nature,
                        qualifier, cache), manager);

                for (IterTokenEntry entry : globalTokens) {
                    IToken iToken = entry.getToken();
                    String rep = iToken.getRepresentation();
                    //if the value is file.readlines, when a compiled module is asked, it'll return
                    //the module __builtin__ with a parent package of __builtin__.file and a representation
                    //of readlines, so, the qualifier matches the representation (and not the full value).
                    //Note that if we didn't have a dot, we wouldn't really need to check that.
                    if (this.value.equals(rep) || qualifier.equals(rep)) {
                        return iToken.getDocStr();
                    }
                }
            }
        }

        return null;
    }

    public void setGeneratorType(ITypeInfo generatorType) {
        this.generatorType = generatorType;
    }

    public ITypeInfo getGeneratorType() {
        return generatorType;
    }
}
