"""Assign Params to Attributes by Joel Hedlund <joel.hedlund at gmail.com>. 

Changed:Fabio Zadrozny (binded to Ctrl+1 too)
"""

__version__ = "1.0.1"

__copyright__ = '''Available under the same conditions as PyDev.

See PyDev license for details.
http://pydev.sourceforge.net
'''

from org.python.pydev.editor.correctionassist.heuristics import IAssistProps #@UnresolvedImport
from org.python.pydev.editor.codecompletion import PyCompletionProposal #@UnresolvedImport
import assign_params_to_attributes_action as helper 
from org.python.pydev.ui import UIConstants #@UnresolvedImport
from org.python.pydev.editor.codecompletion import IPyCompletionProposal #@UnresolvedImport
from java.util import ArrayList
True, False = 1,0


class Prop(PyCompletionProposal):
    '''This is the proposal that Ctrl+1 will require
    '''
    

    def __init__(self,assignToAttribsOfSelf,*args):
        PyCompletionProposal.__init__(self, *args)
        self.assignToAttribsOfSelf = assignToAttribsOfSelf

    def apply(self, document):
        '''java: public void apply(IDocument document)
        '''
        self.assignToAttribsOfSelf.run()
        
    def getSelection(self, document):
        return None
        

class AssistAssignParamsToAttributes(IAssistProps):
    '''This is the assistant class, that will check if we can apply the action and actually apply it
    (really: it just repasses all to AssignToAttribsOfSelf)
    '''
    
    def getImage(self,imageCache, c):
        if imageCache is not None:
            return imageCache.get(c)
        
        return None

    def isValid(self, ps, sel, editor, offset):
        '''java: boolean isValid(PySelection ps, String sel, PyEdit edit, int offset);
        '''
        self.assignToAttribsOfSelf = helper.AssignToAttribsOfSelf(editor)
        return self.assignToAttribsOfSelf.isScriptApplicable(ps, False)
        
    def getProps(self, ps, imageCache, f, nature, editor, offset):
        '''java: List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, 
                                                    IPythonNature nature, PyEdit edit, int offset) 
        '''
        l = ArrayList();
        l.add(Prop(self.assignToAttribsOfSelf, '', 0, 0, 0, self.getImage(imageCache, UIConstants.ASSIST_DOCSTRING),
                "Assign parameters to attributes", None, None, IPyCompletionProposal.PRIORITY_DEFAULT));

        return l
    
    
    