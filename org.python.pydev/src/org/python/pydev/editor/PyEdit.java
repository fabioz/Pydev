/*
 * @author: atotic
 * Created: July 10, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import org.eclipse.core.internal.resources.MarkerAttributeMap;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.python.parser.ParseException;
import org.python.parser.SimpleNode;
import org.python.parser.Token;
import org.python.parser.TokenMgrError;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.outline.PyOutlinePage;
import org.python.pydev.parser.IParserListener;
import org.python.pydev.parser.PyParser;
import org.python.pydev.ui.ColorCache;


/**
 * The TextWidget.
 * 
 * <p>Ties together all the main classes in this plugin.
 * <li>The {@link org.python.pydev.editor.PyEditConfiguration PyEditConfiguration} does preliminary partitioning.
 * <li>The {@link org.python.pydev.parser.PyParser PyParser} does a lazy validating python parse.
 * <li>The {@link org.python.pydev.outline.PyOutlinePage PyOutlinePage} shows the outline
 * 
 * <p>Listens to the parser's events, and displays error markers from the parser
 * 
 * <p>General notes:
 * <p>TextWidget creates SourceViewer, an SWT control
 * @see <a href="http://dev.eclipse.org/newslists/news.eclipse.tools/msg61594.html">This eclipse article was an inspiration</a>
 * 
 */
public class PyEdit extends TextEditor implements IParserListener {

	/** color cache */
	private ColorCache colorCache;
	/** lexical parser that continuously reparses the document on a thread */
	private PyParser parser;
	// Listener waits for tab/spaces preferences that affect sourceViewer
	private Preferences.IPropertyChangeListener prefListener;
	/** need it to support GUESS_TAB_SUBSTITUTION preference */
	private PyAutoIndentStrategy indentStrategy;
	/** need to hold onto it to support indentPrefix change through preferences */
	PyEditConfiguration editConfiguration;
	
	public PyEdit() {
		super();
		colorCache = new ColorCache(PydevPrefs.getPreferences());
		if (getDocumentProvider() == null) {
			setDocumentProvider(new PyDocumentProvider());
		}
		editConfiguration = new PyEditConfiguration(colorCache);
		setSourceViewerConfiguration(editConfiguration);
		indentStrategy = (PyAutoIndentStrategy)editConfiguration.getAutoIndentStrategy(null, null);
		setRangeIndicator(new DefaultRangeIndicator()); // enables standard vertical ruler
	}
	
	/**
	 * Sets the forceTabs preference for auto-indentation.
	 * 
	 * <p>This is the preference that overrides "use spaces" preference
	 * when file contains tabs (like mine do).
	 * <p>If the first indented line starts with a tab, 
	 * then tabs override spaces.
	 */
	private void resetForceTabs() {
		IDocument doc = getDocumentProvider().getDocument(getEditorInput());
		if (doc == null)
			return;
		if ( !PydevPrefs.getPreferences().getBoolean(PydevPrefs.GUESS_TAB_SUBSTITUTION)) {
			indentStrategy.setForceTabs(false);
			return;
		}

		int lines = doc.getNumberOfLines();
		boolean forceTabs = false;
		int i = 0;
		// look for the first line that starts with '  ', or '\t'
		while (i<lines) {
			try {
				IRegion r = doc.getLineInformation(i);
				String text = doc.get(r.getOffset(), r.getLength());
				if (text != null)
					if (text.startsWith("\t")) {
						forceTabs = true;
						break;
					}
					else if (text.startsWith("  ")) {
						forceTabs = false;
						break;
					}
			} catch (BadLocationException e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error forcing tabs", e);
				break;
			}
			i++;
		}
		indentStrategy.setForceTabs(forceTabs);
		editConfiguration.resetIndentPrefixes();
		// display a message in the status line
		if (forceTabs) {
			IEditorStatusLine statusLine = (IEditorStatusLine)getAdapter(IEditorStatusLine.class);
			if (statusLine != null)
				statusLine.setMessage(false, "Pydev: forcing tabs", null);
		}
	}
	
	/** 
	 * Initializes everyone that needs document access
	 * 
	 */
	public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
		super.init(site, input);
		parser = new PyParser(this);
		parser.addParseListener(this);
		parser.setDocument(getDocumentProvider().getDocument(input));
		
		// listen to changes in TAB_WIDTH preference
		prefListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {
				String property= event.getProperty();
				if (property.equals(PydevPrefs.TAB_WIDTH)) {
					ISourceViewer sourceViewer= getSourceViewer();
					if (sourceViewer == null)
						return;
					sourceViewer.getTextWidget().setTabs(PydevPlugin.getDefault().getPluginPreferences().getInt(PydevPrefs.TAB_WIDTH));
				}
				else if (property.equals(PydevPrefs.GUESS_TAB_SUBSTITUTION)) {
					resetForceTabs();
				}
			}
		};
		resetForceTabs();
		PydevPrefs.getPreferences().addPropertyChangeListener(prefListener);
	}
	
	// cleanup
	public void dispose() {
		PydevPrefs.getPreferences().removePropertyChangeListener(prefListener);
		parser.dispose();
		colorCache.dispose();
		super.dispose();
	}

	public PyParser getParser() {
		return parser;
	}
	
	/**
	 * @return an outline view
	 */
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter))
			return new PyOutlinePage(this);
		else
			return super.getAdapter(adapter);
	}
	
	/**
	 * implementation copied from 
	 * @see org.eclipse.ui.externaltools.internal.ant.editor.PlantyEditor#setSelection
	 */
	public void setSelection(int offset, int length) {
		ISourceViewer sourceViewer= getSourceViewer();
		sourceViewer.setSelectedRange(offset, length);
		sourceViewer.revealRange(offset, length);
	}

	/**
	 * this event comes when document was parsed without errors
	 * 
	 * Removes all the error markers
	 */
	public void parserChanged(SimpleNode root) {
		// Remove all the error markers
		IEditorInput input = getEditorInput();
		IFile original= (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
		try {
			if (original != null)
				original.deleteMarkers(IMarker.PROBLEM, false, 1);
		} catch (CoreException e) {
			// What bad can come from removing markers? Ignore this exception
			e.printStackTrace();
		}
	}

	/**
	 * this event comes when parse ended in an error
	 * 
	 * generates an error marker on the document
	 */
	public void parserError(Throwable error) {
		IEditorInput input = getEditorInput();
		IFile original= (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
		if (original == null)
			return;
		try {
			IDocument document = getDocumentProvider().getDocument(getEditorInput());
			original.deleteMarkers(IMarker.PROBLEM, false, 1);

			int errorStart;
			int errorEnd;
			int errorLine;
			String message;
			if (error instanceof ParseException) {
				ParseException.verboseExceptions = true;
				ParseException parseErr = (ParseException)error;
				// Figure out where the error is in the document, and create a marker for it
				Token errorToken =
					parseErr.currentToken.next != null
						? parseErr.currentToken.next
						: parseErr.currentToken;
				IRegion startLine =
					document.getLineInformation(errorToken.beginLine - 1);
				IRegion endLine =
					document.getLineInformation(errorToken.endLine - 1);
				errorStart = startLine.getOffset() + errorToken.beginColumn - 1;
				errorEnd = endLine.getOffset() + errorToken.endColumn;
				errorLine = errorToken.beginLine;
				message = parseErr.getMessage();
			} else {
				TokenMgrError tokenErr = (TokenMgrError)error;
				IRegion startLine =
					document.getLineInformation(tokenErr.errorLine - 1);
				errorStart = startLine.getOffset();
				errorEnd = startLine.getOffset() + tokenErr.errorColumn;
				errorLine = tokenErr.errorLine;
				message = tokenErr.getMessage();
			}
			// map.put(IMarker.LOCATION, "Whassup?"); this is the location field in task manager
			if (message != null) { // prettyprint
				message = message.replaceAll("\\r\\n", " ");
				message = message.replaceAll("\\r", " ");
				message = message.replaceAll("\\n", " ");
			}
			MarkerAttributeMap map = new MarkerAttributeMap();
			map.put(IMarker.MESSAGE, message);
			map.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
			map.put(IMarker.LINE_NUMBER, new Integer(errorLine));
			map.put(IMarker.CHAR_START, new Integer(errorStart));
			map.put(IMarker.CHAR_END, new Integer(errorEnd));
			map.put(IMarker.TRANSIENT, new Boolean(true));
			MarkerUtilities.createMarker(original, map, IMarker.PROBLEM);

		} catch (CoreException e1) {
			// Whatever, could not create a marker. Swallow this one
			e1.printStackTrace();
		} catch (BadLocationException e2) {
			// Whatever, could not create a marker. Swallow this one
			e2.printStackTrace();
		}
	}

}

