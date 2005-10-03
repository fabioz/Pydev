/*
 * Created on 12/06/2005
 */
package org.python.pydev.parser.visitors.scope;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.aliasType;
import org.python.pydev.parser.visitors.NodeUtils;


/**
 * @author Fabio
 */
public class ASTEntry{
    public ASTEntry parent;
    public SimpleNode node;
    public int endLine;
    private String name;
    
    public ASTEntry(ASTEntry parent){
        this.parent = parent;
    }
    
    public String getName(){
        if(name != null){
            return name;
        }
        
        if (node instanceof ClassDef){
            name = NodeUtils.getNameFromNameTok((NameTok) ((ClassDef)node).name);
            
        } else if (node instanceof FunctionDef){
            name = NodeUtils.getNameFromNameTok((NameTok) ((FunctionDef)node).name);
            
        }else if (node instanceof Import){
            aliasType[] names = ((Import)node).names;
            StringBuffer buffer = new StringBuffer("import ");
            
            for (int i = 0; i < names.length; i++) {
                buffer.append(((NameTok)names[i].name).id);
                if(names[i].asname != null){
                    buffer.append(" as ");
                    buffer.append(((NameTok)names[i].asname).id);
                }
            }
            name = buffer.toString();
            
        }else if(node instanceof ImportFrom){
            aliasType[] names = ((ImportFrom)node).names;
            StringBuffer buffer = new StringBuffer("from ");
            buffer.append(((NameTok)((ImportFrom)node).module).id);
            buffer.append(" import ");
            if(names.length > 0){
                for (int i = 0; i < names.length; i++) {
                    buffer.append(((NameTok)names[i].name).id);
                    if(names[i].asname != null){
	                    buffer.append(" as ");
	                    buffer.append(((NameTok)names[i].asname).id);
                    }
                }
            }else{
                buffer.append("*");
            }
            name = buffer.toString();
            
        }else if(node instanceof Attribute){
            Attribute a = (Attribute) node;
            name = a.attr;
            
        }else if(node instanceof Name){
            Name a = (Name) node;
            name = a.id;
        }
        if(name == null){
            throw new RuntimeException("Unable to get node name: "+node);
        }else{
            return name;
        }
    }
}
