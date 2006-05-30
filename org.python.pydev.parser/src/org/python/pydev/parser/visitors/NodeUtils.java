/*
 * Created on 13/07/2005
 */
package org.python.pydev.parser.visitors;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;

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
     * Get the representation for the passed parameter (if it is a String, it is itself, if it
     * is a SimpleNode, get its representation
     */
    private static String discoverRep(Object o){
    	if(o instanceof String){
    		return (String) o;
    	}
    	if(o instanceof NameTok){
    	    return ((NameTok) o).id;
    	}
    	if(o instanceof SimpleNode){
    		return getRepresentationString((SimpleNode) o);
    	}
    	throw new RuntimeException("Expecting a String or a SimpleNode");
    }
    
    public static String getRepresentationString(SimpleNode node) {
    	return getRepresentationString(node, false);
    }
    
    /**
     * @param node this is the node from whom we want to get the representation
     * @return A suitable String representation for some node.
     */
    public static String getRepresentationString(SimpleNode node, boolean useTypeRepr) {
            
        if (REF.hasAttr(node, "name")) {
            Object attrObj = REF.getAttrObj(node, "name");
            if(attrObj instanceof NameTok){
                NameTok n = (NameTok) attrObj;
                return n.id;
            }
            return attrObj.toString();
            
        }else if (REF.hasAttr(node, "id")) {
            return discoverRep(REF.getAttrObj(node, "id"));
    
        }else if (REF.hasAttr(node, "attr")) {
            return discoverRep(REF.getAttrObj(node, "attr"));
    
        }else if (REF.hasAttr(node, "arg")) {
            return discoverRep(REF.getAttrObj(node, "arg"));
            
        }else if (node instanceof Call){
            Call call = ((Call)node);
            return getRepresentationString(call.func, useTypeRepr);
            
        }else if (node instanceof org.python.pydev.parser.jython.ast.List || node instanceof ListComp){
            String val = "[]";
            if(useTypeRepr){
            	val = getBuiltinType(val);
            }
			return val;
    
        }else if (node instanceof Str){
            String val = "'"+((Str)node).s+"'";
            if(useTypeRepr){
            	val = getBuiltinType(val);
            }
			return val;

        }else if (node instanceof Tuple){
            StringBuffer buf = new StringBuffer();
            Tuple t = (Tuple)node;
            for ( exprType e : t.elts){
                buf.append(getRepresentationString(e, useTypeRepr));
                buf.append(", ");
            }
            if(t.elts.length > 0){
                int l = buf.length();
                buf.deleteCharAt(l-1);
                buf.deleteCharAt(l-2);
            }
            String val = "("+buf+")";
            if(useTypeRepr){
            	val = getBuiltinType(val);
            }
			return val;

            
        }else if (node instanceof Num){
            String val = ((Num)node).n.toString();
            if(useTypeRepr){
            	val = getBuiltinType(val);
            }
            return val;
            
        }else if (node instanceof Import){
            aliasType[] names = ((Import)node).names;
            for (aliasType n : names) {
                if(n.asname != null){
                    return ((NameTok)n.asname).id;
                }
                return ((NameTok)n.name).id;
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
    	if (node instanceof Dict){
    		return "dict";
    	}
    	
        if (node instanceof Str || node instanceof Num){
            return getRepresentationString(node, true);
        } 
        
        if (node instanceof Tuple){
            return getRepresentationString(node, true);
        } 
        
        if (node instanceof Subscript){
            return getFullRepresentationString(((Subscript)node).value);
        } 
        
        
        if(node instanceof Call){
            Call c = (Call) node;
            node = c.func;
            if (REF.hasAttr(node, "value") && REF.hasAttr(node, "attr")) {
                return getFullRepresentationString((SimpleNode) REF.getAttrObj(node, "value")) + "." +discoverRep(REF.getAttrObj(node, "attr"));
            }
        }
        
        
        if (node instanceof Attribute){
        	//attributes are tricky because we only have backwards access initially, so, we have to:
        	
        	//get it forwards
        	List attributeParts = getAttributeParts((Attribute) node);
        	StringBuffer buf = new StringBuffer();
        	for (Object part : attributeParts) {
				if(part instanceof Call){
					//stop on a call (that's what we usually want, since the end will depend on the things that 
					//return from the call).
					return buf.toString();
					
				}else if (part instanceof Subscript){
					//stop on a subscript : e.g.: in bb.cc[10].d we only want the bb.cc part
		            return getFullRepresentationString(((Subscript)part).value);

				}else{
					//otherwise, just add another dot and keep going.
					if(buf.length() > 0){
						buf.append(".");
					}
					buf.append(getRepresentationString((SimpleNode) part, true));
				}
			}
        	return buf.toString();
        	
        } 
        
        return getRepresentationString(node, true);
    }
    
    
    /**
     * line and col start at 1
     */
    public static boolean isWithin(int line, int col, SimpleNode node){
    	int colDefinition = NodeUtils.getColDefinition(node);
    	int lineDefinition = NodeUtils.getLineDefinition(node);
    	int[] colLineEnd = NodeUtils.getColLineEnd(node, false);
    	
    	if(lineDefinition <= line && colDefinition <= col &&
    		colLineEnd[0] >= line && colLineEnd[1] >= col){
    		return true;
    	}
    	return false;
    }
    

    public static int getNameLineDefinition(SimpleNode ast2) {
        if (ast2 instanceof ClassDef){
            ClassDef c = (ClassDef) ast2;
            return getLineDefinition(c.name);
        }
        if (ast2 instanceof FunctionDef){
            FunctionDef c = (FunctionDef) ast2;
            return getLineDefinition(c.name);
        }
        return getLineDefinition(ast2);
    }
    
    public static int getNameColDefinition(SimpleNode ast2) {
        if (ast2 instanceof ClassDef){
            ClassDef c = (ClassDef) ast2;
            return getColDefinition(c.name);
        }
        if (ast2 instanceof FunctionDef){
            FunctionDef c = (FunctionDef) ast2;
            return getColDefinition(c.name);
        }
        return getColDefinition(ast2);
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
        if(ast2 instanceof FunctionDef){
            return ((FunctionDef)ast2).name.beginLine;
        }
        return ast2.beginLine;
    }

    
    /**
     * @param ast2 the node to work with
     * @return the column definition of a node
     */
    public static int getColDefinition(SimpleNode ast2) {
        if(ast2 instanceof Attribute){
            //if it is an attribute, we always have to move backward to the first defined token (Attribute.value)
            
            
            exprType value = ((Attribute)ast2).value;
            
            
            //call and subscript are special cases, because they are not gotten directly (we have to go to the first
            //part of it (which in turn may be an attribute)
            if(value instanceof Call){
                Call c = (Call) value;
                return getColDefinition(c.func);
                
            } else if(value instanceof Subscript){
                Subscript s = (Subscript) value;
                return getColDefinition(s.value);
                
            } else {
                return getColDefinition(value);
            }
        }
        if(ast2 instanceof Import || ast2 instanceof ImportFrom){
            return 1;
        }
        return ast2.beginColumn;
    }


    public static int[] getColLineEnd(SimpleNode v) {
    	return getColLineEnd(v, true);
    }

    /**
     * @param v the token to work with
     * @return a tuple with [line, col] of the definition of a token
     */
    public static int[] getColLineEnd(SimpleNode v, boolean getOnlyToFirstDot) {
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
        
        col = getEndColFromRepresentation(v, getOnlyToFirstDot);
        return new int[]{lineEnd, col};
    }

    /**
     * @param v
     * @return
     */
    private static int getEndColFromRepresentation(SimpleNode v, boolean getOnlyToFirstDot) {
        int col;
        String representationString = getFullRepresentationString(v);
        if(representationString == null){
            return -1;
        }
        
        if(getOnlyToFirstDot){
	        int i;
	        if((i = representationString.indexOf('.') )  != -1){
	            representationString = representationString.substring(0,i);
	        }
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

    public static String getNameFromNameTok(NameTok tok){
        return tok.id;
    }


    /**
     * Gets all the parts contained in some attribute in the right order (when we visit
     * some attribute, we have to get that in a backwards fashion, since the attribute
     * is only determined in the end of the token in the grammar)
     * 
     * @return a list with the attribute parts in its forward order, and not backward as presented
     * in the grammar.
     */
	public static List<SimpleNode> getAttributeParts(Attribute node) {
		ArrayList<SimpleNode> nodes = new ArrayList<SimpleNode>();
		
		nodes.add(node.attr);
		SimpleNode s = node.value;
		
		while(true){
			if(s instanceof Attribute){
				nodes.add(s);
				s = ((Attribute) s).value;
				
			}else if(s instanceof Call){
				nodes.add(s);
				s = ((Call) s).func;
				
			}else{
				nodes.add(s);
				break;
			}
		}
		
		Collections.reverse(nodes);
		
		return nodes;
	}


    /**
     * Gets the parent names for a class definition
     * 
     * @param onlyLastSegment determines whether we should return only the last segment if the name
     * of the parent resolves to a dotted name.
     */
    public static List<String> getParentNames(ClassDef def, boolean onlyLastSegment) {
        ArrayList<String> ret = new ArrayList<String>();
        for(exprType base: def.bases){
            String rep = getFullRepresentationString(base);
            if(onlyLastSegment){
                rep = FullRepIterable.getLastPart(rep);
            }
            ret.add(rep);
        }
        return ret;
    }


    /**
     * @return true if the node is an import node (and false otherwise).
     */
	public static boolean isImport(SimpleNode ast) {
        if(ast instanceof Import || ast instanceof ImportFrom){
            return true;
        }
        return false;
	}


	public static NameTok getNameForAlias(aliasType t) {
		if(t.asname != null){
			return (NameTok) t.asname;
		}else{
			return (NameTok) t.name;
		}
	}


	public static NameTok getNameForRep(aliasType[] names, String representation) {
		for (aliasType name : names) {
			NameTok nameForAlias = getNameForAlias(name);
			String aliasRep = NodeUtils.getRepresentationString(nameForAlias);
			if(representation.equals(aliasRep)){
				return nameForAlias;
			}
		}
		return null;
	}

    

}
