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
     * import testcase            (return testcase)
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
     * on module test decorating with module
     * from x import testcase     (return test.x.testcase)
     * from x import testcase as t(return test.x.testcase)
     * import testcase            (return test.testcase)
     * 
     * if not decorating would return the same as above without 'test'
     */
//    public String getOriginalRep(boolean decorateWithModule);

    /**
     * @param baseModule this is the module base to discover from where should it be made relative 
     * 
     * @return the representation as a relative import - this means that it return the original representation
     * plus the module it was defined within without its head.
     */
    public String getAsRelativeImport(String baseModule);

    /**
     * Same as relative from "."
     */
    public String getAsAbsoluteImport();
    
    /**
     * Constant to indicate that it was not possible to know in which line the
     * token was defined.
     */
    public static final int UNDEFINED = -1;
    /**
     * @return the line where this token was defined
     */
    public int getLineDefinition();
    /**
     * @return the col where this token was defined
     */
    public int getColDefinition();
    
    /**
     * @return whether the token we have wrapped is an import
     */
    public boolean isImport();

    /**
     * @return whether the token we have wrapped is a wild import
     */
    public boolean isWildImport();
    
    /**
     * @return the original representation (useful for imports)
     * e.g.: if it was import coilib.Exceptions as Exceptions, would return coilib.Exceptions
     */
    public String getOriginalRep();

    /**
     * @return the original representation without the actual representation (useful for imports, because
     * we have to look within __init__ to check if the token is defined before trying to gather modules, if
     * we have a name clash).
     * 
     * e.g.: if it was import from coilib.test import Exceptions, it would return coilib.test
     */
	public String getOriginalWithoutRep();
}
