/*
 * Created on Oct 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pychecker;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.python.pydev.builder.PydevMarkerUtils;
import org.python.pydev.runners.SimplePythonRunner;

/**
 * 
 *   Options:           Change warning for ... [default value]
 * 
 * Major Options:
 *   -e, --errors       turn off all warnings which are not likely errors
 *       --complexity   turn off all warnings which are related to complexity
 *   -F, --config       specify .pycheckrc file to use
 *       --quixote      support Quixote's PTL modules
 * 
 * Error Control:
 *   -i, --import       unused imports [on]
 *   -k, --pkgimport    unused imports from __init__.py [on]
 *   -M, --reimportself module imports itself [on]
 *   -X, --reimport     reimporting a module [on]
 *   -x, --miximport    module does import and from ... import [on]
 *   -l, --local        unused local variables, except tuples [on]
 *   -t, --tuple        all unused local variables, including tuples [off]
 *   -9, --members      all unused class data members [off]
 *   -v, --var          all unused module variables [off]
 *   -p, --privatevar   unused private module variables [on]
 *   -g, --allglobals   report each occurrence of global warnings [off]
 *   -n, --namedargs    functions called with named arguments (like keywords) [off]
 *   -a, --initattr     Attributes (members) must be defined in __init__() [off]
 *   -I, --initsubclass Subclass.__init__() not defined [off]
 *   -u, --callinit     Baseclass.__init__() not called [on]
 *   -0, --abstract     Subclass needs to override methods that only throw exceptions [on]
 *   -N, --initreturn   Return None from __init__() [on]
 *   -8, --unreachable  unreachable code [off]
 *   -2, --constCond    a constant is used in a conditional statement [on]
 *   -1, --constant1    1 is used in a conditional statement (if 1: or while 1:) [off]
 *       --stringiter   check if iterating over a string [on]
 *       --stringfind   check improper use of string.find() [on]
 *   -A, --callattr     Calling data members as functions [off]
 *   -y, --classattr    class attribute does not exist [on]
 *   -S, --self         First argument to methods [self]
 *       --classmethodargs First argument to classmethods [['cls', 'klass']]
 *   -T, --argsused     unused method/function arguments [on]
 *   -z, --varargsused  unused method/function variable arguments [on]
 *   -G, --selfused     ignore if self is unused in methods [off]
 *   -o, --override     check if overridden methods have the same signature [on]
 *       --special      check if __special__ methods exist and have the correct signature [on]
 *   -U, --reuseattr    check if function/class/method names are reused [on]
 *   -Y, --positive     check if using unary positive (+) which is usually meaningless [on]
 *   -j, --moddefvalue  check if modify (call method) on a parameter that has a default value [on]
 *       --changetypes  check if variables are set to different types [off]
 *       --unpack       check if unpacking a non-sequence [on]
 *       --unpacklen    check if unpacking sequence with the wrong length [on]
 *       --badexcept    check if raising or catching bad exceptions [on]
 *   -4, --noeffect     check if statement appears to have no effect [on]
 *       --modulo1      check if using (expr % 1), it has no effect on integers and strings [on]
 *       --isliteral    check if using (expr is const-literal), doesn't always work on integers and strings [on]
 * 
 * Possible Errors:
 *   -r, --returnvalues check consistent return values [on]
 *   -C, --implicitreturns check if using implict and explicit return values [on]
 *   -O, --objattrs     check that attributes of objects exist [on]
 *   -7, --slots        various warnings about incorrect usage of __slots__ [on]
 *   -3, --properties   using properties with classic classes [on]
 *       --emptyslots   check if __slots__ is empty [on]
 *   -D, --intdivide    check if using integer division [on]
 *   -w, --shadow       check if local variable shadows a global [on]
 *   -s, --shadowbuiltin check if a variable shadows a builtin [on]
 * 
 * Security:
 *       --input        check if input() is used [on]
 *   -6, --exec         check if the exec statement is used [off]
 * 
 * Suppressions:
 *   -q, --stdlib       ignore warnings from files under standard library [off]
 *   -b, --blacklist    ignore warnings from the list of modules
 *                          [['Tkinter', 'wxPython', 'gtk', 'GTK', 'GDK']]
 *   -Z, --varlist      ignore global variables not used if name is one of these values
 *                          [['__version__', '__warningregistry__', '__all__', '__credits__', '__test__', '__author__', '__email__', '__revision__']]
 *   -E, --unusednames  ignore unused locals/arguments if name is one of these values [['_', 'empty', 'unused', 'dummy']]
 *       --deprecated   ignore use of deprecated modules/functions [on]
 * 
 * Complexity:
 *   -L, --maxlines     maximum lines in a function [200]
 *   -B, --maxbranches  maximum branches in a function [50]
 *   -R, --maxreturns   maximum returns in a function [10]
 *   -J, --maxargs      maximum # of arguments to a function [10]
 *   -K, --maxlocals    maximum # of locals in a function [40]
 *   -5, --maxrefs      maximum # of identifier references (Law of Demeter) [5]
 *   -m, --moduledoc    no module doc strings [off]
 *   -c, --classdoc     no class doc strings [off]
 *   -f, --funcdoc      no function/method doc strings [off]
 * 
 * Debug:
 *       --rcfile       print a .pycheckrc file generated from command line args
 *   -P, --printparse   print internal checker parse structures [off]
 *   -d, --debug        turn on debugging for checker [off]
 *   -Q, --quiet        turn off all output except warnings
 *   -V, --version      print the version of PyChecker and exit
 *   
 * @author Fabio Zadrozny
 */
public class PyCheckerLauncher {

    /**
     * Pychecks a list of resources.
     * 
     * @param resources
     */
    public static void pycheck(List resources) {
        StringBuffer allFiles = new StringBuffer();
        for (Iterator it = resources.iterator(); it.hasNext();) {
            IResource element = (IResource) it.next();
            allFiles.append(element.getLocation().toOSString());
            allFiles.append(" ");
        }
        pycheck(resources, allFiles.toString());
    }

    /**
     * Launches pychecker for the given resource.
     * 
     * @param resource: the resource for which pychecker will be launched
     * @return success of pychecker execution
     */
    public static void pycheck(IResource resource) {

        String resourceLocation = resource.getLocation().toOSString();

        List l = new ArrayList();
        l.add(resource);
        pycheck(l, resourceLocation);
    }

    /**
     * @param l: list of resources to be checked.
     * 
     * @param resourceLocation:
     */
    private static void pycheck(List l, String resourceLocation) {
        String pycheckerLocation = PyCheckerPrefPage.getPyCheckerLocation();
        
        String contents = "";
        try {
            contents = new SimplePythonRunner().runAndGetOutput(pycheckerLocation, new String[]{resourceLocation}, new File(pycheckerLocation).getParentFile());
        } catch (RuntimeException e) {
            System.err.println("Exception during process creation of pychecker on resource: " + resourceLocation + ".");
            throw e;
        }
        
        String line = null;
        StringTokenizer tokenizer = new StringTokenizer(contents, "\n");
        while (tokenizer.hasMoreTokens()) {
            line = tokenizer.nextToken();

            for (Iterator iter = l.iterator(); iter.hasNext();) {
                parse(line, resourceLocation, (IResource) iter.next());
            }
        }
    }

    /**
     * The line being parsed comes in the format:
     * 
     * x:/t/testcase.py:23: Overridden method (__call__) doesn't match signature in class ( <class 'unittest.TestCase'>)
     * 
     * that is:
     * 
     * resource:line: Description
     * 
     * we have to be careful because in windows we have c: and in linux not.
     * 
     * @param line
     * @param resourceLocation
     * @param resource
     */
    private static void parse(String line, String resourceLocation, IResource resource) {

        if (line.startsWith(resourceLocation)) {
            line = line.substring(resourceLocation.length() + 1); /* remove path of project and first ':' */
            int index = line.indexOf(':');
            int lineNumber = Integer.parseInt(line.substring(0, index)); /* line number (chars between ':') */
            String message = line.substring(index + 1); /* warning associated to line number */
            PydevMarkerUtils.createProblemMarker(resource, message, lineNumber);
        }
    }
}