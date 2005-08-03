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
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.ListComp;
import org.python.parser.ast.Num;
import org.python.parser.ast.Str;
import org.python.parser.ast.Subscript;
import org.python.parser.ast.Tuple;
import org.python.parser.ast.aliasType;
import org.python.parser.ast.exprType;
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
            
        }else if (node instanceof Tuple){
            StringBuffer buf = new StringBuffer();
            Tuple t = (Tuple)node;
            for ( exprType e : t.elts){
                buf.append(getRepresentationString(e));
                buf.append(", ");
            }
            if(t.elts.length > 0){
                int l = buf.length();
                buf.deleteCharAt(l-1);
                buf.deleteCharAt(l-2);
            }
            return "("+buf+")";
            
        }else if (node instanceof Num){
            return ((Num)node).n.toString();
            
        }else if (node instanceof Import){
            aliasType[] names = ((Import)node).names;
            for (aliasType n : names) {
                if(n.asname != null){
                    return n.asname;
                }
                return n.name;
            }
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
                return getFullRepresentationString((SimpleNode) REF.getAttrObj(node, "value")) + "." +REF.getAttrObj(node, "attr").toString();
            }
        }
        
        
        if (node instanceof Attribute){
            Attribute a = (Attribute)node;
            String fullRepresentationString = getFullRepresentationString(a.value);
            if(fullRepresentationString == null){
                return null;
            }
            return fullRepresentationString + "."+ a.attr;
        } 
        
        if (node instanceof Str || node instanceof Num){
            return NodeUtils.getBuiltinType( getRepresentationString(node) );
        } 
        
        if (node instanceof Tuple){
            return NodeUtils.getBuiltinType( getRepresentationString(node) );
        } 
        
        if (node instanceof Subscript){
            return getFullRepresentationString(((Subscript)node).value);
        } 
        
        
        return getRepresentationString(node);
    }
    
    

    /**
     * @param ast2 the node to work with
     * @return the line definition of a node
     */
    public static int getLineDefinition(SimpleNode ast2) {
        if(ast2 instanceof Attribute){
            if(!(((Attribute)ast2).value instanceof Call)){
                return getLineDefinition(((Attribute)ast2).value);
            }
        }
        return ast2.beginLine;
    }

    
    /**
     * @param ast2 the node to work with
     * @return the column definition of a node
     */
    public static int getColDefinition(SimpleNode ast2) {
        if(ast2 instanceof Attribute){
            if(!(((Attribute)ast2).value instanceof Call)){
                return getColDefinition(((Attribute)ast2).value);
            }
        }
        if(ast2 instanceof Import || ast2 instanceof ImportFrom){
            return 1;
        }
        return ast2.beginColumn;
    }



    /**
     * @param v the token to work with
     * @return a tuple with [line, col] of the definition of a token
     */
    public static int[] getColLineEnd(SimpleNode v) {
        int lineEnd = getLineEnd(v);
        int col = 0;

        if(v instanceof Import || v instanceof ImportFrom){
            return new int[]{lineEnd, -1}; //col is -1... import is always full line
        }
        
        
        if(v instanceof Str){
            if(lineEnd == getLineDefinition(v)){
                String s = ((Str)v).s;
                col = getColDefinition(v) + s.length();
                return new int[]{lineEnd, col};
            }else{
                //it is another line...
                String s = ((Str)v).s;
                int i = s.lastIndexOf('\n');
                String sub = s.substring(i, s.length());
                
                col = sub.length();
                return new int[]{lineEnd, col};
            }
        }
        
        col = getFromRepresentation(v);
        return new int[]{lineEnd, col};
    }

    /**
     * @param v
     * @return
     */
    private static int getFromRepresentation(SimpleNode v) {
        int col;
        String representationString = getFullRepresentationString(v);
        if(representationString == null){
            return -1;
        }
        
        int i;
        if((i = representationString.indexOf('.') )  != -1){
            representationString = representationString.substring(0,i);
        }
        int colDefinition = getColDefinition(v);
        if(colDefinition == -1){
            return -1;
        }
        
        col = colDefinition + representationString.length();
        return col;
    }
    
    public static int getLineEnd(SimpleNode v) {
        if(v instanceof Str){
            String s = ((Str)v).s;
            char[] cs = s.toCharArray();
            int found = 0;
            for (int i = 0; i < cs.length; i++) {
     
                if(cs[i] == '\n'){
                    found += 1;
                }
            }
            return getLineDefinition(v) + found;
        }
        return getLineDefinition(v);
    }

    /**
     * @return the builtin type (if any) for some token (e.g.: '' would return str, 1.0 would return float...
     */
    public static String getBuiltinType(String tok) {
        if(tok.endsWith("'") || tok.endsWith("\"")){
            //ok, we are getting code completion for a string.
            return "str";
            
            
        } else if(tok.endsWith("]")){
            //ok, we are getting code completion for a list.
            return "list";
            
            
        } else if(tok.endsWith("}")){
            //ok, we are getting code completion for a dict.
            return "dict";
            
        } else if(tok.endsWith(")")){
            //ok, we are getting code completion for a tuple.
            return "tuple";
            
            
        } else {
            try {
                Integer.parseInt(tok);
                return "int";
            } catch (Exception e) { //ok, not parsed as int
            }
    
            try {
                Float.parseFloat(tok);
                return "float";
            } catch (Exception e) { //ok, not parsed as int
            }
        }
        
        return null;
    }

    

}
