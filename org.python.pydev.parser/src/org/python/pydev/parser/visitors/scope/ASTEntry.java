/*
 * Created on 12/06/2005
 */
package org.python.pydev.parser.visitors.scope;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
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
public class ASTEntry{
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
    
    public ASTEntry(ASTEntry parent, SimpleNode node){
        this(parent);
        this.node = node;
        this.endLine = node.beginLine;
    }
    
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
            name = ((NameTok)a.attr).id;
            
        }else if(node instanceof Name){
            Name a = (Name) node;
            name = a.id;
            
        }else if(node instanceof NameTok){
            NameTok a = (NameTok) node;
            name = a.id;
        }
        if(name == null){
            throw new RuntimeException("Unable to get node name: "+node);
        }else{
            return name;
        }
    }
}
