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
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Expr;
import org.python.parser.ast.Module;
import org.python.parser.ast.Name;
import org.python.parser.ast.Str;
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
                
            }else if(c == '#'){
                StringBuffer buf = new StringBuffer();
                i = ParsingUtils.eatComments(cs, buf, i);
                
            }
          
        }
        
        return new Module((stmtType[]) stmts.toArray(new stmtType[0]));
    }




    private static int makeMethodDef(char[] cs, int i) {
        //should be something like class C(object):
        StringBuffer buf = new StringBuffer();
        i = ParsingUtils.eatToColon(cs, buf, i);
        
        System.out.println(buf);
        
        return i;
    }

    private static int makeClassDef(char[] cs, int i) throws BadLocationException {
        //should be something like class    -->Class1(object):<--
        ClassDef def = new ClassDef(null,null,null);
        stmts.add(def);
        def.beginColumn = 1;
        def.beginLine = getLine(i -5);

        StringBuffer buf = new StringBuffer();

        int end = ParsingUtils.eatToColon(cs, buf, i);
        
        int j = 0;
        char c;
        do{
            c = buf.charAt(j);
            j++;
        }while(c != ':' && c != '(' && j < buf.length());
        
        //---FOUND its name
        def.name = buf.substring(0, j-1).trim();
        
        List bases = new ArrayList();
        int k = j;
        if(c == '('){ //class has bases
            do{
                c = buf.charAt(k);
                k++;
            }while(c != ')' && k < buf.length());
            String s = buf.substring(j, k-1);
            Name name = new Name(s.trim(), expr_contextType.Load);
            name.beginColumn = i+j+1;
            name.beginLine = getLine(i+j);
            bases.add(name);
        }
        
        def.bases = (exprType[]) bases.toArray(new exprType[0]);

        StringBuffer buf2 = new StringBuffer();
        end = getLiteralDocs(cs, i, end, buf2);    
        def.body = getPyDocs(buf2);
        return end;
    }




    /**
     * @param buf2
     * @return
     */
    private static stmtType[] getPyDocs(StringBuffer buf2) {
        stmtType[] s = new stmtType[]{};
        if(buf2.length() > 0){
            Str str = new Str(buf2.toString());
            str.beginColumn = 5;
            str.beginLine = 2;

            Expr expr = new Expr(str);
            expr.beginColumn = 17;
            expr.beginLine = 2;
            
            s = new stmtType[]{expr};
        }
        return s;
    }




    /**
     * @param cs
     * @param i
     * @param end
     * @param buf2
     * @return
     * @throws BadLocationException
     */
    private static int getLiteralDocs(char[] cs, int i, int end, StringBuffer buf2) throws BadLocationException {
        int lineOfOffset = doc.getLineOfOffset(i); //we don't want anything else in this line
        IRegion lineInformation = doc.getLineInformation(lineOfOffset + 1);
        String text = getText(lineInformation);
        
        int t1 = text.indexOf('\'');
        int t2 = text.indexOf('\"');
        if(t1 != -1){
            end = ParsingUtils.eatLiterals(cs, buf2, lineInformation.getOffset()+t1);
            remove(buf2, '\'');
        }else if(t2 != -1){
            end = ParsingUtils.eatLiterals(cs, buf2, lineInformation.getOffset()+t2);
            remove(buf2, '\"');
        }
        return end;
    }

    private static void remove(StringBuffer buf2, char c) {
        if(buf2.charAt(1) == c && buf2.charAt(2) == c){
            buf2.delete(0, 2);
            buf2.delete(buf2.length()-3, buf2.length()-1);
        }else{
            buf2.deleteCharAt(0);
            buf2.deleteCharAt(buf2.length()-1);
        }
    }




    private static int getLine(int i) throws BadLocationException {
        return doc.getLineOfOffset(i)+1;
    }

    private static String getText(IRegion lineInformation) throws BadLocationException {
        return doc.get(lineInformation.getOffset(), lineInformation.getLength());
    }

    
}
