/*
 * Created on Jun 10, 2006
 * @author Fabio
 */
package org.python.pydev.outline;

import java.util.ArrayList;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;

public class ParsedItem implements Comparable{

    private ParsedItem parent;
    public ParsedItem[] children;
    public ASTEntryWithChildren astThis; //may be null if root
    public ASTEntryWithChildren[] astChildrenEntries;
    public String name;

    public ParsedItem(ParsedItem parent, ASTEntryWithChildren root, ASTEntryWithChildren[] astChildren) {
        this(astChildren);
        this.parent = parent;
        this.astThis = root;
        
    }
    public ParsedItem(ASTEntryWithChildren[] astChildren) {
        this.astChildrenEntries = astChildren;
    }

    public ParsedItem[] getChildren() {
        if(children != null ){
            return children;
        }
        if(astChildrenEntries == null){
            return new ParsedItem[0];
        }
        
        ArrayList<ParsedItem> items = new ArrayList<ParsedItem>();
        for(ASTEntryWithChildren c : astChildrenEntries){
            items.add(new ParsedItem(this, c, c.getChildren()));
        }
        children = items.toArray(new ParsedItem[items.size()]);
        return children;
    }

    public ParsedItem getParent() {
        return parent;
    }

    public String toString() {
        if(name != null){
            return name;
        }
        if (astThis == null){
            name = "null";
            
        } else if (astThis.node instanceof Import) {
            aliasType[] imports = ((Import)astThis.node).names;
            StringBuffer retVal = new StringBuffer();
            for (int i=0; i<imports.length; i++) {
                aliasType aliasType = imports[i];
                
                //as ...
                if(aliasType.asname != null){
                    retVal.append(((NameTok)aliasType.asname).id);
                    retVal.append(" = ");
                }

                retVal.append(((NameTok)aliasType.name).id);
                retVal.append(", ");
            }
            retVal.delete(retVal.length() - 2, retVal.length());
            name = retVal.toString();
            
        }else if (astThis.node instanceof ImportFrom) {
            // from wxPython.wx import *
            ImportFrom importToken = (ImportFrom)astThis.node;
            StringBuffer modules = new StringBuffer();
            for (int i=0; i<importToken.names.length;i++) {
                aliasType aliasType = importToken.names[i];

                //as ...
                if(aliasType.asname != null){
                    modules.append(((NameTok)aliasType.asname).id);
                    modules.append(" = ");
                }

                modules.append(((NameTok)aliasType.name).id);
                modules.append(",");
            }
            if (modules.length() == 0) {
                modules.append("*,"); //the comma will be deleted
            }
            modules.deleteCharAt(modules.length()-1);
            name = modules.toString() + " (" + ((NameTok)importToken.module).id + ")";
            
        }else if (astThis.node instanceof commentType) {
            commentType type = (commentType) astThis.node;
            String rep = type.id.trim();
            rep = FullRepIterable.split(rep, '\n')[0];
            rep = FullRepIterable.split(rep, '\r')[0];
            
            for (int i = 1; i < rep.length(); i++) {
                char c = rep.charAt(i);
                if(c != '-'){
                    name = rep.substring(i, rep.length());
                    break;
                }
            }
            if(name == null){
                name = "---";
            }
            
        }else {
            name = NodeUtils.getFullRepresentationString(astThis.node);
        }
        
        return name;
    }
    
    /**
     * @return rank for sorting ParserItems. When comparing
     * two items, first we compare class ranking, then titles
     */
    public int getClassRanking() {
        int rank = 0;
        if (astThis.node instanceof ImportFrom) {
            rank = 0;
        } else if (astThis.node instanceof Import) {
            rank = 1;
        } else if (astThis.node instanceof commentType) {
            rank = -1;
        } else{
            rank = 10;
        }
        return rank;
    }

    public int compareTo(Object o) {
        if(!(o instanceof ParsedItem)){
            return 0;
        }
        ParsedItem item = (ParsedItem) o;
        int myRank = getClassRanking();
        int rank = item.getClassRanking();
        
        if(myRank == -1 || rank == -1){
            return 0;
        }
        
        if (myRank == rank) {
            return toString().compareTo(item.toString());
            
        }else {
            return (myRank < rank ? -1 : 1);
        }
    }

}
