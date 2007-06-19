/*
 * Created on Jun 10, 2006
 * @author Fabio
 */
package org.python.pydev.outline;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.editor.ErrorDescription;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

public class ParsedItem implements Comparable<Object>{

    private ParsedItem parent;
    public ParsedItem[] children;
    public ASTEntryWithChildren astThis; //may be null if root
    public ASTEntryWithChildren[] astChildrenEntries;
    public String name;
    public ErrorDescription errorDesc;

    /**
     * Constructor for a child with valid ast.
     */
    public ParsedItem(ParsedItem parent, ASTEntryWithChildren root, ASTEntryWithChildren[] astChildren) {
        this(astChildren, null);
        this.parent = parent;
        this.astThis = root;
    }

    /**
     * Constructor for a child with error.
     */
    public ParsedItem(ParsedItem parent, ErrorDescription errorDesc) {
        this.parent = parent;
        this.setErrorDescription(errorDesc);
    }
    
    /**
     * Constructor for the root.
     */
    public ParsedItem(ASTEntryWithChildren[] astChildren, ErrorDescription errorDesc) {
        this.astChildrenEntries = astChildren;
        this.setErrorDescription(errorDesc);
    }

    public void setErrorDescription(ErrorDescription errorDesc) {
        this.errorDesc = errorDesc;
    }

    // returns images based upon element type
    public Image getImage() {
        ImageCache imageCache = PydevPlugin.getImageCache();
        if(astThis == null){
            return imageCache.get(UIConstants.ERROR);
        }
        
        SimpleNode token = astThis.node;
        if (token instanceof ClassDef) {
            return imageCache.get(UIConstants.CLASS_ICON);
        }
        else if (token instanceof FunctionDef) {
            if (NodeUtils.getNameFromNameTok((NameTok) ((FunctionDef)token).name).startsWith("_")) {
                return imageCache.get(UIConstants.PRIVATE_METHOD_ICON);
            }
            else
                return imageCache.get(UIConstants.PUBLIC_METHOD_ICON);
        }
        else if (token instanceof Import) {
            return imageCache.get(UIConstants.IMPORT_ICON);
        }
        else if (token instanceof ImportFrom) {
            return imageCache.get(UIConstants.IMPORT_ICON);
        }
        else if (token instanceof commentType) {
            return imageCache.get(UIConstants.COMMENT);
        }
        else if (token instanceof Attribute || token instanceof Name || token instanceof NameTok) {
            return imageCache.get(UIConstants.PUBLIC_ATTR_ICON);
        }
        else {
            return imageCache.get(UIConstants.ERROR);
        }
    }
    
    public ParsedItem[] getChildren() {
        if(children != null ){
            return children;
        }
        if(astChildrenEntries == null){
            astChildrenEntries = new ASTEntryWithChildren[0];
        }
        
        ArrayList<ParsedItem> items = new ArrayList<ParsedItem>();
        
        //only the root can have an error as a child (from there on, the errors don't contain inner errors)
        if(this.parent == null && errorDesc != null && errorDesc.message != null){
            items.add(new ParsedItem(this, errorDesc));
        }
        
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
        if(errorDesc != null && errorDesc.message != null){
            return errorDesc.message;
        }
        
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
        int rank;
        
        if (astThis == null || (errorDesc != null && errorDesc.message != null)) {
            rank = -2;
        } else if (astThis.node instanceof ImportFrom) {
            rank = 0;
        } else if (astThis.node instanceof Import) {
            rank = 1;
        } else if (astThis.node instanceof commentType) {
            rank = -1;
        } else {
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
        
        if (myRank == rank) {
            if(rank == -1){
                return astThis.node.beginLine < item.astThis.node.beginLine? -1 : 1;
            }else{
                return toString().compareTo(item.toString());
            }
            
        }else {
            return (myRank < rank ? -1 : 1);
        }
    }

}
