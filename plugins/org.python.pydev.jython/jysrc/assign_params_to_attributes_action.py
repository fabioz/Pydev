"""Assign Params to Attributes by Joel Hedlund <joel.hedlund at gmail.com>. 

Changed:Fabio Zadrozny (binded to Ctrl+1 too)
"""

__version__ = "1.0.1"

__copyright__ = '''Available under the same conditions as PyDev.

See PyDev license for details.
http://pydev.sourceforge.net
'''

#=======================================================================================================================
# ScriptUnapplicableError
#=======================================================================================================================
class ScriptUnapplicableError(Exception):
    """Raised when the script is unapplicable to the current line."""

    def __init__(self, msg):
        self.msg = msg

    def __str__(self):
        return self.msg


#=======================================================================================================================
# AssignToAttribsOfSelf
#=======================================================================================================================
class AssignToAttribsOfSelf:
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
    def __init__(self, editor=None):
        self.editor = editor


    def isScriptApplicable(self, ps, showError=True):
        '''Raise ScriptUnapplicableError if the script is unapplicable.
        
        @param ps: The current ps as a PySelection.
        '''
        import re
        _rDef = re.compile(r'^\s+def\s')
        try:
            sCurrentLine = ps.getCursorLineContents()
            if not _rDef.match(sCurrentLine):
                msg = "The current line is not the first line of a method def statement."
                raise ScriptUnapplicableError(msg)

            oParamInfo = ps.getInsideParentesisToks(True)

            if not oParamInfo:
                msg = "The parameter list does not start on the first line of the method def statement."
                raise ScriptUnapplicableError(msg)
            lsParams = list(oParamInfo.o1)

            if not lsParams or lsParams[0] != 'self':
                msg = "The parameter list does not start with self."
                raise ScriptUnapplicableError(msg)

            # Workaround for bug in PySelection.getInsideParentesisToks()
            # in pydev < 1.0.6. In earlier versions, this can happen
            # with legal def lines such as "def moo(self, ):"
            if '' in lsParams:
                lsParams.remove('')

            if not len(lsParams) > 1:
                msg = "The method has no parameters other than self."
                raise ScriptUnapplicableError(msg)

            return True

        except ScriptUnapplicableError, e:
            if showError:
                sTitle = "Script Unapplicable"
                sHeader = "Script: Assign Method Parameters to Attributes of self"
                sBody = "The script cannot be run due to the following error:"
                sDialogText = ps.getEndLineDelim().join([sHeader, '', sBody, str(e)])
                self.editor.showInformationDialog(sTitle, sDialogText)

        return False


    def _assignmentLines(self, endLineDelimiter, params, indent, contentsWithExistingAssigns):
        '''Assemble the python code lines for the assignments.
        
        @param params: The method parameters as a list of str, must 
                       start with 'self'.
        @param indent: The indentation of the assignment lines as a str.
        '''
        sTempl = "self.%(name)s = %(name)s"
        ls = []
        found_assignments = []
        for s in params[1:]:
            assign = sTempl % {'name':s.split('*')[-1]}
            if assign not in contentsWithExistingAssigns:
                ls.append(indent + assign)
            else:
                found_assignments.append(assign)
        return endLineDelimiter.join(ls), found_assignments


    def run(self):
        #gotten here (and not in the class resolution as before) because we want it to be resolved
        #when we execute it, and not when setting it
        ps = self.editor.createPySelection()
        oDocument = ps.getDoc()

        if not self.isScriptApplicable(ps):
            return None

        oParamInfo = ps.getInsideParentesisToks(True)
        lsParams = list(oParamInfo.o1)

        # Determine insert point:
        iClosingParOffset = oParamInfo.o2
        iClosingParLine = ps.getLineOfOffset(iClosingParOffset)
        iInsertAfterLine = iClosingParLine
        currentIndent = ps.getIndentationFromLine()

        sIndent = currentIndent + self.editor.getIndentPrefs().getIndentationString()

        from org.python.pydev.core.docutils import ParsingUtils #@UnresolvedImport
        parsingUtils = ParsingUtils.create(oDocument)

        # Is there a docstring? In that case we need to skip past it.
        sDocstrFirstLine = ps.getLine(iClosingParLine + 1)
        sDocstrStart = sDocstrFirstLine.strip()[:2]

        if sDocstrStart and (sDocstrStart[0] in ['"', "'"]
                             or sDocstrStart in ['r"', "r'"]):
            iDocstrLine = iClosingParLine + 1
            iDocstrLineOffset = ps.getLineOffset(iDocstrLine)
            li = [sDocstrFirstLine.find(s) for s in ['"', "'"]]
            iDocstrStartCol = min([i for i in li if i >= 0])
            iDocstrStart = iDocstrLineOffset + iDocstrStartCol
            iDocstrEnd = parsingUtils.eatLiterals(None, iDocstrStart)
            iInsertAfterLine = ps.getLineOfOffset(iDocstrEnd)
            sIndent = ps.getIndentationFromLine(sDocstrFirstLine)

        # Workaround for bug in PySelection.addLine() in
        # pydev < v1.0.6. Inserting at the last line in the file
        # would raise an exception if the line wasn't newline
        # terminated.
        iDocLength = oDocument.getLength()
        iLastLine = ps.getLineOfOffset(iDocLength)
        sLastChar = str(parsingUtils.charAt(iDocLength - 1))
        endLineDelimiter = ps.getEndLineDelim()

        if iInsertAfterLine == iLastLine and not endLineDelimiter.endswith(sLastChar):
            oDocument.replace(iDocLength, 0, endLineDelimiter)

        line = ps.getLine(iInsertAfterLine + 1)
        if line.strip() == 'pass':
            ps.deleteLine(iInsertAfterLine + 1)
            
        next_starting_scope = ps.getNextLineThatStartsScope(
            ps.CLASS_AND_FUNC_TOKENS, iInsertAfterLine + 1, 99999)
        if next_starting_scope is None:
            search_until = iLastLine
        else:
            search_until = next_starting_scope.iLineStartingScope

        contents = ps.getContentsFromLineRange(iInsertAfterLine + 1, search_until)        

        # Assemble assignment lines and insert them into the document:
        sAssignments, found_assignments = self._assignmentLines(endLineDelimiter, lsParams, sIndent, contents)
        if sAssignments:
            line_to_skip = 0
            if found_assignments:
                for i, line in enumerate(contents.splitlines()):
                    line = line.strip()
                    if not line:
                        continue
                    
                    found = False
                    for f in found_assignments:
                        if f in line:
                            found = True
                            line_to_skip = i + 1
                            break
                    
                    if not found:
                        break
                    
            iInsertAfterLine += line_to_skip
            ps.addLine(sAssignments, iInsertAfterLine)
    
            # Leave cursor at the last char of the new lines.
            iNewOffset = ps.getLineOffset(iInsertAfterLine + 1) + len(sAssignments)
            self.editor.setSelection(iNewOffset, 0)
