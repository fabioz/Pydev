/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/06/2005
 */
package org.python.pydev.parser.visitors.scope;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.structure.DecoratableObject;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * This class defines an AST entry.
 * 
 * It's creator may not fill all the needed info (because, getting that info
 * will usually get more processing), so, be careful when accessing this
 * info to make sure that the creator of this info actually provides it.
 * 
 * @author Fabio
 */
public class ASTEntry extends DecoratableObject {
    /**
     * This is the parent of this entry. It may be null
     */
    public ASTEntry parent;

    /**
     * This is the node that represents this entry
     */
    public SimpleNode node;

    /**
     * This is the line where the entry finishes (most iterators will give you that)
     */
    public int endLine;

    /**
     * This is the name that this entry represents
     */
    private String name;

    /**
     * This is the column where the entry finishes (most iterators will NOT give you that)
     */
    public int endCol;

    public ASTEntry(ASTEntry parent, SimpleNode node) {
        this(parent);
        this.node = node;
        if (node != null) {
            this.endLine = node.beginLine;
        }
    }

    public ASTEntry(ASTEntry parent) {
        this.parent = parent;
    }

    public String getName() {
        if (name != null) {
            return name;
        }

        if (node instanceof ClassDef) {
            name = NodeUtils.getNameFromNameTok((NameTok) ((ClassDef) node).name);

        } else if (node instanceof FunctionDef) {
            name = NodeUtils.getNameFromNameTok((NameTok) ((FunctionDef) node).name);

        } else if (node instanceof Import) {
            aliasType[] names = ((Import) node).names;
            StringBuffer buffer = new StringBuffer("import ");

            for (int i = 0; i < names.length; i++) {
                buffer.append(((NameTok) names[i].name).id);
                if (names[i].asname != null) {
                    buffer.append(" as ");
                    buffer.append(((NameTok) names[i].asname).id);
                }
            }
            name = buffer.toString();

        } else if (node instanceof ImportFrom) {
            aliasType[] names = ((ImportFrom) node).names;
            StringBuffer buffer = new StringBuffer("from ");
            buffer.append(((NameTok) ((ImportFrom) node).module).id);
            buffer.append(" import ");
            if (names.length > 0) {
                for (int i = 0; i < names.length; i++) {
                    buffer.append(((NameTok) names[i].name).id);
                    if (names[i].asname != null) {
                        buffer.append(" as ");
                        buffer.append(((NameTok) names[i].asname).id);
                    }
                }
            } else {
                buffer.append("*");
            }
            name = buffer.toString();

        } else if (node instanceof Attribute) {
            Attribute a = (Attribute) node;
            name = ((NameTok) a.attr).id;

        } else if (node instanceof Name) {
            Name a = (Name) node;
            name = a.id;

        } else if (node instanceof NameTok) {
            NameTok a = (NameTok) node;
            name = a.id;

        } else if (node instanceof Module) {
            name = "Module";

        } else if (node instanceof Str) {
            name = "Str";

        } else if (node instanceof While) {
            name = "While";

        } else if (node instanceof If) {
            name = "If";

        } else if (node instanceof For) {
            name = "For";

        } else if (node instanceof TryExcept) {
            name = "TryExcept";

        } else if (node instanceof TryFinally) {
            name = "TryFinally";

        } else if (node instanceof With) {
            name = "With";

        } else if (node instanceof commentType) {
            name = "comment";
        }

        if (name == null) {
            throw new RuntimeException("Unable to get node name: " + node);
        } else {
            return name;
        }
    }

    public SimpleNode getNameNode() {
        if (node instanceof ClassDef) {
            return ((ClassDef) node).name;

        } else if (node instanceof FunctionDef) {
            return ((FunctionDef) node).name;

        } else {
            return node;

        }
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.getClass().getSimpleName());
        buffer.append("<");
        buffer.append(getName());
        buffer.append(" (");
        buffer.append(FullRepIterable.getLastPart(node.getClass().getName()));
        buffer.append(" L=");
        buffer.append(node.beginLine);
        buffer.append(" C=");
        buffer.append(node.beginColumn);
        buffer.append(")");
        buffer.append(">");
        return buffer.toString();
    }

    @Override
    public int hashCode() {
        int i = 31;
        String n = getName();
        if (n != null) {
            i *= n.hashCode();
        }
        i += node.beginLine;
        i *= node.beginColumn;
        return i;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ASTEntry)) {
            return false;
        }
        ASTEntry other = (ASTEntry) obj;

        if (node.beginColumn != other.node.beginColumn || node.beginLine != other.node.beginLine
                || endCol != other.endCol || endLine != other.endLine) {
            return false;
        }

        //compare names (cannot be null)
        String n = getName();
        String oN = other.getName();
        if (!n.equals(oN)) {
            return false;
        }
        return true;
    }

}
