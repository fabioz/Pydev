/*
 * Created on 03/09/2005
 */
package org.python.pydev.core.docutils;

public class StringUtils {

    public static String format(String str, Object ... args){
        StringBuffer buffer = new StringBuffer();
        int j = 0;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if(c == '%' && i+1 < str.length()){
                char nextC = str.charAt(i+1);
                if (nextC == 's'){
                    buffer.append(args[j]);
                    j++;
                    i++;
                }
                else if (nextC == '%'){
                    buffer.append('%');
                    j++;
                    i++;
                }
            }else{
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

    public static int countPercS(String str) {
        int j = 0;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if(c == '%' && i+1 < str.length()){
                char nextC = str.charAt(i+1);
                if (nextC == 's'){
                    j++;
                    i++;
                }
            }
        }
        return j;
    }

    public static String rightTrim(String input) {
        int len = input.length();
        int st = 0;
        int off = 0;      
        char[] val = input.toCharArray();

        while ((st < len) && (val[off + len - 1] <= ' ')) {
            len--;
        }
        return input.substring(0, len);
    }
}
