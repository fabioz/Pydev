/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pylint;

import java.io.File;
import java.util.StringTokenizer;

import org.eclipse.core.internal.resources.Marker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.SimplePythonRunner;

/**
 * 
 * 
 *  --version             show program's version number and exit
 *  -h, --help            show this help message and exit
 *
 *  Master:
 *    lint Python modules using external checkers.
 *    --disable-all       Disable all possible checkers. This option should
 *                        precede enable-* options.
 *    --help-msg=<msg-id>
 *                        Display a help message for the given message id and
 *                        exit. This option may be a comma separated list.
 *    --zope              Initialize Zope products before starting.
 *    --cache-size=<size>
 *                        Set the cache size for astng objects.
 *    --generate-rcfile   Generate a sample configuration file according to the
 *                        current configuration. You can put other options before
 *                        this one to use them in the configuration. This option
 *                        causes the program to exit
 *    --ignore=<file>     Add <file> (may be a directory) to the black list. It
 *                        should be a base name, not a path. You may set this
 *                        option multiple times.
 *    --persistent=<y_or_n>
 *                        Pickle collected data for later comparisons.
 *
 *  Reports:
 *    Options related to messages / statistics reporting
 *    --enable-msg=<msg ids>
 *                        Enable the message with the given id. This option may be
 *                        a comma separated list or be set multiple time.
 *    --disable-msg=<msg ids>
 *                        Disable the message with the given id. This option may
 *                        be a comma separated list or be set multiple time.
 *    --disable-report=<rpt ids>
 *                        Disable the report with the given id. This option may be
 *                        a comma separated list or be set multiple time.
 *    --html              Use HTML as output format instead of text
 *    --parseable         Use a parseable text output format, so your favorite
 *                        text editor will be able to jump to the line
 *                        corresponding to a message.
 *    --reports=<y_or_n>  Tells wether to display a full report or only the
 *                        messages
 *    --files-output=<y_or_n>
 *                        Put messages in a separate file for each module /
 *                        package specified on the command line instead of
 *                        printing them on stdout. Reports (if any) will be
 *                        written in a file name "pylint_global.[txt|html]".
 *    --evaluation=<python_expression>
 *                        Python expression which should return a note less than
 *                        10 (10 is the highest note).You have access to the
 *                        variables errors, warnings, statements which respectivly
 *                        contain the number of errors / warnings messages and the
 *                        total number of statements analyzed. This is used by the
 *                        global evaluation report (R0004).
 *    --comment=<y_or_n>  Add a comment according to your evaluation note. This is
 *                        used by the global evaluation report (R0004).
 *    --include-ids=<y_or_n>
 *                        Include message's id in output
 *
 *  Basic:
 *    checks for :
 *    * doc strings
 *    * modules / classes / functions / methods / arguments / variables name
 *    * number of arguments, local variables, branchs, returns and statements
 *    in functions, methods
 *    * required module attributes
 *    * dangerous default values as arguments
 *    * redefinition of function / method / class
 *    * uses of the global statement
 *    --enable-basic=<y_or_n>
 *                        Enable / disable this checker
 *    --max-args=<int>    Maximum number of arguments for function / method
 *    --max-locals=<int>  Maximum number of locals for function / method body
 *    --max-returns=<int>
 *                        Maximum number of return / yield for function / method
 *                        body
 *    --max-branchs=<int>
 *                        Maximum number of branch for function / method body
 *    --max-statements=<int>
 *                        Maximum number of statements in function / method body
 *    --required-attributes=<attributes>
 *                        Required attributes for module, separated by a comma
 *    --no-docstring-rgx=<regexp>
 *                        Regular expression which should only match functions or
 *                        classes name which do not require a docstring
 *    --min-name-length=<int>
 *                        Minimal length for module / class / function / method /
 *                        argument / variable names
 *    --module-rgx=<regexp>
 *                        Regular expression which should only match correct
 *                        module names
 *    --class-rgx=<regexp>
 *                        Regular expression which should only match correct class
 *                        names
 *    --function-rgx=<regexp>
 *                        Regular expression which should only match correct
 *                        function names
 *    --method-rgx=<regexp>
 *                        Regular expression which should only match correct
 *                        method names
 *    --argument-rgx=<regexp>
 *                        Regular expression which should only match correct
 *                        argument names
 *    --variable-rgx=<regexp>
 *                        Regular expression which should only match correct
 *                        variable names
 *    --good-names=<names>
 *                        Good variable names which should always be accepted,
 *                        separated by a comma
 *    --bad-names=<names>
 *                        Bad variable names which should always be refused,
 *                        separated by a comma
 *    --bad-functions=<builtin function names>
 *                        List of builtins function names that should not be used,
 *                        separated by a comma
 *
 *  Classes:
 *    checks for :
 *    * methods without self as first argument
 *    * overriden methods signature
 *    * access only to existant members via self
 *    * attributes not defined in the __init__ method
 *    * supported interfaces implementation
 *    * unreachable code
 *    --enable-classes=<y_or_n>
 *                        Enable / disable this checker
 *    --ignore-iface-methods=<method names>
 *                        List of interface methods to ignore, separated by a
 *                        comma. This is used for instance to not check methods
 *                        defines in Zope's Interface base class.
 *    --ignore-mixin-members=<y_or_n>
 *                        Tells wether missing members accessed in mixin class
 *                        should be ignored. A mixin class is detected if its name
 *                        ends with "mixin" (case insensitive).
 *
 *  Exceptions:
 *    checks for
 *    * excepts without exception filter
 *    * string exceptions
 *    --enable-exceptions=<y_or_n>
 *                        Enable / disable this checker
 *
 *  Format:
 *    checks for :
 *    * unauthorized constructions
 *    * strict indentation
 *    * line length
 *    * use of <>
 *    --enable-format=<y_or_n>
 *                        Enable / disable this checker
 *    --max-line-length=<int>
 *                        Maximum number of characters on a single line.
 *    --max-module-lines=<int>
 *                        Maximum number of lines in a module
 *    --indent-string=<string>
 *                        String used as indentation unit. This is usually "    "
 *                        (4 spaces) or "\t" (1 tab).
 *
 *  Imports:
 *    checks for
 *    * external modules dependancies
 *    * relative / wildcard imports
 *    * cyclic imports
 *    * uses of deprecated modules
 *    --enable-imports=<y_or_n>
 *                        Enable / disable this checker
 *    --deprecated-modules=<modules>
 *                        Deprecated modules which should not be used, separated
 *                        by a comma
 *
 *  Miscellaneous:
 *    checks for:
 *    * source code with non ascii characters but no encoding declaration (PEP
 *    263)
 *    * warning notes in the code like FIXME, XXX
 *    --enable-miscellaneous=<y_or_n>
 *                        Enable / disable this checker
 *    --notes=<comma separated values>
 *                        List of note tags to take in consideration, separated by
 *                        a comma. Default to FIXME, XXX, TODO
 *
 *  Metrics:
 *    does not check anything but gives some raw metrics :
 *    * total number of lines
 *    * total number of code lines
 *    * total number of docstring lines
 *    * total number of comments lines
 *    * total number of empty lines
 *    --enable-metrics=<y_or_n>
 *                        Enable / disable this checker
 *
 *  Variables:
 *    checks for
 *    * unused variables / imports
 *    * undefined variables
 *    * redefinition of variable from builtins or from an outer scope
 *    * use of variable before assigment
 *    --enable-variables=<y_or_n>
 *                        Enable / disable this checker
 *    --init-import=<y_or_n>
 *                        Tells wether we should check for unused import in
 *                        __init__ files.
 *
 *  Environment variables:
 *     The following environment variables are used :
 *    * PYLINTHOME
 *    path to the directory where data of persistent run will be stored. If
 *    not found, it defaults to ~/.pylint.d/ or .pylint.d (in the current
 *    working directory) . The current PYLINTHOME is C:\Documents and
 *    Settings\fabioz\.pylint.d.                         * PYLINTRC
 *    path to the configuration file. If not found, it will use the first
 *    existant file in ~/.pylintrc, /etc/pylintrc. The current PYLINTRC is
 *    None .
 *    * PYLINT_IMPORT
 *    this variable is set by pylint since some packages may want to known
 *    when they are imported by pylint.
 *
 *  Output:
 *     Using the default text output, the message format is :
 *    MESSAGE_TYPE: LINE_NUM:[OBJECT:] MESSAGE
 *    There are 3 kind of message types :
 *    * (W) warning and (E) error
 *    these message types are used to distinguish the gravity of the detected
 *    problem.
 *    * (F) fatal
 *    an error occured which prevented pylint from doing further processing.
 * @author Fabio Zadrozny
 */
public class PyLintVisitor  extends PyDevBuilderVisitor {

    /* (non-Javadoc)
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitResource(org.eclipse.core.resources.IResource)
     */
    public static final String PYLINT_PROBLEM_MARKER = "org.python.pydev.pylintproblemmarker";
    public boolean visitResource(IResource resource) {
        IProject project = resource.getProject();
        if (project != null && resource instanceof IFile) {
            System.out.println("pylint visiting = "+ resource);
            IFile file = (IFile) resource;
            IPath location = PydevPlugin.getLocation(file.getFullPath());
            
            File script;
            try {
                script = PydevPlugin.getScriptWithinPySrc("ThirdParty/logilab/pylint/lint.py");
	            File arg = new File(location.toOSString());
	            System.out.println("pylint executing ... ");
	            String lintargs = " --include-ids=y ";
	            lintargs += PyLintPrefPage.getPylintArgs();
	            lintargs += " ";
	            
	            String output = SimplePythonRunner.runAndGetOutput(script.getAbsolutePath(), lintargs+arg.getAbsolutePath(), script.getParentFile());
	            System.out.println("pylint ended ");
	            System.out.println("output = "+ output);
	            StringTokenizer tokenizer = new StringTokenizer(output, "\r\n");
	            
	            boolean useW = PyLintPrefPage.useWarnings();
	            boolean useE = PyLintPrefPage.useErrors();
	            boolean useF = PyLintPrefPage.useFatal();
	            boolean useC = PyLintPrefPage.useCodingStandard();
	            boolean useR = PyLintPrefPage.useRefactorTips();
	            
	            resource.deleteMarkers(PYLINT_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
	            
	            while(tokenizer.hasMoreTokens()){
	                String tok = tokenizer.nextToken();
	                
	                try {
                        String type = null;
                        int priority = 0;
                        
                        //W0611:  3: Unused import finalize
                        //F0001:  0: Unable to load module test.test2 (list index out of range)
                        //C0321: 25:fdfd: More than one statement on a single line
                        if(tok.startsWith("C")&& useC && tok.indexOf(":") != -1){
                            type = PYLINT_PROBLEM_MARKER;
                            priority = Marker.SEVERITY_WARNING;
                        }
                        else if(tok.startsWith("R")  && useR && tok.indexOf(":") != -1){
                            type = PYLINT_PROBLEM_MARKER;
                            priority = Marker.SEVERITY_WARNING;
                        }
                        else if(tok.startsWith("W")  && useW && tok.indexOf(":") != -1){
                            type = PYLINT_PROBLEM_MARKER;
                            priority = Marker.SEVERITY_WARNING;
                        }
                        else if(tok.startsWith("E") && useE && tok.indexOf(":") != -1){
                            type = PYLINT_PROBLEM_MARKER;
                            priority = Marker.SEVERITY_ERROR;
                        }
                        else if(tok.startsWith("F") && useF && tok.indexOf(":") != -1){
                            type = PYLINT_PROBLEM_MARKER;
                            priority = Marker.SEVERITY_ERROR;
                        }
                        
                        String initial = tok;
                        try {
                            if(type != null){
                                String id = tok.substring(0, tok.indexOf(":")).trim();
                                
                                tok = tok.substring(tok.indexOf(":")+1);
                                int line = Integer.parseInt(tok.substring(0, tok.indexOf(":")).trim() );
                                tok = tok.substring(tok.indexOf(":")+1);
                                createMarker(resource, "ID:"+id+" "+tok , line,  type, priority);
                            }
                        } catch (RuntimeException e2) {
                            System.out.println("ERROR - "+initial);
                            e2.printStackTrace();
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
	            }
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }
        }
        return true;
    }

}
