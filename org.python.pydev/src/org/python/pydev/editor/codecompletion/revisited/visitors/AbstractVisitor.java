/*
 * Created on Dec 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.Call;
import org.python.parser.ast.Expr;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.ListComp;
import org.python.parser.ast.Num;
import org.python.parser.ast.Str;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.stmtType;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.utils.REF;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractVisitor extends VisitorBase{

    public static final int GLOBAL_TOKENS = 1;

    public static final int WILD_MODULES = 2;
    
    public static final int ALIAS_MODULES = 3;
    
    public static final int MODULE_DOCSTRING = 4;
    
    public static final int INNER_DEFS = 5;

    protected List tokens = new ArrayList();
    
    /**
     * Module being visited.
     */
    protected String moduleName;
    
    public static String getFullRepresentationString(SimpleNode node) {
	    if (node instanceof Attribute){
	        Attribute a = (Attribute)node;
	        return getRepresentationString(a.value) + "."+ a.attr;
	    } else {
	        return getRepresentationString(node);
	    }
        
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
            return getRepresentationString(((Call)node).func);
            
        }else if (node instanceof org.python.parser.ast.List || node instanceof ListComp){
            return "[]";

        }else if (node instanceof Str){
            return "'"+((Str)node).s+"'";
            
        }else if (node instanceof Num){
            return ((Num)node).n.toString();
            
        }
        System.err.println("Unable to get representation to:"+node);
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
                e.printStackTrace();
            }
        }
        return null;
    }
    
    /**
     * Adds a token with a docstring.
     * 
     * @param node
     */
    protected void addToken(SimpleNode node) {
        //add the token
        SourceToken t = new SourceToken(node, getRepresentationString(node), getNodeArgs(node), getNodeDocString(node), moduleName);
        this.tokens.add(t);
    }

    /**
     * @param node
     * @return
     */
    private String getNodeArgs(SimpleNode node) {
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
     * This method transverses the ast and returns a list of found tokens.
     * 
     * @param ast
     * @param which
     * @param name
     * @return
     * @throws Exception
     */
    public static IToken[] getTokens(SimpleNode ast, int which, String moduleName) throws Exception {
        AbstractVisitor modelVisitor;
        if(which == INNER_DEFS){
            modelVisitor = new InnerModelVisitor();
        }else{
            modelVisitor = new GlobalModelVisitor(which, moduleName);
        }
        
        if (ast != null){
            ast.accept(modelVisitor);
            return (SourceToken[]) modelVisitor.tokens.toArray(new SourceToken[0]);
        }else{
            return new SourceToken[0];
        }
    }
    
}
