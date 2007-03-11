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
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.uiutils.RunInUiThread;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.IParserObserver;
import org.python.pydev.parser.jython.SimpleNode;
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
		
		public void parserChanged(SimpleNode root, IAdaptable file, IDocument doc) {
			pyEdit.getParser().removeParseListener(this); //we'll only listen for this single parse
			
			try {
				ISourceViewer viewer= pyEdit.getPySourceViewer();
				IDocument document= viewer.getDocument();
				PySelection ps = PySelection.createFromNonUiThread(pyEdit);
				LinkedPositionGroup group= new LinkedPositionGroup();
				
				if(!fillWithOccurrences(document, group, new NullProgressMonitor(), ps)){
					return ;
				}
				
				if (group.isEmpty()) {
					return ;
				}
				
				LinkedModeModel model= new LinkedModeModel();
				model.addGroup(group);
				model.forceInstall();
				final LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
				Tuple<String,Integer> currToken = ps.getCurrToken();
				ui.setExitPosition(viewer, currToken.o2 + currToken.o1.length(), 0, Integer.MAX_VALUE);
				Runnable r = new Runnable(){
					public void run() {
						ui.enter();
					}
				};
				RunInUiThread.async(r);
			} catch (BadLocationException e) {
				Log.log(e);
			} catch (Exception e) {
				Log.log(e);
			}
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
				pyEdit.getParser().addParseListener(observer); //it will analyze when the next parse is finished
				pyEdit.getParser().parseNow(true);
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
		List<ASTEntry> occurrences = processor.getOccurrences();
		
		if(monitor.isCanceled()){
			return false;
		}

		//used so that we don't add duplicates
		Set<Position> found = new HashSet<Position>();
		
		if(occurrences != null){
			int i = 0;
			for (ASTEntry entry : occurrences) {
				i++;
                try {
					IRegion lineInformation = document.getLineInformation(entry.node.beginLine - 1);
					int colDef = NodeUtils.getClassOrFuncColDefinition(entry.node) -1;
                    ProposalPosition proposalPosition = new ProposalPosition(document, lineInformation.getOffset() + colDef, req.initialName.length(), i , new ICompletionProposal[0]);
                    if(found.contains(proposalPosition) == false){
                    	found.add(proposalPosition);
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
