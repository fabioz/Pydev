/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


public class InterpreterInfo implements Serializable{
    public String executable;
    public java.util.List libs = new ArrayList();
    
    public InterpreterInfo(String exe, Collection c){
        this.executable = exe;
        libs.addAll(c);
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (!(o instanceof InterpreterInfo)){
            return false;
        }

        InterpreterInfo info = (InterpreterInfo) o;
        if(info.executable.equals(this.executable) == false){
            return false;
        }
        
        if(info.libs.equals(this.libs) == false){
            return false;
        }
        
        return true;
    }

    /**
     * 
     */
    public static InterpreterInfo fromString(String str) {
        String[] strings = str.split("\\|");
        String executable = strings[0].substring(strings[0].indexOf(":")+1, strings[0].length());
        ArrayList l = new ArrayList();
        for (int i = 1; i < strings.length; i++) {
            String trimmed = strings[i].trim();
            if(trimmed.length() > 0){
                l.add(trimmed);
            }
        }

        return new InterpreterInfo(executable, l);
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Executable:");
        buffer.append(executable);
        buffer.append("|");
        for (Iterator iter = libs.iterator(); iter.hasNext();) {
            Object e = iter.next();
            buffer.append(e.toString());
            buffer.append("|");
        }
        
        return buffer.toString();
    }
}