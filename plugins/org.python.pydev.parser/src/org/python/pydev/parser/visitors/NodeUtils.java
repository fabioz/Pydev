/*
 * Created on 13/07/2005
 */
package org.python.pydev.parser.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.ISpecialStrOrToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;

public class NodeUtils {

    /**
     * @param node a function definition (if other will return an empty string)
     * @return a string with the representation of the parameters of the function
     */
    public static String getNodeArgs(SimpleNode node) {
        if(node instanceof ClassDef){
            ClassDef c = (ClassDef) node;
            for(stmtType t: c.body){
                if(t instanceof FunctionDef){
                    FunctionDef def = (FunctionDef) t;
                    if(((NameTok)def.name).id.equals("__init__")){
                        node = def;
                        break; //keep going to get the arguments of the __init__
                    }
                }
            }
        }
        
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
        if(node instanceof NameTok){
            NameTok tok = (NameTok) node;
            return tok.id;
        }
        
        if(node instanceof Name){
            Name name = (Name) node;
            return name.id;
        }
        
        if(node instanceof aliasType){
            aliasType type = (aliasType) node;
            return ((NameTok)type.name).id;
        }
        if(node instanceof Attribute){
            Attribute attribute = (Attribute) node;
            return discoverRep(attribute.attr);
            
        }
        
        if(node instanceof keywordType){
            keywordType type = (keywordType) node;
            return discoverRep(type.arg);
        }
        
        if(node instanceof ClassDef){
            ClassDef def = (ClassDef) node;
            return ((NameTok)def.name).id;
        }
        
        if(node instanceof FunctionDef){
            FunctionDef def = (FunctionDef) node;
            return ((NameTok)def.name).id;
        }
        
        if (node instanceof Call){
            Call call = ((Call)node);
            return getRepresentationString(call.func, useTypeRepr);
        }
        
        if (node instanceof org.python.pydev.parser.jython.ast.List || node instanceof ListComp){
            String val = "[]";
            if(useTypeRepr){
                val = getBuiltinType(val);
            }
            return val;
        }
        
        if (node instanceof org.python.pydev.parser.jython.ast.Dict){
            String val = "{}";
            if(useTypeRepr){
                val = getBuiltinType(val);
            }
            return val;
        }
        
        if (node instanceof Str){
            String val;
            if(useTypeRepr){
                val = getBuiltinType("''");
            }else{
                val = "'"+((Str)node).s+"'";
            }
            return val;
        }
        
        if (node instanceof Tuple){
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
        }
        
        if (node instanceof Num){
            String val = ((Num)node).n.toString();
            if(useTypeRepr){
                val = getBuiltinType(val);
            }
            return val;
        }
        
        if (node instanceof Import){
            aliasType[] names = ((Import)node).names;
            for (aliasType n : names) {
                if(n.asname != null){
                    return ((NameTok)n.asname).id;
                }
                return ((NameTok)n.name).id;
            }
        }
        

        if(node instanceof commentType){
            commentType type = (commentType) node;
            return type.id;
        }
        
        if(node instanceof excepthandlerType){
            excepthandlerType type = (excepthandlerType) node;
            return type.name.toString();
            
        }

        
        return null;
    }

    /**
     * @param node
     * @param t
     */
    public static String getNodeDocString(SimpleNode node) {
        Str s = getNodeDocStringNode(node);
        if(s != null){
            return s.s;
        }
        return null;
    }


    public static Str getNodeDocStringNode(SimpleNode node) {
        Str s = null;
        stmtType body[] = null;
        if(node instanceof FunctionDef){
            FunctionDef def = (FunctionDef) node;
            body = def.body;
        } else if(node instanceof ClassDef){
            ClassDef def = (ClassDef) node;
            body = def.body;
            
        }
        if (body != null && body.length > 0) {
            if (body[0] instanceof Expr) {
                Expr e = (Expr) body[0];
                if (e.value instanceof Str) {
                    s = (Str) e.value;
                }
            }
        }
        return s;
    }

    
    
    public static String getFullRepresentationString(SimpleNode node) {
        return getFullRepresentationString(node, false);
    }
    
    public static String getFullRepresentationString(SimpleNode node, boolean fullOnSubscriptOrCall) {
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
            List<SimpleNode> attributeParts = getAttributeParts((Attribute) node);
            StringBuffer buf = new StringBuffer();
            for (Object part : attributeParts) {
                if(part instanceof Call){
                    //stop on a call (that's what we usually want, since the end will depend on the things that 
                    //return from the call).
                    if(!fullOnSubscriptOrCall){
                        return buf.toString();
                    }else{
                        buf.append("()");//call
                    }
                    
                }else if (part instanceof Subscript){
                    if(!fullOnSubscriptOrCall){
                        //stop on a subscript : e.g.: in bb.cc[10].d we only want the bb.cc part
                        return getFullRepresentationString(((Subscript)part).value);
                    }else{
                        buf.append(getFullRepresentationString(((Subscript)part).value));
                        buf.append("[]");//subscript access
                    }

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
    
    public static SimpleNode getNameTokFromNode(SimpleNode ast2){
        if (ast2 instanceof ClassDef){
            ClassDef c = (ClassDef) ast2;
            return c.name;
        }
        if (ast2 instanceof FunctionDef){
            FunctionDef c = (FunctionDef) ast2;
            return c.name;
        }
        return ast2;
        
    }

    public static int getNameLineDefinition(SimpleNode ast2) {
        return getLineDefinition(getNameTokFromNode(ast2));
    }
    
    public static int getNameColDefinition(SimpleNode ast2) {
        return getColDefinition(getNameTokFromNode(ast2));
    }
    
    /**
     * @param ast2 the node to work with
     * @return the line definition of a node
     */
    public static int getLineDefinition(SimpleNode ast2) {
        while(ast2 instanceof Attribute){
            exprType val = ((Attribute)ast2).value;
            if(!(val instanceof Call)){
                ast2 = val;
            }else{
                break;
            }
        }
        if(ast2 instanceof FunctionDef){
            return ((FunctionDef)ast2).name.beginLine;
        }
        if(ast2 instanceof ClassDef){
            return ((ClassDef)ast2).name.beginLine;
        }
        return ast2.beginLine;
    }

    
    public static int getColDefinition(SimpleNode ast2) {
        return getColDefinition(ast2, true);
    }
    
    /**
     * @param ast2 the node to work with
     * @return the column definition of a node
     */
    public static int getColDefinition(SimpleNode ast2, boolean always1ForImports) {
        if(ast2 instanceof Attribute){
            //if it is an attribute, we always have to move backward to the first defined token (Attribute.value)
            exprType value = ((Attribute)ast2).value;
            return getColDefinition(value);
        }
        
        //call and subscript are special cases, because they are not gotten directly (we have to go to the first
        //part of it (which in turn may be an attribute)
        else if(ast2 instanceof Call){
            Call c = (Call) ast2;
            return getColDefinition(c.func);
            
        } else if(ast2 instanceof Subscript){
            Subscript s = (Subscript) ast2;
            return getColDefinition(s.value);
            
        } else if(always1ForImports){
            if(ast2 instanceof Import || ast2 instanceof ImportFrom){
                return 1;
            }
        }
        return getClassOrFuncColDefinition(ast2);
    }


    public static int getClassOrFuncColDefinition(SimpleNode ast2) {
        if(ast2 instanceof ClassDef){
            ClassDef def = (ClassDef) ast2;
            return def.name.beginColumn;
        }
        if(ast2 instanceof FunctionDef){
            FunctionDef def = (FunctionDef) ast2;
            return def.name.beginColumn;
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
        if(v instanceof Expr){
            Expr expr = (Expr) v;
            v = expr.value;
        }
        if(v instanceof ImportFrom){
            ImportFrom f = (ImportFrom) v;
            FindLastLineVisitor findLastLineVisitor = new FindLastLineVisitor();
            try {
                f.accept(findLastLineVisitor);
                SimpleNode lastNode = findLastLineVisitor.getLastNode();
                ISpecialStrOrToken lastSpecialStr = findLastLineVisitor.getLastSpecialStr();
                if(lastSpecialStr != null && lastSpecialStr.toString().equals(")")){
                    //it was an from xxx import (euheon, utehon)
                    return lastSpecialStr.getBeginLine();
                }else{
                    return lastNode.beginLine;
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        if(v instanceof Import){
            Import f = (Import) v;
            FindLastLineVisitor findLastLineVisitor = new FindLastLineVisitor();
            try {
                f.accept(findLastLineVisitor);
                SimpleNode lastNode = findLastLineVisitor.getLastNode();
                return lastNode.beginLine;
            } catch (Exception e) {
                Log.log(e);
            }
        }
        if(v instanceof Str){
            String s = ((Str)v).s;
            int found = 0;
            for (int i = 0; i < s.length(); i++) {
     
                if(s.charAt(i) == '\n'){
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
            
            
        } else if(tok.endsWith("]") && tok.startsWith("[")){
            //ok, we are getting code completion for a list.
            return "list";
            
            
        } else if(tok.endsWith("}") && tok.startsWith("{")){
            //ok, we are getting code completion for a dict.
            return "dict";
            
        } else if(tok.endsWith(")") && tok.startsWith("(")){
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

    /**
     * @return true if the node is a comment import node (and false otherwise).
     */
    public static boolean isComment(SimpleNode ast) {
        if(ast instanceof commentType) {
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


    /**
     * @param lineNumber the line we want to get the context from (starts at 0)
     * @param ast the ast that corresponds to our context
     * @return the full name for the context where we are (in the format Class.method.xxx.xxx)
     */
    public static String getContextName(int lineNumber, SimpleNode ast) {
        if(ast != null){
            EasyASTIteratorVisitor visitor = EasyASTIteratorVisitor.create(ast);
            Iterator<ASTEntry> classesAndMethodsIterator = visitor.getClassesAndMethodsIterator();
            ASTEntry last = null;
            while (classesAndMethodsIterator.hasNext()) {
                ASTEntry entry = classesAndMethodsIterator.next();
                if(entry.node.beginLine > lineNumber+1){
                    //ok, now, let's find out which context actually contains it...
                    break;
                }                
                last = entry;
            }
            
            while(last != null && last.endLine <= lineNumber){
                last = last.parent;
            }
            
            if(last != null){
    
                StringBuffer buffer = new StringBuffer();
                boolean first = true;
                while (last != null){
                    String name = last.getName();
                    buffer.insert(0, name);
                    last = last.parent;
                    if(!first){
                        buffer.insert(name.length(),".");
                    }
                    first = false;
                }
                return buffer.toString();
            }
        }
        return null;
    }

    
    protected static final String[] strTypes = new String[]{
        "'''",
        "\"\"\"",
        "'",
        "\""
    };

    public static String getStringToPrint(Str node){
        StringBuffer buffer = new StringBuffer();
        if(node.unicode){
            buffer.append("u");
        }
        if(node.binary){
            buffer.append("b");
        }
        if(node.raw){
            buffer.append("r");
        }
        final String s = strTypes[node.type-1];
        
        buffer.append(s);
        buffer.append(node.s);
        buffer.append(s);
        return buffer.toString();
    }


    /**
     * @param node the if node that we want to check.
     * @return null if the passed node is not 
     */
    public static boolean isIfMAinNode(If node) {
        if (node.test instanceof Compare) {
            Compare compareNode = (Compare)node.test;
            // handcrafted structure walking
            if (compareNode.left instanceof Name 
                && ((Name)compareNode.left).id.equals("__name__")
                && compareNode.ops != null
                && compareNode.ops.length == 1 
                && compareNode.ops[0] == Compare.Eq){
                
                if ( compareNode.comparators != null
                    && compareNode.comparators.length == 1
                    && compareNode.comparators[0] instanceof Str 
                    && ((Str)compareNode.comparators[0]).s.equals("__main__")){
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * @return true if the given name is a valid python name.
     */
    public static boolean isValidNameRepresentation(String rep) {
        if(rep == null){
            return false;
        }
        if("pass".equals(rep) || rep.startsWith("!<") || rep.indexOf(' ') != -1){
            //Name generated during the parsing (in AbstractPythonGrammar)
            return false;
        }
        
        return true;
    }


    /**
     * Creates an attribute from the passed string
     * 
     * @param attrString: A string as 'a.b.c' or 'self.b' (at least one dot must be in the string) or self.xx()
     * Note that the call is only accepted as the last part.
     * @return an Attribute representing the string.
     */
    public static exprType makeAttribute(String attrString){
        List<String> dotSplit = StringUtils.dotSplit(attrString);
        Assert.isTrue(dotSplit.size() > 1);
        
        exprType first = null;
        Attribute last = null;
        Attribute attr = null;
        for(int i=dotSplit.size()-1;i>0;i--){
            Call call = null;
            String part = dotSplit.get(i);
            if(part.endsWith("()")){
                if(i == dotSplit.size()-1){
                    part = part.substring(0, part.length()-2);
                    call = new Call(null, new exprType[0], new keywordType[0], null, null);
                    first = call;
                }else{
                    throw new RuntimeException("Call only accepted in the last part.");
                }
            }
            attr = new Attribute(null, new NameTok(part, NameTok.Attrib), Attribute.Load);
            if(call != null){
                call.func = attr;
            }
            if(last != null){
                last.value = attr;
            }
            last = attr;
            if(first == null){
                first = last;
            }
        }
        
        String lastPart = dotSplit.get(0);
        if(lastPart.endsWith("()")){
            last.value = new Call(new Name(lastPart.substring(0, lastPart.length()-2), Name.Load, false), null, null, null, null);
        }else{
            last.value = new Name(lastPart, Name.Load, false);
        }
        return first;
    }

}
