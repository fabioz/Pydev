/*
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.python.pydev.editor.codecompletion.PyCodeCompletion;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractToken implements IToken{

    private String rep;
    private String originalRep;
    private String doc;
    private String args;
    private String parentPackage;
    public int type;

    public AbstractToken(String rep, String doc, String args, String parentPackage, int type, String originalRep){
        this(rep, doc, args, parentPackage, type);
        this.originalRep = originalRep;
    }
    
    public AbstractToken(String rep, String doc, String args, String parentPackage, int type){
        if (rep != null)
            this.rep = rep;
        else
            this.rep = "";
        
        if (args != null)
            this.args = args;
        else
            this.args = "";
        
        this.originalRep = this.rep;
        
        if (doc != null)
            this.doc = doc;
        else
            this.doc = "";
        
        
        if (parentPackage != null)
            this.parentPackage = parentPackage;
        else
            this.parentPackage = "";
        
        this.type = type;
    }
    
    
    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IToken#getArgs()
     */
    public String getArgs() {
        return args;
    }
    
    /**
     * @see org.python.pydev.editor.javacodecompletion.IToken#getRepresentation()
     */
    public String getRepresentation() {
        return rep;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.IToken#getDocStr()
     */
    public String getDocStr() {
        return doc;
    }
    
    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IToken#getParentPackage()
     */
    public String getParentPackage() {
        return parentPackage;
    }
    
    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IToken#getType()
     */
    public int getType() {
        return type;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractToken)) {
            return false;
        }

        AbstractToken c = (AbstractToken) obj;
        
        if(c.getRepresentation().equals(getRepresentation()) == false){
            return false;
        }
        
        if(c.getDocStr().equals(getDocStr()) == false){
            return false;
        }

        if(c.getParentPackage().equals(getParentPackage()) == false){
            return false;
        }

        if(c.getType() != getType()){
            return false;
        }
        
        return true;
            
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getRepresentation().hashCode() * getType();
    }


    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        AbstractToken comp = (AbstractToken) o;
        
        int thisT = getType();
        int otherT = comp.getType();
        
        if(thisT != otherT){
            if (thisT == PyCodeCompletion.TYPE_PARAM)
                return -1;

            if (otherT == PyCodeCompletion.TYPE_PARAM)
                return 1;

            if (thisT == PyCodeCompletion.TYPE_IMPORT)
                return -1;

            if (otherT == PyCodeCompletion.TYPE_IMPORT)
                return 1;
        }
        
        
        int c = getRepresentation().compareTo(comp.getRepresentation());
        if (c!= 0)
            return c;
        
        c = getParentPackage().compareTo(comp.getParentPackage());
        if (c!= 0)
            return c;
        
        return c;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        
        if(getParentPackage() != null && getParentPackage().length() > 0){
            return new StringBuffer(getRepresentation()).append(" - ").append(getParentPackage()).toString();
        }else{
            return getRepresentation();
        }
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IToken#getCompletePath()
     */
    public String getCompletePath() {
        String p = getParentPackage();
        if( p != null && p.length()>0){
            return p+"."+originalRep;
        }
        return originalRep;
    }

    public int getLineDefinition() {
        return UNDEFINED;
    }

    public int getColDefinition() {
        return UNDEFINED;
    }

    public boolean isImport() {
        return false;
    }
}
