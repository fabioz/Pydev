/*
 * Created on Mar 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core.structure;

/**
 * @author Fabio Zadrozny
 */
public class CompletionRecursionException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 4134633236965099432L;

    /**
     * @param string
     */
    public CompletionRecursionException(String string) {
        super(string);
    }

}
