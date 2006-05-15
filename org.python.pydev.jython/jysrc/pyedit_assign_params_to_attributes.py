"""Assign Params to Attributes by Joel Hedlund <joel.hedlund@gmail.com>.

PyDev script for generating python code that assigns method parameter 
values to attributes of self with the same name. Activates with 'a' by 
default. Edit global constants ACTIVATION_STRING and WAIT_FOR_ENTER if this
does not suit your needs. See docs on the class AssignToAttribsOfSelf for 
more details.

Contact the author for bug reports/feature requests.
"""

__version__ = "1.0.0"

__copyright__ = """Available under the same conditions as PyDev.

See PyDev license for details.
http://pydev.sourceforge.net

"""

# Change this if the default does not suit your needs
ACTIVATION_STRING = 'a'
WAIT_FOR_ENTER = False

# For earlier Python versions
True, False = 1,0

# Set to True to force Jython script interpreter restart on save events.
# Useful for Jython PyDev script development, not useful otherwise.
DEBUG = False

# This is a magic trick that tells the PyDev Extensions editor about the 
# namespace provided for pydev scripts:
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
    
assert cmd is not None 
assert editor is not None

if DEBUG and cmd == 'onSave':
    from org.python.pydev.jython import JythonPlugin #@UnresolvedImport
    editor.pyEditScripting.interpreter = JythonPlugin.newPythonInterpreter()

if cmd == 'onCreateActions' or DEBUG and cmd == 'onSave': 
    import re
    from java.lang import StringBuffer
    from org.eclipse.jface.action import Action #@UnresolvedImport
    from org.eclipse.jface.dialogs import MessageDialog #@UnresolvedImport
    from org.python.pydev.core.docutils import PySelection #@UnresolvedImport
    from org.python.pydev.editor.actions import PyAction #@UnresolvedImport
    from org.python.pydev.core.docutils import ParsingUtils #@UnresolvedImport
    
    class ScriptUnapplicableError(Exception):
        """Raised when the script is unapplicable to the current line."""
        def __init__(self, msg):
            self.msg = msg
        def __str__(self):
            return self.msg
    
    class AssignToAttribsOfSelf(Action):
        """Assign method parameter values to attributes with same name. 
        
        Pydev script for generating code that assigns the values of 
        method parameters to attributes of self with the same name.
        
        This script must be executed at the method def line, which must 
        contain both the def keyword and the opening paranthesis of the 
        parameter list. Otherwise the script will not make any changes to 
        your code.
        
        Ex:
        def moo(self, cow, sheep=1, *pargs, **kwargs):
            '''Docstring for method.'''
        
        Executing this script at the method def line will generate four 
        lines of code while preserving the docstring, like so:

        def moo(self, cow, sheep=1, *pargs, **kwargs):
            '''Docstring for method.'''
            self.cow = cow
            self.sheep = sheep
            self.pargs = pargs
            self.kwargs = kwargs
            
        """
        _rDef = re.compile(r'^\s+def\s')
        _sNewline = PyAction.getDelimiter(editor.getDocument())
        
        def _scriptApplicable(self, selection):
            '''Raise ScriptUnapplicableError if the script is unapplicable.
            
            @param selection: The current selection as a PySelection.
            '''
            try:
                sCurrentLine = selection.getCursorLineContents()
                if not self._rDef.match(sCurrentLine):
                    msg = ("The current line is not the first line of a "
                           "method def statement.")
                    raise ScriptUnapplicableError(msg)
                oParamInfo = selection.getInsideParentesisToks(True)
                if not oParamInfo:
                    msg = ("The parameter list does not start on the "
                           "first line of the method def statement.")
                    raise ScriptUnapplicableError(msg)
                lsParams = list(oParamInfo.o1)
                if not lsParams or lsParams[0] != 'self':
                    msg = ("The parameter list does not start with self.")
                    raise ScriptUnapplicableError(msg)
                # Workaround for bug in PySelection.getInsideParentesisToks()
                # in pydev < 1.0.6. In earlier versions, this can happen 
                # with legal def lines such as "def moo(self, ):"
                if '' in lsParams:
                    lsParams.remove('')
                if not len(lsParams) > 1:
                    msg = ("The method has no parameters other than self.")
                    raise ScriptUnapplicableError(msg)
                return True
            except ScriptUnapplicableError, e:
                sTitle = "Script Unapplicable"
                sHeader = "Script: Assign Method Parameters to Attributes of self"
                sBody = "The script cannot be run due to the following error:"
                sDialogText = self._sNewline.join([sHeader, '', sBody, str(e)])
                oShell = editor.getSite().getShell()
                MessageDialog.openInformation(oShell, sTitle, sDialogText)                
            return False
        
        def _indent(self, sel):
            """Return the indent of the current line as a str.
            
            @param sel: The current selection as a PySelection.
            """
            return PySelection.getIndentationFromLine(sel.getCursorLineContents())
        
        def _assignmentLines(self, params, indent):
            '''Assemble the python code lines for the assignments.
            
            @param params: The method parameters as a list of str, must 
                           start with 'self'.
            @param indent: The indentation of the assignment lines as a str.
            '''
            sTempl = indent + "self.%(name)s = %(name)s"
            ls = [sTempl % {'name':s.split('*')[-1]} for s in params[1:]]
            return self._sNewline.join(ls)
            
        def run(self):
            oSelection = PySelection(editor)            
            oDocument = editor.getDocument()
            if not self._scriptApplicable(oSelection):
                return None

            oParamInfo = oSelection.getInsideParentesisToks(True)
            lsParams = list(oParamInfo.o1)

            # Determine insert point:
            iClosingParOffset = oParamInfo.o2
            iClosingParLine = oSelection.getLineOfOffset(iClosingParOffset)
            iInsertAfterLine = iClosingParLine
            sIndent = self._indent(oSelection) + PyAction.getStaticIndentationString(editor)
            
            # Is there a docstring? In that case we need to skip past it.
            sDocstrFirstLine = oSelection.getLine(iClosingParLine + 1)
            sDocstrStart = sDocstrFirstLine.strip()[:2]
            if sDocstrStart and (sDocstrStart[0] in ['"', "'"] 
                                 or sDocstrStart in ['r"', "r'"]):
                iDocstrLine = iClosingParLine + 1
                iDocstrLineOffset = oSelection.getLineOffset(iDocstrLine)
                li = [sDocstrFirstLine.find(s) for s in ['"', "'"]]
                iDocstrStartCol = min([i for i in li if i >= 0])
                iDocstrStart = iDocstrLineOffset + iDocstrStartCol
                oDummy = StringBuffer()
                iDocstrEnd = ParsingUtils.eatLiterals(oDocument, oDummy, iDocstrStart)
                iInsertAfterLine = oSelection.getLineOfOffset(iDocstrEnd)
                sIndent = PySelection.getIndentationFromLine(sDocstrFirstLine)

            # Workaround for bug in PySelection.addLine() in 
            # pydev < v1.0.6. Inserting at the last line in the file
            # would raise an exception if the line wasn't newline 
            # terminated.
            iDocLength = oDocument.getLength()
            iLastLine = oSelection.getLineOfOffset(iDocLength)
            sLastChar = str(ParsingUtils.charAt(oDocument, iDocLength - 1))
            if (iInsertAfterLine == iLastLine
                and not self._sNewline.endswith(sLastChar)):
                oDocument.replace(iDocLength, 0, self._sNewline)
                
            # Assemble assignment lines and insert them into the document:
            sAssignments = self._assignmentLines(lsParams, sIndent)
            oSelection.addLine(sAssignments, iInsertAfterLine)
            
            # Leave cursor at the last char of the new lines.
            iNewOffset = oSelection.getLineOffset(iInsertAfterLine + 1) + len(sAssignments)
            editor.setSelection(iNewOffset, 0)
            del oSelection
                        
    sDescription = 'Assign method params to attribs of self'
    o = AssignToAttribsOfSelf()
    editor.addOfflineActionListener(ACTIVATION_STRING, o, sDescription, WAIT_FOR_ENTER)


