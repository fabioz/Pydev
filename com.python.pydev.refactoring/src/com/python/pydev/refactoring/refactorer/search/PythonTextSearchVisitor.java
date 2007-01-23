package com.python.pydev.refactoring.refactorer.search;

import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.core.text.TextSearchScope;
import org.eclipse.search.internal.core.text.DocumentCharSequence;
import org.eclipse.search.internal.core.text.FileCharSequenceProvider;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.docutils.StringUtils;

public class PythonTextSearchVisitor  {
	
	public static class ReusableMatchAccess extends TextSearchMatchAccess {
		
		private int fOffset;
		private int fLength;
		private IFile fFile;
		private CharSequence fContent;
		
		public void initialize(IFile file, int offset, int length, CharSequence content) {
			fFile= file;
			fOffset= offset;
			fLength= length;
			fContent= content;
		}
				
		public IFile getFile() {
			return fFile;
		}
		
		public int getMatchOffset() {
			return fOffset;
		}
		
		public int getMatchLength() {
			return fLength;
		}

		public int getFileContentLength() {
			return fContent.length();
		}

		public char getFileContentChar(int offset) {
			return fContent.charAt(offset);
		}

		public String getFileContent(int offset, int length) {
			return fContent.subSequence(offset, offset + length).toString(); // must pass a copy!
		}
	}
	

	private final TextSearchRequestor fCollector;
	
	private Map fDocumentsInEditors;
	private String fSearchText;
	private IProgressMonitor fProgressMonitor;

	private int fNumberOfScannedFiles;
	private int fNumberOfFilesToScan;
	private IFile fCurrentFile;

	private final MultiStatus fStatus;
	
	private final FileCharSequenceProvider fFileCharSequenceProvider;
	
	private final ReusableMatchAccess fMatchAccess;
	
	public PythonTextSearchVisitor(TextSearchRequestor collector, String searchText) {
		fCollector= collector;
		fSearchText = searchText;
		fStatus= new MultiStatus(NewSearchUI.PLUGIN_ID, IStatus.OK, SearchMessages.TextSearchEngine_statusMessage, null);
		
		fFileCharSequenceProvider= new FileCharSequenceProvider();
		fMatchAccess= new ReusableMatchAccess();
	}
	
	public IStatus search(IFile[] files, IProgressMonitor monitor) {
		fProgressMonitor= monitor == null ? new NullProgressMonitor() : monitor;
        fNumberOfScannedFiles= 0;
        fNumberOfFilesToScan= files.length;
        fCurrentFile= null;
        
        Job monitorUpdateJob= new Job(SearchMessages.TextSearchVisitor_progress_updating_job) {
        	private int fLastNumberOfScannedFiles= 0;
        	
        	public IStatus run(IProgressMonitor inner) {
        		while (!inner.isCanceled()) {
					IFile file= fCurrentFile;
					if (file != null) {
						String fileName= file.getName();
						Object[] args= { fileName, new Integer(fNumberOfScannedFiles), new Integer(fNumberOfFilesToScan)};
						fProgressMonitor.subTask(Messages.format(SearchMessages.TextSearchVisitor_scanning, args));
						int steps= fNumberOfScannedFiles - fLastNumberOfScannedFiles;
						fProgressMonitor.worked(steps);
						fLastNumberOfScannedFiles += steps;
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						return Status.OK_STATUS;
					}
				}
    			return Status.OK_STATUS;
        	}
        };

        try {
        	String taskName= StringUtils.format("Searcing for %s", fSearchText);
            fProgressMonitor.beginTask(taskName, fNumberOfFilesToScan);
            monitorUpdateJob.setSystem(true);
            monitorUpdateJob.schedule();
            try {
	            fCollector.beginReporting();
	            processFiles(files);
	            return fStatus;
            } finally {
                monitorUpdateJob.cancel();
            }
        } finally {
            fProgressMonitor.done();
            fCollector.endReporting();
        }
	}
		
	public IStatus search(TextSearchScope scope, IProgressMonitor monitor) {
		return search(scope.evaluateFilesInScope(fStatus), monitor);
    }
    
	private void processFiles(IFile[] files) {
		fDocumentsInEditors= evalNonFileBufferDocuments();
        for (int i= 0; i < files.length; i++) {
        	fCurrentFile= files[i];
            boolean res= processFile(fCurrentFile);
            if (!res)
            	break;
		}
		fDocumentsInEditors= null;
	}
	
	/**
	 * @return returns a map from IFile to IDocument for all open, dirty editors
	 */
	private Map evalNonFileBufferDocuments() {
		Map result= new HashMap();
		IWorkbench workbench= SearchPlugin.getDefault().getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorReference[] editorRefs= pages[x].getEditorReferences();
				for (int z= 0; z < editorRefs.length; z++) {
					IEditorPart ep= editorRefs[z].getEditor(false);
					if (ep instanceof ITextEditor && ep.isDirty()) { // only dirty editors
						evaluateTextEditor(result, ep);
					}
				}
			}
		}
		return result;
	}

	private void evaluateTextEditor(Map result, IEditorPart ep) {
		IEditorInput input= ep.getEditorInput();
		if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) input).getFile();
			if (!result.containsKey(file)) { // take the first editor found
				ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
				ITextFileBuffer textFileBuffer= bufferManager.getTextFileBuffer(file.getFullPath());
				if (textFileBuffer != null) {
					// file buffer has precedence
					result.put(file, textFileBuffer.getDocument());
				} else {
					// use document provider
					IDocument document= ((ITextEditor) ep).getDocumentProvider().getDocument(input);
					if (document != null) {
						result.put(file, document);
					}
				}
			}
		}
	}

	public boolean processFile(IFile file) {
		try {
		    if (!fCollector.acceptFile(file) || fSearchText == null) {
		       return true;
		    }
		        
			IDocument document= getOpenDocument(file);
			
			if (document != null) {
				DocumentCharSequence documentCharSequence= new DocumentCharSequence(document);
				// assume all documents are non-binary
				locateMatches(file, documentCharSequence);
			} else {
				CharSequence seq= null;
				try {
					seq= fFileCharSequenceProvider.newCharSequence(file);
					if (hasBinaryContent(seq, file) && !fCollector.reportBinaryFile(file)) {
						return true;
					}
					locateMatches(file, seq);
				} catch (FileCharSequenceProvider.FileCharSequenceException e) {
					e.throwWrappedException();
				} finally {
					if (seq != null) {
						try {
							fFileCharSequenceProvider.releaseCharSequence(seq);
						} catch (IOException e) {
							SearchPlugin.log(e);
						}
					}
				}
			}
		} catch (UnsupportedCharsetException e) {
			String[] args= { getCharSetName(file), file.getFullPath().makeRelative().toString()};
			String message= Messages.format(SearchMessages.TextSearchVisitor_unsupportedcharset, args); 
			fStatus.add(new Status(IStatus.WARNING, NewSearchUI.PLUGIN_ID, IStatus.WARNING, message, e));
		} catch (IllegalCharsetNameException e) {
			String[] args= { getCharSetName(file), file.getFullPath().makeRelative().toString()};
			String message= Messages.format(SearchMessages.TextSearchVisitor_illegalcharset, args);
			fStatus.add(new Status(IStatus.WARNING, NewSearchUI.PLUGIN_ID, IStatus.WARNING, message, e));
		} catch (IOException e) {
			String[] args= { getExceptionMessage(e), file.getFullPath().makeRelative().toString()};
			String message= Messages.format(SearchMessages.TextSearchVisitor_error, args); 
			fStatus.add(new Status(IStatus.WARNING, NewSearchUI.PLUGIN_ID, IStatus.WARNING, message, e));
		} catch (CoreException e) {
			String[] args= { getExceptionMessage(e), file.getFullPath().makeRelative().toString()};
			String message= Messages.format(SearchMessages.TextSearchVisitor_error, args); 
			fStatus.add(new Status(IStatus.WARNING, NewSearchUI.PLUGIN_ID, IStatus.WARNING, message, e));
		} catch (StackOverflowError e) {
			String message= SearchMessages.TextSearchVisitor_patterntoocomplex0;
			fStatus.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
			return false;
		} finally {
			fNumberOfScannedFiles++;
		}
		if (fProgressMonitor.isCanceled())
			throw new OperationCanceledException(SearchMessages.TextSearchVisitor_canceled);
		
		return true;
	}
	
	private boolean hasBinaryContent(CharSequence seq, IFile file) throws CoreException {
		IContentDescription desc= file.getContentDescription();
		if (desc != null) {
			IContentType contentType= desc.getContentType();
			if (contentType != null && contentType.isKindOf(Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT))) {
				return false;
			}
		}
		
		// avoid calling seq.length() at it runs through the complete file,
		// thus it would do so for all binary files.
		try {
			int limit= FileCharSequenceProvider.BUFFER_SIZE;
			for (int i= 0; i < limit; i++) {
				if (seq.charAt(i) == '\0') {
					return true;
				}
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return false;
	}

	private void locateMatches(IFile file, CharSequence searchInput) throws CoreException {
		try {
			int k= 0;
			int total = 0;

			try{
				for(int i=0;;i++){
					total+=1;
					char c = searchInput.charAt(i);
					if(c == fSearchText.charAt(k)){
						k+=1;
						if(k == fSearchText.length()-1){
							k=0;
							fMatchAccess.initialize(file, 0, 1, searchInput);
							fCollector.acceptPatternMatch(fMatchAccess);
							return; //return on first match 
						}
					}else{
						k=0;
					}
					
					if (total++ == 20) {
						if (fProgressMonitor.isCanceled()) {
							throw new OperationCanceledException(SearchMessages.TextSearchVisitor_canceled);
						}
						total= 0;
					}
				}
			}catch(IndexOutOfBoundsException e){
				//that's because we don'th check for the len of searchInput.len because it may be slow
			}
			
		} finally {
			fMatchAccess.initialize(null, 0, 0, new String()); // clear references
		}
	}
	
	
	private String getExceptionMessage(Exception e) {
		String message= e.getLocalizedMessage();
		if (message == null) {
			return e.getClass().getName();
		}
		return message;
	}

	private IDocument getOpenDocument(IFile file) {
		IDocument document= (IDocument) fDocumentsInEditors.get(file);
		if (document == null) {
			ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
			ITextFileBuffer textFileBuffer= bufferManager.getTextFileBuffer(file.getFullPath());
			if (textFileBuffer != null) {
				document= textFileBuffer.getDocument();
			}
		}
		return document;
	}
	
	private String getCharSetName(IFile file) {
		try {
			return file.getCharset();
		} catch (CoreException e) {
			return "unknown"; //$NON-NLS-1$
		}
	}

}

