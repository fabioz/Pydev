/*
 * Created on 28/07/2005
 */
package org.python.pydev.core;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * iterates through a string so that parts of it are gotten each time in a progressive way based on dots
 * 
 * (e.g.: a.b.c
 * 
 * will return
 * 
 * a
 * a.b
 * a.b.c
 * 
 * @author Fabio
 */
public class FullRepIterable implements Iterable<String>{

    private static final class ReverseFullRepIterator implements Iterator<String> {

        private String fullRep;

        public ReverseFullRepIterator(String fullRep) {
            this.fullRep = fullRep;
        }

        public boolean hasNext() {
            return fullRep.length() > 0;
        }

        public String next() {
            if(fullRep.length() == 0){
                throw new RuntimeException("no more items");
            }
            String ret = fullRep;
            int l = fullRep.lastIndexOf('.');
            if(l == -1){
                fullRep = "";
            }else{
                fullRep = fullRep.substring(0, l);
            }
            return ret;
        }

        public void remove() {
            throw new RuntimeException("Not supported");
        }
        
    }
    
    private static final class FullRepIterator implements Iterator<String> {
        private int i = -1;
        private boolean lastStep; //even if there is no point, we should return the last string
        private String fullRep;
        
        public FullRepIterator(String fullRep) {
            this.fullRep = fullRep;
            lastStep = false;
        }

        public boolean hasNext() {
            boolean ret = !lastStep || fullRep.indexOf('.', i) != -1;
            return ret;
        }

        public String next() {
            int j = fullRep.indexOf('.', i);
            if(j == -1){
                lastStep = true;
                return fullRep;
            }
            i = j+1;
            return fullRep.substring(0,j);
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }
    }

    
    
    private String fullRep;
    private boolean reverse;

    public FullRepIterable(String fullRep) {
        this(fullRep, false);
    }

    /**
     * User is able to specify reverse mode.
     * 
     * If it is the standard mode, 'aa.bb' would be 'aa', 'aa.bb'.
     * 
     * In reverse mode 'aa.bb' would be 'aa.bb', 'aa' 
     * 
     * @param fullRep The dotted string we want to gather in parts
     * @param reverse whether we want to get it in reverse order
     */
    public FullRepIterable(String fullRep, boolean reverse) {
        this.fullRep = fullRep;
        this.reverse = reverse;
    }

    public Iterator<String> iterator() {
        if(!reverse){
            return new FullRepIterator(this.fullRep);
        }else{
            return new ReverseFullRepIterator(this.fullRep);
        }
    }
    
    public static String[] headAndTail(String fullRep){
        return headAndTail(fullRep, true);
    }
    
    public static final int HEAD = 1;
    public static final int TAIL = 0;
    
    /**
     * on string 'aa.bb.cc', the head is 'cc' and the tail is 'aa.bb'
     * 
     * head is pos 1 (cc)
     * tail is pos 0 (aa.bb)
     * 
     * if it does not have a ".", everything is part of the tail - this is a strange behaviour by the way... but it is what I need, because
     * of the way to calculate the modules on some cases.
     */
    public static String[] headAndTail(String fullRep, boolean emptyTailIfNoDot){
        int i = fullRep.lastIndexOf('.');
        if(i != -1){
            return new String[]{ 
                    fullRep.substring(0, i), 
                    fullRep.substring(i+1)
                    };
        }else{
            if(emptyTailIfNoDot){
                return new String[]{ 
                        "",
                        fullRep 
                };
            }else{
                return new String[]{ 
                        fullRep,
                        ""
                };
            }
        }
    }

    /**
     * @return the name of the parent module of the module represented by currentModuleName
     */
	public static String getParentModule(String currentModuleName) {
		return headAndTail(currentModuleName, true)[0];
	}

	/**
	 * @return All that is after the last dot (or the whole string if there is no dot)
	 */
	public static String getLastPart(String tokToCheck) {
		int i = tokToCheck.lastIndexOf('.');
		if(i == -1){
			return tokToCheck;
		}
		return tokToCheck.substring(i+1);
	}

	/**
	 * @return All that is before the first dot (or the whole string if there is no dot)
	 */
	public static String getFirstPart(String tokToCheck) {
		int i = tokToCheck.indexOf('.');
		if(i == -1){
			return tokToCheck;
		}
		return tokToCheck.substring(0, i);
	}
	
	/**
	 * @return All that is before the last dot (or an empty string if there is no dot)
	 */
	public static String getWithoutLastPart(String currentModuleName) {
		int i = currentModuleName.lastIndexOf('.');
		if(i == -1){
			return "";
		}
		return currentModuleName.substring(0, i);
	}

	public static String joinFirstParts(String[] actToks) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < actToks.length-1; i++) {
			if(i > 0){
				buffer.append('.');
			}
			buffer.append(actToks[i]);
		}
		return buffer.toString();
	}

	/**
	 * @return whether the foundRep contains some part with the nameToFind
	 */
	public static boolean containsPart(String foundRep, String nameToFind) {
		String[] strings = dotSplit(foundRep);
		for (String string : strings) {
			if(string.equals(nameToFind)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Splits the string as would string.split("\\."), but without yielding empty strings
	 */
	public static String[] dotSplit(String string) {
		ArrayList<String> ret = new ArrayList<String>();
		int len = string.length();
		
		int last = 0;
		
		char c = 0;
		
		for (int i = 0; i < len; i++) {
			c = string.charAt(i);
			if(c == '.'){
				if(last != i){
					ret.add(string.substring(last, i));
				}
				while(c == '.' && i < len-1){
					i++;
					c = string.charAt(i);
				}
				last = i;
			}
		}
		if(c != '.'){
			if(last == 0 && len > 0){
				ret.add(string); //it is equal to the original (no dots)
				
			}else if(last < len){
				ret.add(string.substring(last, len));
				
			}
		}
		return ret.toArray(new String[ret.size()]);
	}
	

}
