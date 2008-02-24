/*
 * Created on May 21, 2006
 */
package com.python.pydev.refactoring.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.parser.IParserObserver;
import org.python.pydev.core.parser.ISimpleNode;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.markoccurrences.MarkOccurrencesJob;
import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;

/**
 * This action should mark to rename all the occurrences found for some name in the file
 */
public class PyRenameInFileAction extends Action{
	
	/**
	 * This class makes the rename when the reparse we asked for is triggered.
	 */
	private class RenameInFileParserObserver implements IParserObserver {
		
	    /**
	     * As soon as the reparse is done, this method is called to actually make the rename.
	     */
		public void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc) {
		    pyEdit.getParser().removeParseListener(this); //we'll only listen for this single parse
		    
		    /**
		     * Create an ui job to actually make the rename (otherwise we can't make ui.enter() nor create a PySelection.)
		     */
		    UIJob job = new UIJob("Rename"){
		        
		        @Override
		        public IStatus runInUIThread(IProgressMonitor monitor) {
		            try {
		                ISourceViewer viewer= pyEdit.getPySourceViewer();
		                IDocument document= viewer.getDocument();
		                PySelection ps = new PySelection(pyEdit);
		                LinkedPositionGroup group= new LinkedPositionGroup();
		                
		                if(!fillWithOccurrences(document, group, new NullProgressMonitor(), ps)){
		                    return Status.OK_STATUS;
		                }
		                
		                if (group.isEmpty()) {
		                    return Status.OK_STATUS;
		                }
		                
		                LinkedModeModel model= new LinkedModeModel();
		                model.addGroup(group);
		                if(model.tryInstall() && model.getTabStopSequence().size() > 0){
		                    final LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
		                    Tuple<String,Integer> currToken = ps.getCurrToken();
		                    ui.setExitPosition(viewer, currToken.o2 + currToken.o1.length(), 0, Integer.MAX_VALUE);
		                    ui.enter();
		                }
		            } catch (BadLocationException e) {
		                Log.log(e);
		            } catch (Throwable e) {
		                Log.log(e);
		            }
		            return Status.OK_STATUS;
		        }
		    };
		    job.setPriority(Job.INTERACTIVE);
		    job.schedule();
		}
		
		public void parserError(Throwable error, IAdaptable file, IDocument doc) {
		}
	}



	/**
	 * This class adds an observer and triggers a reparse that this listener should listen to. 
	 */
    private class RenameInFileJob extends Job {

		private RenameInFileJob(String name) {
			super(name);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IParserObserver observer = new RenameInFileParserObserver();
			PyParser parser = pyEdit.getParser();
			parser.addParseListener(observer); //it will analyze when the next parse is finished
			parser.forceReparse();
			return Status.OK_STATUS;
		}
	}


	private PyEdit pyEdit;


    public PyRenameInFileAction(PyEdit edit) {
        this.pyEdit = edit;
    }


    public void run() {
    	Job j = new RenameInFileJob("Rename In File");
    	j.setPriority(Job.INTERACTIVE);
    	j.schedule();
    }


    /**
     * Puts the found positions referente to the occurrences in the group
     * 
     * @param document the document that will contain this positions 
     * @param group the group that will contain this positions
     * @param ps the selection used
     * @return 
     * 
     * @throws BadLocationException
     * @throws OperationCanceledException
     * @throws CoreException
     */
	private boolean fillWithOccurrences(IDocument document, LinkedPositionGroup group, IProgressMonitor monitor, PySelection ps) throws BadLocationException, OperationCanceledException, CoreException {
		
		RefactoringRequest req = MarkOccurrencesJob.getRefactoringRequest(pyEdit, MarkOccurrencesJob.getRefactorAction(pyEdit), ps);
		if(monitor.isCanceled()){
			return false;
		}
		
		PyRenameEntryPoint processor = new PyRenameEntryPoint(req);
        
        //process it to get what we need
		processor.checkInitialConditions(monitor);
        processor.checkFinalConditions(monitor, null);
        HashSet<ASTEntry> occurrences = processor.getOccurrences();
		
		if(monitor.isCanceled()){
			return false;
		}

		//used so that we don't add duplicates
		Set<Tuple<Integer,Integer>> found = new HashSet<Tuple<Integer,Integer>>();
		
		if(occurrences != null){
			int i = 0;
			for (ASTEntry entry : occurrences) {
                try {
					IRegion lineInformation = document.getLineInformation(entry.node.beginLine - 1);
					int colDef = NodeUtils.getClassOrFuncColDefinition(entry.node) -1;
					
                    int offset = lineInformation.getOffset() + colDef;
					int len = req.initialName.length();
					Tuple<Integer, Integer> foundAt = new Tuple<Integer, Integer>(offset, len);
					
                    if(!found.contains(foundAt)){
                    	i++;
                    	ProposalPosition proposalPosition = new ProposalPosition(document, offset, len, i , new ICompletionProposal[0]);
                    	found.add(foundAt);
                    	group.addPosition(proposalPosition);
                    }
				} catch (Exception e) {
					Log.log(e);
					return false;
				}
			}
		}
		return true;
	}

}
