/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PyBackspace;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.CompletionProposal;
import org.python.pydev.editor.correctionassist.FixCompletionProposal;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.ClassNode;
import org.python.pydev.editor.model.FunctionNode;
import org.python.pydev.editor.model.ModelUtils;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistCreations implements IAssistProps {

    private String cls;
    private String delim;
    private String indentation;
    private String params;
    private int firstCharPosition;
    private ArrayList l;
    private String sel;
    private String callName;
    private String self;
    private String method;

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache, java.io.File, org.python.pydev.plugin.PythonNature)
     */
    public List getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, AbstractNode root) throws BadLocationException {
        l = new ArrayList();
        initializeAttrs(ps);

        if (sel.trim().length() == 0) {
            return l;
        }
        if(callName.length() == 0){
            return l;
        }


        if (firstCharPosition == 0){ 

            //so, first thing is checking the global 
            int newPos = 0;
            int lineOfOffset = ps.getDoc().getLineOfOffset(ps.getAbsoluteCursorOffset());
            
            if(lineOfOffset > 0){
                newPos = ps.getDoc().getLineInformation(lineOfOffset).getOffset();
            }
            
            l.add(new FixCompletionProposal(cls, newPos, 0, cls.length()+1, imageCache.get(UIConstants.ASSIST_NEW_CLASS),
                    "Make this a new class", null, null, ps.getStartLineIndex()+4));

            method = method.replaceFirst("%s", "");
            l.add(new FixCompletionProposal(method, newPos, 0, method.length()+1, imageCache.get(UIConstants.ASSIST_NEW_METHOD),
                    "Make this a new method", null, null, ps.getStartLineIndex()+2));

        }else{ //we are in a method or class context

	        if (root == null){
	            return l;
	        }
	        
	        //now, discover in which node we are right now...
	        AbstractNode current = ModelUtils.getLessOrEqualNode(root, ps.getAbsoluteCursorOffset(), ps.getDoc());
	        while (current != null) {
	            if (current instanceof FunctionNode
	                    || current instanceof ClassNode) {
	                break;
	            }
	            current = ModelUtils.getPreviousNode(current);
	        }
	        
	        
	        if(sel.indexOf("self.") != -1){ //we are going for a class method.
	            
	            if (current instanceof FunctionNode) { //if we are in a class, here we are within a method.
	                FunctionNode node = (FunctionNode) current;
	                
	                int newPos = 0;
	                int lineOfOffset = node.getStart().line;
	                
	                if(lineOfOffset > 0){
	                    newPos = ps.getDoc().getLineInformation(lineOfOffset).getOffset();
	                }
	                
	                int col = node.getStart().column;
	                String newIndent = indentation;
	                
	                while(newIndent.length() < col){
	                    newIndent += indentation;
	                }
	                String atStart = newIndent.replaceFirst(indentation, "");
	                method = method.replaceAll(indentation, newIndent);
	                method = atStart+method+delim;
	                
	                method = method.replaceFirst("%s", self);
	                
		            l.add(new CompletionProposal(method, newPos, 0, method.length()-4, imageCache.get(UIConstants.ASSIST_NEW_METHOD),
		                    "Create new method (in class)", null, null));
	            }
	            
	        }else{ //we are going for a class or a global method.
		        
	            while (current != null) {
	                if(current instanceof ClassNode) {
		                break;
		            }
		            current = ModelUtils.getPreviousNode(current);
		        }
	            if(current instanceof ClassNode){
	                ClassNode node = (ClassNode) current;
	                
	                int newPos = 0;
	                int lineOfOffset = node.getStart().line;
	                
	                if(lineOfOffset > 0){
	                    newPos = ps.getDoc().getLineInformation(lineOfOffset).getOffset();
	                }
	                String finalMethod = method+delim;
	                
	                finalMethod = finalMethod.replaceFirst("%s", "" );
	                
		            l.add(new CompletionProposal(finalMethod, newPos, 0, finalMethod.length(), imageCache.get(UIConstants.ASSIST_NEW_METHOD),
		                    "Create new method (in global context)", null, null));

	                cls = cls+delim;

	                l.add(new CompletionProposal(cls, newPos, 0, cls.length(), imageCache.get(UIConstants.ASSIST_NEW_CLASS),
		                    "Create new class (in global context)", null, null));
	                

	                
	                //now get the attempt to make this a new method ------------------------------------------
	                newPos = 0;
	                lineOfOffset = ps.getDoc().getLineOfOffset(ps.getAbsoluteCursorOffset());
	                
	                if(lineOfOffset > 0){
	                    newPos = ps.getDoc().getLineInformation(lineOfOffset).getOffset();
	                }

	                int col = firstCharPosition;
	                String newIndent = indentation;
	                
	                while(newIndent.length() <= col){ //one more...
	                    newIndent += indentation;
	                }
	                String atStart = newIndent.replaceFirst(indentation, "");
	                method = method.replaceAll(indentation, newIndent);
	                method = atStart+method+delim;

	                if(params.trim().length() > 0){
	                    method = method.replaceFirst("%s", "self, ");
	                }else{
	                    method = method.replaceFirst("%s", "self");
	                }
	                l.add(new FixCompletionProposal(method, newPos, 0, method.length()+1, imageCache.get(UIConstants.ASSIST_NEW_METHOD),
	                        "Make this a new method (in class)", null, null, ps.getStartLineIndex()+3));

	            }
	        }
        }        
        return l;
    }

    /**
     * @param ps
     * @throws BadLocationException
     */
    private void initializeAttrs(PySelection ps) throws BadLocationException {
        sel = PyAction.getLineWithoutComments(ps);

        //here, any callable can be used to create a new method or class.
        //we have to parse it, because we have to discover the parameters for the new class or method.
        
        callName = PyAction.getBeforeParentesisTok(ps);


        params = PyAction.getInsideParentesisTok(ps);
        firstCharPosition = PyAction.getFirstCharRelativePosition(ps.getDoc(), ps.getAbsoluteCursorOffset());
        
        indentation = PyBackspace.getStaticIndentationString();

        
        delim = PyAction.getDelimiter(ps.getDoc());
        cls = "class "+callName+"(object):"+delim+delim;
        
        self = "self";
        if(params.trim().length() != 0){
            self += ", ";
        }
        
        cls += indentation+"def __init__("+self+params+"):"+delim;
        cls += indentation+indentation+"pass"+delim;
        
        method = "def "+callName+"(%s"+params+"):"+delim+indentation+"pass"+delim;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.editor.actions.PySelection, java.lang.String)
     */
    public boolean isValid(PySelection ps, String sel) {
        return sel.indexOf("class ") == -1 && sel.indexOf("def ") == -1 && sel.indexOf("import ") == -1 && 
        ps.getTextSelection().getLength() == 0;
    }

}
