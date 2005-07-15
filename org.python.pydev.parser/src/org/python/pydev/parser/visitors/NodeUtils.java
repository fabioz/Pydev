/*
 * Created on 13/07/2005
 */
package org.python.pydev.parser.visitors;

import java.lang.reflect.Field;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.Call;
import org.python.parser.ast.Expr;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.ListComp;
import org.python.parser.ast.Num;
import org.python.parser.ast.Str;
import org.python.parser.ast.stmtType;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;

public class NodeUtils {

    /**
     * @param node
     * @return
     */
    public static String getNodeArgs(SimpleNode node) {
        if(node instanceof FunctionDef){
            FunctionDef f = (FunctionDef)node;
            
            String startPar = "( ";
            StringBuffer buffer = new StringBuffer(startPar);
            
            for (int i = 0; i < f.args.args.length; i++) {
                if(buffer.length() > startPar.length()){
                    buffer.append(", ");
                }
                buffer.append( getRepresentationString(f.args.args[i]) );
            }
            buffer.append(" )");
            return buffer.toString();
        }
        return "";
    }

    /**
     * @param node
     * @return
     */
    public static String getRepresentationString(SimpleNode node) {
            
        if (REF.hasAttr(node, "name")) {
            return REF.getAttrObj(node, "name").toString();
            
        }else if (REF.hasAttr(node, "id")) {
            return REF.getAttrObj(node, "id").toString();
    
        }else if (REF.hasAttr(node, "attr")) {
            return REF.getAttrObj(node, "attr").toString();
    
        }else if (REF.hasAttr(node, "arg")) {
            return REF.getAttrObj(node, "arg").toString();
            
        }else if (node instanceof Call){
            Call call = ((Call)node);
            return getRepresentationString(call.func);
            
        }else if (node instanceof org.python.parser.ast.List || node instanceof ListComp){
            return "[]";
    
        }else if (node instanceof Str){
            return "'"+((Str)node).s+"'";
            
        }else if (node instanceof Num){
            return ((Num)node).n.toString();
            
        }
        
        return null;
    }

    /**
     * @param node
     * @param t
     */
    public static String getNodeDocString(SimpleNode node) {
        //and check if it has a docstring.
        if (REF.hasAttr(node, "body")) {
    
            Field field = REF.getAttr(node, "body");
            try {
                Object obj = field.get(node);
                if (obj instanceof stmtType[]) {
                    stmtType body[] = (stmtType[]) obj;
                    if (body.length > 0) {
                        if (body[0] instanceof Expr) {
                            Expr e = (Expr) body[0];
                            if (e.value instanceof Str) {
                                Str s = (Str) e.value;
                                return s.s;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return null;
    }

    public static String getFullRepresentationString(SimpleNode node) {
        if(node instanceof Call){
            Call c = (Call) node;
            node = c.func;
            if (REF.hasAttr(node, "value") && REF.hasAttr(node, "attr")) {
                return getRepresentationString((SimpleNode) REF.getAttrObj(node, "value")) + "." +REF.getAttrObj(node, "attr").toString();
            }
        }
        
        
        if (node instanceof Attribute){
            Attribute a = (Attribute)node;
            return getRepresentationString(a.value) + "."+ a.attr;
        } 
        
        return getRepresentationString(node);
    }

}
