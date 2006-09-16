/*
 * Created on Sep 16, 2006
 * @author Fabio
 */
package org.python.pydev.parser;

import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;

public interface IGrammar {

    void enable_tracing();

    SimpleNode file_input() throws ParseException;

}
