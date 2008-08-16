/*
 * Created on Oct 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.structure.FastStringBuffer;

/**
 * @author Fabio Zadrozny
 */
public class FileNode {
    
    public Object node;
    public int stmts;
    public int exec;
    public String notExecuted;
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if(!(obj instanceof FileNode)){
            return false;
        }
        
        FileNode f = (FileNode) obj;
        return f.node.equals(node) && f.exec == exec && f.notExecuted.equals(notExecuted) && f.stmts == stmts; 
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return FileNode.toString(node.toString(), stmts, exec, notExecuted);
    }
    
    public static String toString(String str, int stmts, int exec, String notExecuted) {
        return new FastStringBuffer().
            append(getName(str)).
            append("   ").
            append(getStmts(stmts)).
            append("     ").
            append(exec).
            append("      ").
            append(calcCover(stmts, exec)).
            append("  ").
            append(notExecuted).toString();
    }
    
    public static String getName(String str){
        if(str.length() > 40){
            str = str.substring(str.length()-37, str.length());
            str = ".. "+str;
        }
        while (str.length() < 40){
            str = " "+str;
        }
        return str;
    }

    public static String getStmts(int stmts){
        FastStringBuffer str = new FastStringBuffer();
        str.append(stmts);
        while (str.length() < 4){
            str.insert(0, " ");
        }
        return str.toString();
    }

    public static String getExec(int exec){
        return getStmts(exec);
    }

    public static String calcCover(int stmts, int exec){
        double v = 0;
        if(stmts != 0){
            v = ((double)exec) / ((double)stmts) * 100.0;
        }
        DecimalFormat format = new DecimalFormat("##.#");
        String str = format.format(v);
        str += "%";
        while (str.length() < 5){
            str = " "+str;
        }
        return str;
    }

    /**
     * @return an iterator with the lines that were not executed
     */
    public Iterator<Object> notExecutedIterator() {
        List<Object> l = new ArrayList<Object>();
        
        String[] toks = notExecuted.replaceAll(" ", "").split(",");
        for (int i = 0; i < toks.length; i++) {
            String tok = toks[i].trim();
            if(tok.length() == 0){
            	continue;
            }
			if(tok.indexOf("-") == -1){
                l.add(new Integer(tok));
            }else{
                String[] begEnd = tok.split("-");
                for (int j = Integer.parseInt(begEnd[0]) ; j <= Integer.parseInt(begEnd[1]); j++){
                    l.add(new Integer(j));
                }
            }
        }
        
        return l.iterator();
    }

}

