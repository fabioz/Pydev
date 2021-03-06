// Autogenerated AST node
package org.python.pydev.parser.jython.ast;

import org.python.pydev.parser.jython.SimpleNode;
import java.util.Arrays;

public final class MatchClass extends patternType {
    public exprType cls;
    public patternType[] args;

    public MatchClass(exprType cls, patternType[] args) {
        this.cls = cls;
        this.args = args;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cls == null) ? 0 : cls.hashCode());
        result = prime * result + Arrays.hashCode(args);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MatchClass other = (MatchClass) obj;
        if (cls == null) { if (other.cls != null) return false;}
        else if (!cls.equals(other.cls)) return false;
        if (!Arrays.equals(args, other.args)) return false;
        return true;
    }
    @Override
    public MatchClass createCopy() {
        return createCopy(true);
    }
    @Override
    public MatchClass createCopy(boolean copyComments) {
        patternType[] new0;
        if(this.args != null){
        new0 = new patternType[this.args.length];
        for(int i=0;i<this.args.length;i++){
            new0[i] = (patternType) (this.args[i] != null?
            this.args[i].createCopy(copyComments):null);
        }
        }else{
            new0 = this.args;
        }
        MatchClass temp = new MatchClass(cls!=null?(exprType)cls.createCopy(copyComments):null,
        new0);
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
        StringBuffer sb = new StringBuffer("MatchClass[");
        sb.append("cls=");
        sb.append(dumpThis(this.cls));
        sb.append(", ");
        sb.append("args=");
        sb.append(dumpThis(this.args));
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Object accept(VisitorIF visitor) throws Exception {
        return visitor.visitMatchClass(this);
    }

    @Override
    public void traverse(VisitorIF visitor) throws Exception {
        if (cls != null) {
            cls.accept(visitor);
        }
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    args[i].accept(visitor);
                }
            }
        }
    }

}