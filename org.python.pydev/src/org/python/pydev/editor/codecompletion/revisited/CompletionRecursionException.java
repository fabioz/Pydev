/*
 * Created on Mar 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

/**
 * @author Fabio Zadrozny
 */
public class CompletionRecursionException extends RuntimeException {

    /**
     * @param string
     */
    public CompletionRecursionException(String string) {
        super(string);
    }

}
