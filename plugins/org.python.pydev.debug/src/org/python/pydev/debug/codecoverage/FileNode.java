/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.structure.FastStringBuffer;

/**
 * @author Fabio Zadrozny
 */
public class FileNode implements ICoverageLeafNode{
    
    public File node;
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
    
    @Override
    public int hashCode() {
        return node.hashCode()*3 + ((exec+1) * 7) + ((stmts+1)*5);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer();
        FileNode.appendToBuffer(buf, node.toString(), stmts, exec, notExecuted);
        return buf.toString();
    }
    
    public FastStringBuffer appendToBuffer(FastStringBuffer buffer, String baseLocation) {
        String name = node.toString();
        if(name.toLowerCase().startsWith(baseLocation.toLowerCase())){
            name = name.substring(baseLocation.length());
        }
        if(name.startsWith("/") || name.startsWith("\\")){
            name = name.substring(1);
        }
        return appendToBuffer(buffer, name, stmts, exec, notExecuted);
    }
    
    /**
     * @param buffer
     * @return
     */
    public static FastStringBuffer appendToBuffer(FastStringBuffer buffer, String str, int stmts, int exec, String notExecuted) {
        buffer.
        append(getName(str)).
        append("   ").
        append(getStmts(stmts)).
        append("     ").
        append(getStmts(exec)).
        append("      ").
        append(calcCover(stmts, exec)).
        append("  ").
        append(notExecuted);
        return buffer;
    }

    
    public static String getName(String str){
        FastStringBuffer buffer = new FastStringBuffer(str, str.length() > 40?0:40-str.length());
        
        if(buffer.length() > 40){
            buffer = buffer.delete(0, Math.abs(37-str.length()));
            buffer.insert(0, "...");
        }
        if (buffer.length() < 40){
            buffer.appendN(' ', 40-str.length());
        }
        return buffer.toString();
    }

    private static String getStmts(int stmts){
        FastStringBuffer str = new FastStringBuffer();
        str.append(stmts);
        while (str.length() < 4){
            str.insert(0, " ");
        }
        return str.toString();
    }


    public static String calcCover(int stmts, int exec){
        double v = 0;
        if(stmts != 0){
            v = ((double)exec) / ((double)stmts) * 100.0;
        }
        DecimalFormat format = new DecimalFormat("###.#");
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

