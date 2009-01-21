"""Assign Params to Attributes by Joel Hedlund <joel.hedlund at gmail.com>. 

Changed:Fabio Zadrozny (binded to Ctrl+1 too)
"""

__version__ = "1.0.1"

__copyright__ = '''Available under the same conditions as PyDev.

See PyDev license for details.
http://pydev.sourceforge.net
'''

from org.eclipse.jface.action import Action #@UnresolvedImport
import re
from org.eclipse.jface.dialogs import MessageDialog #@UnresolvedImport
from org.python.pydev.core.docutils import PySelection #@UnresolvedImport
from org.python.pydev.editor.actions import PyAction #@UnresolvedImport
from org.python.pydev.core.docutils import ParsingUtils #@UnresolvedImport
True, False = 1,0

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
    def __init__(self, editor = None):
        Action.__init__(self)
        self.editor = editor
    
    def isScriptApplicable(self, selection, showError=True):
        '''Raise ScriptUnapplicableError if the script is unapplicable.
        
        @param selection: The current selection as a PySelection.
        '''
        _rDef = re.compile(r'^\s+def\s')
        try:
            sCurrentLine = selection.getCursorLineContents()
            if not _rDef.match(sCurrentLine):
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
            if showError:
                sTitle = "Script Unapplicable"
                sHeader = "Script: Assign Method Parameters to Attributes of self"
                sBody = "The script cannot be run due to the following error:"
                sDialogText = self.getNewLineDelim().join([sHeader, '', sBody, str(e)])
                oShell = self.editor.getSite().getShell()
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
        return self.getNewLineDelim().join(ls)
        
    def getNewLineDelim(self):
        if not hasattr(self, '_sNewline'):
            self._sNewline = PyAction.getDelimiter(self.editor.getDocument())
        return self._sNewline
        
    def run(self):
        #gotten here (and not in the class resolution as before) because we want it to be resolved 
        #when we execute it, and not when setting it
        oSelection = PySelection(self.editor)            
        oDocument = self.editor.getDocument()
        if not self.isScriptApplicable(oSelection):
            return None

        oParamInfo = oSelection.getInsideParentesisToks(True)
        lsParams = list(oParamInfo.o1)

        # Determine insert point:
        iClosingParOffset = oParamInfo.o2
        iClosingParLine = oSelection.getLineOfOffset(iClosingParOffset)
        iInsertAfterLine = iClosingParLine
        sIndent = self._indent(oSelection) + PyAction.getStaticIndentationString(self.editor)
        
        parsingUtils = ParsingUtils.create(oDocument)
        
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
            iDocstrEnd = parsingUtils.eatLiterals(None, iDocstrStart)
            iInsertAfterLine = oSelection.getLineOfOffset(iDocstrEnd)
            sIndent = PySelection.getIndentationFromLine(sDocstrFirstLine)

        # Workaround for bug in PySelection.addLine() in 
        # pydev < v1.0.6. Inserting at the last line in the file
        # would raise an exception if the line wasn't newline 
        # terminated.
        iDocLength = oDocument.getLength()
        iLastLine = oSelection.getLineOfOffset(iDocLength)
        sLastChar = str(parsingUtils.charAt(iDocLength - 1))
        if (iInsertAfterLine == iLastLine
            and not self.getNewLineDelim().endswith(sLastChar)):
            oDocument.replace(iDocLength, 0, self.getNewLineDelim())
            
        # Assemble assignment lines and insert them into the document:
        sAssignments = self._assignmentLines(lsParams, sIndent)
        oSelection.addLine(sAssignments, iInsertAfterLine)
        
        # Leave cursor at the last char of the new lines.
        iNewOffset = oSelection.getLineOffset(iInsertAfterLine + 1) + len(sAssignments)
        self.editor.setSelection(iNewOffset, 0)
        del oSelection
