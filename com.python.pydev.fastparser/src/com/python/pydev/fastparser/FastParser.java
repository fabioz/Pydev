/*
 * Created on 13/07/2005
 */
package com.python.pydev.fastparser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.parser.SimpleNode;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Expr;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Module;
import org.python.parser.ast.Name;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.Num;
import org.python.parser.ast.Str;
import org.python.parser.ast.argumentsType;
import org.python.parser.ast.exprType;
import org.python.parser.ast.expr_contextType;
import org.python.parser.ast.stmtType;
import org.python.pydev.parser.visitors.ParsingUtils;

/**
 * This class is used to do a fast parsing of a file.
 * 
 * It, however does not provide full information on the file. 
 * 
 * The information provided is the following:
 * 
 * - classes definitions (with docstrings if available)
 * - methods definitions (with docstrings if available)
 * - attributes
 * 
 * This class is provided for efficiency and not accuracy, and does 
 * not report any errors on the file. 
 * 
 * The output provided 
 * 
 * @author Fabio
 */
public class FastParser {
    
    static List stmts;
    static IDocument doc;

    public static SimpleNode reparseDocument(String fileContents) throws BadLocationException {
        doc = new Document(fileContents);
        stmts = new ArrayList();
        
        char[] cs = fileContents.toCharArray();
        int length = cs.length;
        
        //we are only interested in class and def
        for(int i = 0 ; i<length; i++){
            char c = cs[i];
            if(i == 0){
                i = searchDef(cs, i, c);
            }
            
            if(c == '\n' || c == '\r'){
                if(i < cs.length-1){
                    searchDef(cs, i+1, cs[i+1]);
                }
                
            } else if(c == '#'){
                i = ParsingUtils.eatComments(cs, i);
                
            }else if(c == '\'' || c == '"'){
                i = ParsingUtils.eatLiterals(cs, null, i);
            }
          
        }
        
        return new Module((stmtType[]) stmts.toArray(new stmtType[0]));
    }




    /**
     * @param cs
     * @param i
     * @param c
     * @return
     * @throws BadLocationException
     */
    private static int searchDef(char[] cs, int i, char c) throws BadLocationException {
        if(c == 'c'){ 
            StringBuffer buf = new StringBuffer();
            i = ParsingUtils.eatToken(cs, buf , i);
            if(buf.toString().equals("class ")){
                i = makeClassDef(cs, i); //return : offset
            }
            
        }else if(c == 'd'){
            StringBuffer buf = new StringBuffer();
            i = ParsingUtils.eatToken(cs, buf , i);
            if(buf.toString().equals("def ")){
                i = makeMethodDef(cs, i);
            }
        }
        return i;
    }




    private static int makeMethodDef(char[] cs, int i) throws BadLocationException {
        //should be something like def method(a,b,c=1):
        FunctionDef def = new FunctionDef(null,null,null, null);
        def.args = new argumentsType(null, null, null, new exprType[]{});
        stmts.add(def);

        StringBuffer buf = new StringBuffer();
        int end = ParsingUtils.eatToColon(cs, buf, i);
        
        //---FIND its name
        int j = getNameEnd(buf);
        def.name = getName(buf, j, NameTok.FunctionName);
        
        char c = buf.charAt(j-1);
        List args = new ArrayList();
        List defaults = new ArrayList();
        if(c == '('){ //method params
            buf.delete(0, j);
            ParsingUtils.removeCommentsAndWhitespaces(buf);
            ParsingUtils.removeToClosingPar(buf);
            String[] strings = buf.toString().replaceAll("\\(", "").replaceAll("\\)", "").split(",");
            for (int k = 0; k < strings.length; k++) {
                String s = strings[k];
                if(s.startsWith("**")){
                    throw new RuntimeException("fix");
//                    def.args.kwarg = s.substring(2);
                }else if(s.startsWith("*")){
                    throw new RuntimeException("fix");
//                    def.args.vararg = s.substring(1);
                }else{
                    int index = s.indexOf('=');
                    if(index<0){
                        Name name = new Name(s, expr_contextType.Store);
                        args.add(name);
                    }else{
                        Name name = new Name(s.substring(0, index), expr_contextType.Store);
                        args.add(name);
                        String defaultVal = s.substring(index+1);
                        defaults.add(makeDefault(defaultVal));
                    }
                }
            }

            def.args.args = (exprType[]) args.toArray(new exprType[0]);
            def.args.defaults = (exprType[]) defaults.toArray(new exprType[0]);
        }else{
            throw new RuntimeException("expecting '('");
        }
        
        StringBuffer buf2 = new StringBuffer();
        int[] startEnd = getLiteralDocs(cs, end, end, buf2);
        end = startEnd[1];
        def.body = getPyDocs(buf2, startEnd[0], end);

        return end;
    }




    /**
     * @param defaultVal
     * @return
     */
    private static SimpleNode makeDefault(String defaultVal) {
        int index;
        try {
            Integer.parseInt(defaultVal);
            return new Num(defaultVal);
        } catch (Exception e) {
        }
        if(defaultVal.startsWith("'")){
            defaultVal = removeMarks(defaultVal, '\'');
            return new Str(defaultVal);
        }
        else if(defaultVal.startsWith("\"")){
            defaultVal = removeMarks(defaultVal, '"');
            return new Str(defaultVal);
        }
        else if((index = defaultVal.indexOf(".")) != -1){
            String id = defaultVal.substring(0,index);
            String attr = defaultVal.substring(index+1,defaultVal.length());
            return new Attribute(new Name(id, Name.Load), new NameTok(attr, NameTok.Attrib), Attribute.Load);
        }
        return new Str(defaultVal);
    }

    private static int makeClassDef(char[] cs, int i) throws BadLocationException {
        //should be something like class    -->Class1(object):<--
        ClassDef def = new ClassDef(null,new exprType[0],null);
        stmts.add(def);

        StringBuffer buf = new StringBuffer();

        int end = ParsingUtils.eatToColon(cs, buf, i);
        
        //---FIND its name
        int j = getNameEnd(buf);
        def.name = getName(buf, j, NameTok.ClassName);
        
        char c = buf.charAt(j-1);
        int k = j;
        List args = new ArrayList();
        if(c == '('){ //method params
            j = ParsingUtils.eatWhitespaces(buf, k);
            k = j;
            do{
                c = buf.charAt(k);
                if(c == ','){
                    String s = buf.substring(j, k);
                    Name name = new Name(s.trim(), expr_contextType.Load);
                    args.add(name);
                    j = ParsingUtils.eatWhitespaces(cs, k+1)+1;
                    k = j;
                }
                
                k++;
            }while(c != ')' && k < buf.length());
            String s = buf.substring(j, k-1);
            Name name = new Name(s.trim(), expr_contextType.Load);
            args.add(name);

            def.bases = (exprType[]) args.toArray(new exprType[0]);
        }


        StringBuffer buf2 = new StringBuffer();
        int[] startEnd = getLiteralDocs(cs, i, end, buf2);
        end = startEnd[1];
        def.body = getPyDocs(buf2, startEnd[0], end);
        return end;
    }




    /**
     * @param buf
     * @param j
     * @return
     */
    private static NameTok getName(StringBuffer buf, int j, int ctx) {
        String n = buf.substring(0, j-1).trim();
        NameTok tok = new NameTok(n, ctx);
        return tok;
    }




    /**
     * @param buf
     * @return
     */
    private static int getNameEnd(StringBuffer buf) {
        int j = 0;
        char ch;
        do{
            ch = buf.charAt(j);
            j++;
        }while(ch != ':' && ch != '(' && j < buf.length());
        return j;
    }




    /**
     * @param buf2
     * @param end 
     * @param i 
     * @return
     * @throws BadLocationException 
     */
    private static stmtType[] getPyDocs(StringBuffer buf2, int i, int end) throws BadLocationException {
        
        stmtType[] s = new stmtType[]{};
        if(buf2.length() > 0){
            Str str = new Str(buf2.toString());

            Expr expr = new Expr(str);
            
            s = new stmtType[]{expr};
        }
        return s;
    }


    protected static void setBegColLine(SimpleNode expr, int abs) throws BadLocationException {
        int[] colLine = getColLine(abs);
        expr.beginColumn = colLine[0];
        expr.beginLine = colLine[1];
    }




    protected static int [] getColLine(int abs) throws BadLocationException{
        int[] r = new int[2];
        int lineOfOffset = doc.getLineOfOffset(abs); //we don't want anything else in this line
        IRegion lineInformation = doc.getLineInformation(lineOfOffset);
        r[0] = abs - lineInformation.getOffset()+1;
        r[1] = lineOfOffset+1;
        return r;
    }

    /**
     * @param cs
     * @param i
     * @param end
     * @param buf2
     * @return
     * @throws BadLocationException
     */
    private static int[] getLiteralDocs(char[] cs, int i, int end, StringBuffer buf2) throws BadLocationException {
        int lineOfOffset = doc.getLineOfOffset(i); //we don't want anything else in this line
        IRegion lineInformation = doc.getLineInformation(lineOfOffset + 1);
        String text = getText(lineInformation);
        
        int t1 = text.indexOf('\'');
        int t2 = text.indexOf('\"');
        int t = t1;
        char c = '\'';
        if(t2 != -1 && t2 < t1){
            t = t2;
            c = '"';
        }
    
        if(t != -1){
            end = ParsingUtils.eatLiterals(cs, buf2, lineInformation.getOffset()+t);
            removeMarks(buf2, c);
        }
        return new int[]{lineInformation.getOffset()+t, end};
    }

    private static String removeMarks(String buf, char c) {
        StringBuffer buffer = new StringBuffer(buf);
        return removeMarks(buffer, c).toString();
    }
    private static StringBuffer removeMarks(StringBuffer buf2, char c) {
        if(buf2.charAt(1) == c && buf2.length() > 2 && buf2.charAt(2) == c){
            buf2.delete(0, 3);
            buf2.delete(buf2.length()-3, buf2.length());
        }else{
            buf2.deleteCharAt(0);
            buf2.deleteCharAt(buf2.length()-1);
        }
        return buf2;
    }




    protected static int getLine(int i) throws BadLocationException {
        return doc.getLineOfOffset(i)+1;
    }

    private static String getText(IRegion lineInformation) throws BadLocationException {
        return doc.get(lineInformation.getOffset(), lineInformation.getLength());
    }

    
}
