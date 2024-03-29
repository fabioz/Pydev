// Autogenerated AST node
package org.python.pydev.parser.jython.ast;

import org.python.pydev.parser.jython.SimpleNode;
import java.util.Arrays;

public final class excepthandlerType extends SimpleNode {
    public exprType type;
    public exprType name;
    public stmtType[] body;
    public boolean isExceptionGroup;

    public excepthandlerType(exprType type, exprType name, stmtType[] body, boolean
    isExceptionGroup) {
        this.type = type;
        this.name = name;
        this.body = body;
        this.isExceptionGroup = isExceptionGroup;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + Arrays.hashCode(body);
        result = prime * result + (isExceptionGroup ? 17 : 137);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        excepthandlerType other = (excepthandlerType) obj;
        if (type == null) { if (other.type != null) return false;}
        else if (!type.equals(other.type)) return false;
        if (name == null) { if (other.name != null) return false;}
        else if (!name.equals(other.name)) return false;
        if (!Arrays.equals(body, other.body)) return false;
        if(this.isExceptionGroup != other.isExceptionGroup) return false;
        return true;
    }
    @Override
    public excepthandlerType createCopy() {
        return createCopy(true);
    }
    @Override
    public excepthandlerType createCopy(boolean copyComments) {
        stmtType[] new0;
        if(this.body != null){
        new0 = new stmtType[this.body.length];
        for(int i=0;i<this.body.length;i++){
            new0[i] = (stmtType) (this.body[i] != null? this.body[i].createCopy(copyComments):null);
        }
        }else{
            new0 = this.body;
        }
        excepthandlerType temp = new
        excepthandlerType(type!=null?(exprType)type.createCopy(copyComments):null,
        name!=null?(exprType)name.createCopy(copyComments):null, new0, isExceptionGroup);
        temp.beginLine = this.beginLine;
        temp.beginColumn = this.beginColumn;
        if(this.specialsBefore != null && copyComments){
            for(Object o:this.specialsBefore){
                if(o instanceof commentType){
                    commentType commentType = (commentType) o;
                    temp.getSpecialsBefore().add(commentType.createCopy(copyComments));
                }
            }
        }
        if(this.specialsAfter != null && copyComments){
            for(Object o:this.specialsAfter){
                if(o instanceof commentType){
                    commentType commentType = (commentType) o;
                    temp.getSpecialsAfter().add(commentType.createCopy(copyComments));
                }
            }
        }
        return temp;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("excepthandler[");
        sb.append("type=");
        sb.append(dumpThis(this.type));
        sb.append(", ");
        sb.append("name=");
        sb.append(dumpThis(this.name));
        sb.append(", ");
        sb.append("body=");
        sb.append(dumpThis(this.body));
        sb.append(", ");
        sb.append("isExceptionGroup=");
        sb.append(dumpThis(this.isExceptionGroup));
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Object accept(VisitorIF visitor) throws Exception {
        if (visitor instanceof VisitorBase) {
            ((VisitorBase) visitor).traverse(this);
        } else {
            traverse(visitor);
        }
        return null;
    }

    @Override
    public void traverse(VisitorIF visitor) throws Exception {
        if (type != null) {
            type.accept(visitor);
        }
        if (name != null) {
            name.accept(visitor);
        }
        if (body != null) {
            for (int i = 0; i < body.length; i++) {
                if (body[i] != null) {
                    body[i].accept(visitor);
                }
            }
        }
    }

}
