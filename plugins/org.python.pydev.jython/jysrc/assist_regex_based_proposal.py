"""Quick Assistant: Regex based proposals.

This module combines AssistProposal, regexes and string formatting to 
provide a way of swiftly coding your own custom Quick Assistant proposals.
These proposals are ready for instatiation and registering with 
assist_proposal.register_proposal(): AssignToAttributeOfSelf, 
AssignEmptyDictToVarIfNone, AssignEmptyDictToVarIfNone and
AssignAttributeOfSelfToVarIfNone. Using these as examples it should be 
straightforward to code your own regex driven Quick Assistant proposals.

"""
__author__ = """Joel Hedlund <joel.hedlund at gmail.com>"""

__version__ = "1.0.0"

__copyright__ = '''Available under the same conditions as PyDev.

See PyDev license for details.
http://pydev.sourceforge.net
'''

import re

from org.python.pydev.core.docutils import PySelection #@UnresolvedImport
from org.python.pydev.editor.actions import PyAction #@UnresolvedImport

import assist_proposal

class RegexBasedAssistProposal(assist_proposal.AssistProposal):
    """Base class for regex driven Quick Assist proposals.
    
    More docs available in base class source.
        
    New class data members
    ======================
    regex = re.compile(r'^(?P<initial>\s*)(?P<name>\w+)\s*$'): <regex>
        Must .match() current line for .isValid() to return true. Any named
        groups will be available in self.vars.
    template = "%(initial)sprint 'Hello World!'": <str>
        This will replace what's currently on the line on .apply(). May use
        string formatters with names from self.vars.
    base_vars = {}: <dict <str>:<str>>
        Used to initiallize self.vars.

    New instance data members
    =========================
    vars = <dict <str>:<str>>
        Variables used with self.template to produce the code that replaces
        the current line. This will contain values from self.base_vars, all
        named groups in self.regex, as well with these two additional ones:
        'indent': the static indentation string
        'newline': the line delimiter string        
    selection, current_line, editor, offset:
        Same as the corresponding args to .isValid().
    
    """
    template = ""
    base_vars = {}
    regex = re.compile(r'^(?P<initial>\s*)(?P<name>\w+)\s*$')

    def isValid(self, selection, current_line, editor, offset):
        """Is this proposal applicable to this line of code?
        
        If current_line .match():es against self.regex then we will store
        a lot of information on the match and environment, and return True.
        Otherwise return False.
        
        IN:
        pyselection: <PySelection>
            The current selection. Highly useful.
        current_line: <str>
            The text on the current line.
        editor: <PyEdit>
            The current editor.
        offset: <int>
            The current position in the editor.

        OUT:
        Boolean. Is the proposal applicable in the current situation?
        
        """
        m = self.regex.match(current_line)
        if not m:
            return False
        self.vars = {'indent': editor.getIndentPrefs().getIndentationString()}
        self.vars.update(self.base_vars)
        self.vars.update(m.groupdict())
        self.selection = selection
        self.current_line = current_line
        self.editor = editor
        self.offset = offset
        return True

    def apply(self, document):
        """Replace the current line with the populated template.
        
        IN:
        document: <IDocument>
            The edited document.
        
        OUT:
        None.

        """
        self.vars['newline'] = PyAction.getDelimiter(document)
        sNewCode = self.template % self.vars
        
        # Move to insert point:
        iStartLineOffset = self.selection.getLineOffset()
        iEndLineOffset = iStartLineOffset + len(self.current_line)
        self.editor.setSelection(iEndLineOffset, 0)
        self.selection = PySelection(self.editor)
        
        # Replace the old code with the new assignment expression:
        self.selection.replaceLineContentsToSelection(sNewCode)
        
        #mark the value so that the user can change it
        selection = PySelection(self.editor)
        absoluteCursorOffset = selection.getAbsoluteCursorOffset()
        val = self.vars['value']
        self.editor.selectAndReveal(absoluteCursorOffset-len(val),len(val))

class AssignToAttributeOfSelf(RegexBasedAssistProposal):
    """Assign variable to attribute of self.
    
    Effect
    ======
    Generates code that assigns a variable to attribute of self with the 
    same name.
    
    Valid when
    ==========
    When the current line contains exactly one alphanumeric word. No check
    is performed to see if the word is defined or valid in any other way. 

    Use case
    ========
    It's often a good idea to use the same names in args, variables and 
    data members. This keeps the terminology consistent. This way 
    customer_id should always contain a customer id, and any other 
    variants are misspellings that probably will lead to bugs. This 
    proposal helps you do this by assigning variables to data members with 
    the same name.

    """
    description = "Assign to attribute of self"
    tag = "ASSIGN_VARIABLE_TO_ATTRIBUTE_OF_SELF"
    regex = re.compile(r'^(?P<initial> {8}\s*)(?P<name>\w+)\s*$')
    template = "%(initial)sself.%(name)s = %(name)s"
    
class AssignDefaultToVarIfNone(RegexBasedAssistProposal):
    """Assign default value to variable if None.
    
    This is a base class intended for subclassing.
    
    Effect
    ======
    Generates code that tests if a variable is none, and if so, assigns a 
    default value to it.
    
    Valid when
    ==========
    When the current line contains exactly one alphanumeric word. No check
    is performed to see if the word is defined or valid in any other way. 
    
    Use case
    ========
    It's generally a bad idea to use mutable objects as default values to 
    methods and functions. The common way around it is to use None as the 
    default value, check the arg in the fuction body, and then assign 
    the desired mutable to it. This proposal does the check/assignment for
    you. You only need to type the arg name where you want the check, and 
    then activate the Quick Assistant.

    """
    description = "Assign default value to var if None"
    tag = "ASSIGN_DEFAULT_VALUE_TO_VARIABLE_IF_NONE"
    regex = re.compile(r'^(?P<initial>\s*)(?P<name>\w+)\s*$')
    template = ("%(initial)sif %(name)s is None:%(newline)s"
                "%(initial)s%(indent)s%(name)s = %(value)s")
    base_vars = {'value': "[]"}
    
class AssignValueToVarIfNone(AssignDefaultToVarIfNone):
    """Assign value to variable if None."""
    description = "Assign value to var if None"
    tag = "ASSIGN_VALUE_TO_VARIABLE_IF_NONE"

class AssignEmptyListToVarIfNone(AssignDefaultToVarIfNone):
    """Assign empty list to variable if None."""
    description = "Assign empty list to var if None"
    tag = "ASSIGN_EMPTY_LIST_TO_VARIABLE_IF_NONE"

class AssignEmptyDictToVarIfNone(AssignEmptyListToVarIfNone):
    """Assign empty dictionary to variable if None."""
    description = "Assign empty dict to var if None"
    tag = "ASSIGN_EMPTY_DICT_TO_VARIABLE_IF_NONE"
    base_vars = {'value': "dict()"}

class AssignAttributeOfSelfToVarIfNone(AssignDefaultToVarIfNone):
    """Assign an attribute of self with same name to variable if None.

    Valid when
    ==========
    When the current line contains exactly one alphanumeric word indented 
    by more than 8 spaces. This script does not check if the word is 
    defined or valid in any other way. 

    Use case
    ========
    If a method does something using a data member, but just as well could do 
    the same thing using an argument, it's generally a good idea to let the 
    implementation reflect that. This makes the code more flexible. This is 
    usually done like so:
    --------------------------
    class MyClass:
        def func(arg = None):
            if arg is None:
                arg = self.arg
            ...
    --------------------------
    
    This proposal does the check/assignment for you. You only need to type the 
    arg name where you want the check, and then activate the Quick Assistant.
    
    """
    description = "Assign attribute of self to var if None"
    tag = "ASSIGN_ATTRIBUTE_OF_SELF_TO_VARIABLE_IF_NONE"
    regex = re.compile(r'^(?P<initial> {8}\s*)(?P<name>\w+)\s*$')
    template = ("%(initial)sif %(name)s is None:%(newline)s"
                "%(initial)s%(indent)s%(name)s = self.%(name)s")
    