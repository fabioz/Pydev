/*
 * Created on Oct 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.text.DecimalFormat;

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
        return getName(str) + "   " +getStmts(stmts)+"     "+exec+"      "+calcCover(stmts, exec) +"  "+notExecuted;
    }
    
    public static String getName(String str){
        if(str.length() > 40){
            str = str.substring(str.length()-37, str.length());
            str = ".. "+str;
        }
        while (str.length() < 40){
            str += " ";
        }
        return str;
    }

    public static String getStmts(int stmts){
        String str = stmts+"";
        while (str.length() < 4){
            str += " ";
        }
        return str;
    }

    public static String getExec(int exec){
        String str = exec+"";
        while (str.length() < 4){
            str += " ";
        }
        return str;
    }

    public static String calcCover( int stmts, int exec){
        double v = ((double)exec) / ((double)stmts) * 100.0;
        DecimalFormat format = new DecimalFormat("##.#");
        String str = format.format(v);
        str += "%";
        while (str.length() < 5){
            str += " ";
        }
        return str;
    }

}

