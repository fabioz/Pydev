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
    private String doc;
    private String parentPackage;
    private int type;

    public AbstractToken(String rep, String doc, String parentPackage, int type){
        this.rep = rep;
        this.doc = doc;
        this.parentPackage = parentPackage;
        this.type = type;
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
        
        if(getParentPackage().length() > 0){
            return new StringBuffer(getRepresentation()).append(" - ").append(getParentPackage()).toString();
        }else{
            return getRepresentation();
        }
    }

}
