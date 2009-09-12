/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.visitors.FindLastLineVisitor;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;

public class BeginOffset extends AbstractOffsetStrategy {

    public BeginOffset(IASTNodeAdapter<? extends SimpleNode> adapter, IDocument doc) {
        super(adapter, doc);
    }

    protected int getLine() {
        SimpleNode node = adapter.getASTNode();
        if(nodeHelper.isClassDef(node)){
            ClassDef classNode = (ClassDef) node;
            Str strNode = NodeUtils.getNodeDocStringNode(node);

            if(strNode != null){
                return NodeUtils.getLineEnd(strNode) - 1;
            }

            FindLastLineVisitor findLastLineVisitor = new FindLastLineVisitor();
            try{
                classNode.name.accept(findLastLineVisitor);
                if(classNode.bases != null){
                    for(SimpleNode n:classNode.bases){
                        n.accept(findLastLineVisitor);
                    }
                }
                SimpleNode lastNode = findLastLineVisitor.getLastNode();
                SpecialStr lastSpecialStr = findLastLineVisitor.getLastSpecialStr();
                if(lastSpecialStr != null && (lastSpecialStr.str.equals(":") || lastSpecialStr.str.equals(")"))){
                    // it was an from xxx import (euheon, utehon)
                    return lastSpecialStr.beginLine - 1;
                }else{
                    return lastNode.beginLine - 1;
                }
            }catch(Exception e){
                Log.log(e);
            }

        }

        int startLine = adapter.getNodeFirstLine() - 1;
        if(startLine < 0){
            startLine = 0;
        }
        return startLine;
    }

    @Override
    protected int getLineIndendation() throws BadLocationException {
        if(adapter.getNodeBodyIndent() == 0){
            return 0;
        }else{
            return doc.getLineLength(getLine());
        }
    }
}
