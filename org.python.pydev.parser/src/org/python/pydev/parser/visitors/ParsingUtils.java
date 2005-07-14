/*
 * Created on 13/07/2005
 */
package org.python.pydev.parser.visitors;



public class ParsingUtils {

    /**
     * @param cs the char array we are parsing
     * @param buf used to add the comments contents (out)
     * @param i the # position
     * @return the end of the comments position (end of document or new line char)
     */
    public static int eatComments(char[] cs, StringBuffer buf, int i) {
        while(i < cs.length && cs[i] != '\n' && cs[i] != '\r'){
            buf.append(cs[i]);
            i++;
        }
        if(i < cs.length)
            buf.append(cs[i]);
    
        return i;
    }
    
    /**
     * @param cs the char array we are parsing
     * @param buf used to add the token contents (out)
     * @param i the start of the token
     * @return the end of the token position (end of document or new line char or whitespace)
     */
    public static int eatToken(char[] cs, StringBuffer buf, int i) {
        while(i < cs.length && Character.isSpace(cs[i])){
            buf.append(cs[i]);
            i++;
        }
        if(i < cs.length)
            buf.append(cs[i]);
        
        return i;
    }

    /**
     * @param cs the char array we are parsing
     * @param buf used to add the literal contents (out)
     * @param i the ' or " position
     * @return the end of the literal position (or end of document)
     */
    public static int eatLiterals(char[] cs, StringBuffer buf, int i) {
        //ok, current pos is ' or "
        //check if we're starting a single or multiline comment...
        char curr = cs[i];
        
        if(curr != '"' && curr != '\''){
            throw new RuntimeException("Wrong location to eat literals. Expecting ' or \" ");
        }
        
        boolean multi = isMultiLiteral(cs, i, curr);
        
        int j;
        if(multi){
            j = findNextMulti(cs, i+3, curr);
        }else{
            j = findNextSingle(cs, i+1, curr);
        }
        
        for (int k = i; k < cs.length && k <= j; k++) {
            buf.append(cs[k]);
        }
        return j;
        
    }

    /**
     * @param cs the char array we are parsing
     * @param buf used to add the comments contents (out)
     * @param i the ' or " position
     * @return the end of the literal position (or end of document)
     */
    public static int eatPar(char[] cs, int i, StringBuffer buf) {
        char c = ' ';
        StringBuffer locBuf = new StringBuffer();
        
        int j = i+1;
        while(j < cs.length && (c = cs[j]) != ')'){
            
            j++;
            
            if(c == '\'' || c == '"'){ //ignore comments or multiline comments...
                j = ParsingUtils.eatLiterals( cs, locBuf, j-1)+1;
                
            }else if(c == '#'){
                j = ParsingUtils.eatComments(cs, locBuf, j-1)+1;
                
            }else if( c == '('){ //open another par.
                j = eatPar(cs, j-1, locBuf)+1;
            
            }else{

                locBuf.append(c);
            }
        }
        return j;
    }

    
    /**
     * @param cs
     * @param i
     */
    public static int findNextSingle(char[] cs, int i, char curr) {
        while(i < cs.length && cs[i] != curr){
            i++;
        }
        return i;
    }

    /**
     * @param cs
     * @param i
     */
    public static int findNextMulti(char[] cs, int i, char curr) {
        while(i+2 < cs.length){
            if (cs[i] == curr && cs[i+1] == curr && cs[i+2] == curr){
                break;
            }
            i++;
        }
        if(cs.length < i+2){
            return cs.length;
        }
        return i+2;
    }

    public static boolean isMultiLiteral(char cs[], int i, char curr){
        if(cs.length <= i + 2){
            return false;
        }
        if(cs[i+1] == curr && cs[i+2] == curr){
            return true;
        }
        return false;
    }

}
