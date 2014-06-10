"""Convenience module for scripting PyDev Quick Assist proposals in Jyton.

USAGE
=====
Create pyedit_*.py file in your jython script dir of choice, import this 
module, subclass AssistProposal, instantiate it and register the instance 
with Pydev. 

Example:
-------------------------------------------------------------
from assist_proposal import AssistProposal, register_proposal

class MyProposal(AssistProposal):
    implementation_goes_here

register_proposal(MyProposal())
-------------------------------------------------------------

The cmd variable is provided automatically by pydev and will be a string 
such as 'onSave' or 'onCreateActions' etc...

See docs in source for further details.

"""

__author__ = """Joel Hedlund <joel.hedlund at gmail.com>

Some ideas borrowed from Fabio Zadrozny. These cases are explicitly noted 
in the relevant code docs.

"""

__version__ = "1.0.0"

__copyright__ = """Available under the same conditions as PyDev.

See PyDev license for details.
http://pydev.sourceforge.net
"""

from org.python.pydev.editor.correctionassist.heuristics import IAssistProps #@UnresolvedImport

class AssistProposal:
    """Convenience class for adding assist proposals to pydev.
    
    This class does nothing useful. Subclasses should assign proper values
    to data members and provide sane implementations for methods.
    
    Class data members
    ==================
    description: <str>
        The text displayed to the user in the quick assist menu (Ctrl-1).
    tag: <str>
        Unique descriptive identifier for the assist.
        
    """
    description = "Remember to change this description"
    tag = "REMEMBER_TO_CHANGE_THIS_TAG"

    def isValid(self, selection, current_line, editor, offset):
        """Return True if the proposal is applicable, False otherwise.
        
        This method should provide the same interface as the method with 
        the same name in IAssistProps.
        
        If desirable, subclasses may store the isValid args as instance 
        data members for use with .apply().
        
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
        return False

    def apply(self, document):
        """Do what the assist is supposed to do when activated.
        
        This method should provide the same interface as the method with 
        same name in PyCompletionProposal.

        See also docs for the .isValid() method. You might like to use data
        from there.
        
        IN:
        document: <IDocument>
            The edited document.
        
        OUT:
        None.
            
        """

def register_proposal(proposal, debug=False):
    """Register the proposal with the quick assistant.

    IN:
    proposal: <AssistantProposal>
        The object that holds all relevant information and does all the 
        necessary work for the proposal.
    force = False: <bool>
        If False (default), we will not attempt to re-register the assist 
        proposal if an assist proposal with the same tag is already 
        registered. If True, then we will override the registered proposal
        with our own. This is mainly useful for debugging.

    OUT:
    None.
    
    """
    from org.python.pydev.editor.correctionassist import PythonCorrectionProcessor #@UnresolvedImport
    bTagInUse = PythonCorrectionProcessor.hasAdditionalAssist(proposal.tag)
    if debug or not bTagInUse:
        oInterface = AssistantInterface(proposal)
        PythonCorrectionProcessor.addAdditionalAssist(proposal.tag, oInterface)


class AssistantInterface(IAssistProps):
    """Assistant interface wrapper for AssistProposal instances.
    
    The Quick Assistant will ask this class if we can apply the proposal, 
    and if so, which properties does it have?
    
    Adapted from Fabio Zadroznys AssistAssignParamsToAttributes class in 
    assign_params_to_attributes_assist.py.

    Instance data members
    =====================
    proposal: <AssistantProposal>
        The object that holds all relevant information and does all the 
        necessary work for the proposal.

    """

    def __init__(self, proposal, *args):
        """A new Assistant Interface.
        
        IN:
        proposal: <AssistantProposal>
        
        """
        self.proposal = proposal

    def getImage(self, imageCache, c):
        if imageCache is not None:
            return imageCache.get(c)
        return None

    def isValid(self, ps, sel, editor, offset):
        """java: boolean isValid(PySelection ps, String sel, PyEdit edit, int offset);
        """
        return self.proposal.isValid(ps, sel, editor, offset)

    def getProps(self, ps, imageCache, f, nature, editor, offset):
        '''java: List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, 
                                                    IPythonNature nature, PyEdit edit, int offset) 
        '''
        from java.util import ArrayList #@UnresolvedImport
        IPyCompletionProposal = editor.getIPyCompletionProposalClass() #@UnresolvedImport
        PyCompletionProposal = editor.getPyCompletionProposalClass() #@UnresolvedImport
        UIConstants = editor.getUIConstantsClass() #@UnresolvedImport

        class Prop(PyCompletionProposal):
            """This is the proposal that Ctrl+1 will require.
            
            Adapted from Fabio Zadroznys Prop class in 
            assign_params_to_attributes_assist.py.
            
            Instance data members
            =====================
            proposal: <AssistantProposal>
                The object that holds all relevant information and does all the 
                necessary work for the proposal.
        
            """

            def __init__(self, proposal, *args):
                PyCompletionProposal.__init__(self, *args)
                self.proposal = proposal

            def apply(self, document):
                """java: public void apply(IDocument document)
                """
                self.proposal.apply(document)

            def getSelection(self, document):
                return None

        oProp = Prop(self.proposal,
                     '', 0, 0, 0,
                     self.getImage(imageCache, UIConstants.ASSIST_DOCSTRING),
                     self.proposal.description,
                     None, None,
                     IPyCompletionProposal.PRIORITY_DEFAULT)
        l = ArrayList()
        l.add(oProp)
        return l
