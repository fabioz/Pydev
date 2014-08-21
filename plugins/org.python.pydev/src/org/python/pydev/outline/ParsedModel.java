/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.outline;

import java.util.ArrayList;

import org.eclipse.jface.viewers.StructuredSelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;
import org.python.pydev.parser.visitors.scope.OutlineCreatorVisitor;
import org.python.pydev.shared_core.editor.IBaseEditor;
import org.python.pydev.shared_core.model.ErrorDescription;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_ui.outline.BaseModel;
import org.python.pydev.shared_ui.outline.IParsedItem;

/**
 * ParsedModel represents a python file, parsed for OutlineView display
 * It takes PyParser, and converts it into a tree of ParsedItems
 */
public class ParsedModel extends BaseModel {

    /**
     * @param outline - If not null, view to notify when parser changes
     */
    public ParsedModel(IBaseEditor editor) {
        super(editor);
    }

    @Override
    protected IParsedItem createInitialRootFromEditor() {
        ISimpleNode ast = ((PyEdit) editor).getAST();
        return createParsedItemFromSimpleNode(ast);
    }

    @Override
    protected IParsedItem createParsedItemFromSimpleNode(ISimpleNode ast) {
        OutlineCreatorVisitor visitor = OutlineCreatorVisitor.create((SimpleNode) ast);
        return new ParsedItem(visitor.getAll().toArray(new ASTEntryWithChildren[0]),
                ((PyEdit) ParsedModel.this.editor).getErrorDescription());
    }

    @Override
    protected IParsedItem duplicateRootAddingError(ErrorDescription errorDesc) {
        IParsedItem currRoot = getRoot();

        IParsedItem newRoot;
        if (currRoot != null) {
            newRoot = new ParsedItem(((ParsedItem) currRoot).getAstChildrenEntries(), errorDesc);
            newRoot.updateTo(currRoot);
        } else {
            newRoot = new ParsedItem(new ASTEntryWithChildren[0], errorDesc);
        }
        return newRoot;
    }

    public SimpleNode[] getSelectionPosition(StructuredSelection sel) {
        if (sel.size() == 1) { // only sync the editing view if it is a single-selection
            Object firstElement = sel.getFirstElement();
            ASTEntryWithChildren p = ((ParsedItem) firstElement).getAstThis();
            if (p == null) {
                return null;
            }
            SimpleNode node = p.node;
            if (node instanceof ClassDef) {
                ClassDef def = (ClassDef) node;
                node = def.name;

            } else if (node instanceof Attribute) {
                Attribute attribute = (Attribute) node;
                node = attribute.attr;

            } else if (node instanceof FunctionDef) {
                FunctionDef def = (FunctionDef) node;
                node = def.name;

            } else if (node instanceof Import) {
                ArrayList<SimpleNode> ret = new ArrayList<SimpleNode>();
                Import importToken = (Import) node;
                for (int i = 0; i < importToken.names.length; i++) {
                    aliasType aliasType = importToken.names[i];

                    //as ...
                    if (aliasType.asname != null) {
                        ret.add(aliasType.asname);
                    }

                    ret.add(aliasType.name);
                }
                return ret.toArray(new SimpleNode[0]);

            } else if (node instanceof ImportFrom) {
                ArrayList<SimpleNode> ret = new ArrayList<SimpleNode>();
                ImportFrom importToken = (ImportFrom) node;
                boolean found = false;
                for (int i = 0; i < importToken.names.length; i++) {
                    found = true;
                    aliasType aliasType = importToken.names[i];

                    //as ...
                    if (aliasType.asname != null) {
                        ret.add(aliasType.asname);
                    }

                    ret.add(aliasType.name);
                }
                if (!found) {
                    ret.add(importToken.module);
                }
                return ret.toArray(new SimpleNode[0]);
            }
            return new SimpleNode[] { node };
        }
        return null;
    }

}