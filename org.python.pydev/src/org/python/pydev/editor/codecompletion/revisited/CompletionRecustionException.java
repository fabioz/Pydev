/*
 * Created on Mar 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

/**
 * @author Fabio Zadrozny
 */
public class CompletionRecustionException extends RuntimeException {

    /**
     * @param string
     */
    public CompletionRecustionException(String string) {
        super(string);
    }

}
