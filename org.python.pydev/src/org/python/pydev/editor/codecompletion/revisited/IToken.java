/*
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.Serializable;

/**
 * @author Fabio Zadrozny
 */
public interface IToken extends Serializable, Comparable{

    /**
     * 
     * @return the representation of this token.
     * 
     * The following cases apply for imports:
     * 
     * from x import testcase     (return testcase)
     * from x import testcase as t(return t)
     */
    public String getRepresentation();
    public String getDocStr();
    public int getType();
    public String getArgs();
    public String getParentPackage();
    
    /**
     * 
     * @return The complete path for the token.
     * 
     * The following cases apply for imports:
     * 
     * from x import testcase     (return testcase)
     * from x import testcase as t(return x.testcase)
     */
    public String getCompletePath();
}
