/*
 * @author: atotic
 * Created: July 10, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListResourceBundle;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.python.parser.ParseException;
import org.python.parser.SimpleNode;
import org.python.parser.Token;
import org.python.parser.TokenMgrError;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.editor.codecompletion.PythonShell;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codefolding.CodeFoldingSetter;
import org.python.pydev.editor.codefolding.PyEditProjection;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.IModelListener;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editor.model.ModelMaker;
import org.python.pydev.outline.PyOutlinePage;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.ColorCache;

/**
 * The TextWidget.
 * 
 * <p>
 * Ties together all the main classes in this plugin.
 * <li>The {@link org.python.pydev.editor.PyEditConfiguration PyEditConfiguration}does preliminary partitioning.
 * <li>The {@link org.python.pydev.parser.PyParser PyParser}does a lazy validating python parse.
 * <li>The {@link org.python.pydev.outline.PyOutlinePage PyOutlinePage}shows the outline
 * 
 * <p>
 * Listens to the parser's events, and displays error markers from the parser.
 * 
 * <p>
 * General notes:
 * <p>
 * TextWidget creates SourceViewer, an SWT control
 * 
 * @see <a href="http://dev.eclipse.org/newslists/news.eclipse.tools/msg61594.html">This eclipse article was an inspiration </a>
 *  
 */
public class PyEdit extends PyEditProjection implements IPyEdit {

    static public String EDITOR_ID = "org.python.pydev.editor.PythonEditor";

    static public String ACTION_OPEN = "OpenEditor";

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

    /** Python model */
    AbstractNode pythonModel;

    /**
     * AST that created python model
     */
    SimpleNode ast;

    /** Hyperlinking listener */
    Hyperlink fMouseListener;

    /** listeners that get notified of model changes */
    ArrayList modelListeners;

    public PyEdit() {
        super();
        modelListeners = new ArrayList();
        colorCache = new ColorCache(getChainedPrefStore());
        
        editConfiguration = new PyEditConfiguration(colorCache, this);
        setSourceViewerConfiguration(editConfiguration);
        indentStrategy = editConfiguration.getPyAutoIndentStrategy();
        setRangeIndicator(new DefaultRangeIndicator()); // enables standard
        // vertical ruler

        //Added to set the code folding.
        CodeFoldingSetter codeFoldingSetter = new CodeFoldingSetter(this);
        this.addModelListener(codeFoldingSetter);
        this.addPropertyListener(codeFoldingSetter);

        //we also want to initialize our shells...
        //we use 2: one for refactoring and one for code completion.
        new Thread() {
            public void run() {
                try {
                    try {
                        PythonShell.getServerShell(PythonShell.OTHERS_SHELL);
                    } catch (RuntimeException e1) {
                    }
                    try {
                        PythonShell.getServerShell(PythonShell.COMPLETION_SHELL);
                    } catch (RuntimeException e1) {
                    }
                } catch (Exception e) {
                }

            }
        }.start();

    }

    /**
     * Sets the forceTabs preference for auto-indentation.
     * 
     * <p>
     * This is the preference that overrides "use spaces" preference when file contains tabs (like mine do).
     * <p>
     * If the first indented line starts with a tab, then tabs override spaces.
     */
    private void resetForceTabs() {
        IDocument doc = getDocumentProvider().getDocument(getEditorInput());
        if (doc == null)
            return;
        if (!PydevPrefs.getPreferences().getBoolean(PydevPrefs.GUESS_TAB_SUBSTITUTION)) {
            indentStrategy.getIndentPrefs().setForceTabs(false);
            return;
        }

        int lines = doc.getNumberOfLines();
        boolean forceTabs = false;
        int i = 0;
        // look for the first line that starts with ' ', or '\t'
        while (i < lines) {
            try {
                IRegion r = doc.getLineInformation(i);
                String text = doc.get(r.getOffset(), r.getLength());
                if (text != null)
                    if (text.startsWith("\t")) {
                        forceTabs = true;
                        break;
                    } else if (text.startsWith("  ")) {
                        forceTabs = false;
                        break;
                    }
            } catch (BadLocationException e) {
                PydevPlugin.log(IStatus.ERROR, "Unexpected error forcing tabs", e);
                break;
            }
            i++;
        }
        indentStrategy.getIndentPrefs().setForceTabs(forceTabs);
        editConfiguration.resetIndentPrefixes();
        // display a message in the status line
        if (forceTabs) {
            IEditorStatusLine statusLine = (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
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

        IDocument document = getDocument(input);
        
        // check the document partitioner (sanity check / fix)
        PyPartitionScanner.checkPartitionScanner(document);

        parser = new PyParser(this);
        parser.addParseListener(this);


        // Also adds Python nature to the project.
    	// The reason this is done here is because I want to assign python
    	// nature automatically to any project that has active python files.
        PythonNature.addNature(input);
        
        //set the parser for the document
        parser.setDocument(document);

        // listen to changes in TAB_WIDTH preference
        prefListener = new Preferences.IPropertyChangeListener() {
            public void propertyChange(Preferences.PropertyChangeEvent event) {
                String property = event.getProperty();
                if (property.equals(PydevPrefs.TAB_WIDTH)) {
                    ISourceViewer sourceViewer = getSourceViewer();
                    if (sourceViewer == null)
                        return;
                    sourceViewer.getTextWidget().setTabs(PydevPlugin.getDefault().getPluginPreferences().getInt(PydevPrefs.TAB_WIDTH));
                } else if (property.equals(PydevPrefs.GUESS_TAB_SUBSTITUTION)) {
                    resetForceTabs();
                } else if (property.equals(PydevPrefs.CODE_COLOR) || property.equals(PydevPrefs.DECORATOR_COLOR) || property.equals(PydevPrefs.NUMBER_COLOR)
                        || property.equals(PydevPrefs.KEYWORD_COLOR) || property.equals(PydevPrefs.COMMENT_COLOR) || property.equals(PydevPrefs.STRING_COLOR)) {
                    colorCache.reloadNamedColor(property);
                    editConfiguration.updateSyntaxColor(property);
                    getSourceViewer().invalidateTextPresentation();
                } 
                else if (property.equals(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_HYPERLINK_COLOR)) {
                    colorCache.reloadNamedColor(property);
                    if (fMouseListener != null)
                        fMouseListener.updateColor(getSourceViewer());
                }
            }
        };
        resetForceTabs();
        PydevPrefs.getPreferences().addPropertyChangeListener(prefListener);
    }

    
    /**
     * @param input
     * @return
     */
    private IDocument getDocument(final IEditorInput input) {
        return getDocumentProvider().getDocument(input);
    }

    /**
     * @param input
     * @return
     */
    private IDocument getDocument() {
        return getDocumentProvider().getDocument(getEditorInput());
    }

    /** 
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#performSave(boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
        fixEncoding(getEditorInput(), getDocument());
        super.performSave(overwrite, progressMonitor);
    }
    /**
     * Forces the encoding to the one specified in the file
     * 
     * @param input
     * @param document
     */
    private void fixEncoding(final IEditorInput input, IDocument document) {
        if (input instanceof FileEditorInput) {
            final IFile file = (IFile) ((FileEditorInput) input).getAdapter(IFile.class);
            final String encoding = PythonPathHelper.getPythonFileEncoding(document);
            if (encoding != null) {
                try {
                    if (encoding.equals(file.getCharset()) == false) {

                        new Job("Change encoding") {

                            protected IStatus run(IProgressMonitor monitor) {
                                try {
                                    file.setCharset(encoding, monitor);
                                    ((TextFileDocumentProvider) getDocumentProvider()).setEncoding(input, encoding);
                                    //refresh it...
                                    file.refreshLocal(IResource.DEPTH_INFINITE, null);
                                } catch (CoreException e) {
                                    PydevPlugin.log(e);
                                }
                                return Status.OK_STATUS;
                            }

                        }.schedule();
                    }
                } catch (CoreException e) {
                    PydevPlugin.log(e);
                }
            }
        }
    }

    public IProject getProject() {
        IEditorInput editorInput = this.getEditorInput();
        if (editorInput instanceof FileEditorInput) {
            IFile file = (IFile) ((FileEditorInput) editorInput).getAdapter(IFile.class);
            return file.getProject();
        }
        return null;
    }

    /**
     * @return
     *  
     */
    public File getEditorFile() {
        File f = null;
        IEditorInput editorInput = this.getEditorInput();
        if (editorInput instanceof FileEditorInput) {
            IFile file = (IFile) ((FileEditorInput) editorInput).getAdapter(IFile.class);

            IPath path = file.getLocation().makeAbsolute();
            f = path.toFile();
        }
        return f;
    }

    // cleanup
    public void dispose() {
        PydevPrefs.getPreferences().removePropertyChangeListener(prefListener);
        parser.dispose();
        colorCache.dispose();
        super.dispose();
    }

    private static final String TEMPLATE_PROPOSALS_ID = "org.python.pydev.editors.PyEdit.TemplateProposals";

    private static final String CONTENTASSIST_PROPOSAL_ID = "org.python.pydev.editors.PyEdit.ContentAssistProposal";

    private static final String CORRECTIONASSIST_PROPOSAL_ID = "org.python.pydev.editors.PyEdit.CorrectionAssist";

    public static final int CORRECTIONASSIST_PROPOSALS = 999777;

    private static class MyResources extends ListResourceBundle {
        public Object[][] getContents() {
            return contents;
        }

        static final Object[][] contents = { { "CorrectionAssist", "CorrectionAssist" }, { "ContentAssistProposal", "ContentAssistProposal" }, { "TemplateProposals", "TemplateProposals" }, };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#createActions()
     */
    protected void createActions() {
        super.createActions();

        MyResources resources = new MyResources();

        //quick fix in editor.
        IAction action = new TextOperationAction(resources, "CorrectionAssist", this, CORRECTIONASSIST_PROPOSALS); //$NON-NLS-1$

        action.setActionDefinitionId(CORRECTIONASSIST_PROPOSAL_ID);
        setAction(CORRECTIONASSIST_PROPOSAL_ID, action); //$NON-NLS-1$ 
        markAsStateDependentAction(CORRECTIONASSIST_PROPOSAL_ID, true); //$NON-NLS-1$ 
        setActionActivationCode(CORRECTIONASSIST_PROPOSAL_ID, '1', -1, SWT.CTRL);

        // This action will fire a CONTENTASSIST_PROPOSALS operation
        // when executed
        // -------------------------------------------------------------------------------------
        action = new TextOperationAction(resources, "ContentAssistProposal", this, SourceViewer.CONTENTASSIST_PROPOSALS);

        action.setActionDefinitionId(CONTENTASSIST_PROPOSAL_ID);
        // Tell the editor about this new action
        setAction(CONTENTASSIST_PROPOSAL_ID, action);
        // Tell the editor to execute this action
        // when Ctrl+Spacebar is pressed
        setActionActivationCode(CONTENTASSIST_PROPOSAL_ID, ' ', -1, SWT.CTRL);

        //template proposals
        // ---------------------------------------------------------------------------------
        action = new TextOperationAction(resources, "TemplateProposals", this, ISourceViewer.CONTENTASSIST_PROPOSALS);

        action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        setAction(TEMPLATE_PROPOSALS_ID, action);
        markAsStateDependentAction(TEMPLATE_PROPOSALS_ID, true);

        //open action
        // ----------------------------------------------------------------------------------------
        IAction openAction = new PyOpenAction();
        setAction(ACTION_OPEN, openAction);
        enableBrowserLikeLinks();
    }

    protected void initializeKeyBindingScopes() {
        setKeyBindingScopes(new String[] { "org.python.pydev.ui.editor.scope" }); //$NON-NLS-1$
    }

    public PyParser getParser() {
        return parser;
    }

    public AbstractNode getPythonModel() {
        return pythonModel;
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
     * implementation copied from org.eclipse.ui.externaltools.internal.ant.editor.PlantyEditor#setSelection
     */
    public void setSelection(int offset, int length) {
        ISourceViewer sourceViewer = getSourceViewer();
        sourceViewer.setSelectedRange(offset, length);
        sourceViewer.revealRange(offset, length);
    }

    /**
     * Selects & reveals the model node
     */
    public void revealModelNode(AbstractNode node) {
        if (node == null)
            return; // nothing to see here
        boolean wholeLine = false;
        Location start = node.getStart();
        Location end = node.getEnd();
        IDocument document = getDocumentProvider().getDocument(getEditorInput());
        int offset, length;
        try {
            if (wholeLine) {
                IRegion r;
                r = document.getLineInformation(start.line);
                offset = r.getOffset();
                length = r.getLength();
            } else {
                offset = start.toOffset(document);
                length = end.toOffset(document) - offset;
            }
        } catch (BadLocationException e) {
            PydevPlugin.log(IStatus.WARNING, "error trying to revealModelItem" + node.toString(), e);
            return;
        }
        setSelection(offset, length);
    }

    /**
     * this event comes when document was parsed without errors
     * 
     * Removes all the error markers
     */
    public void parserChanged(SimpleNode root) {
        // Remove all the error markers
        IEditorInput input = getEditorInput();
        IPath filePath = null;
        if (input instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) input).getFile();
            filePath = file.getLocation();
        } else if (input instanceof IStorageEditorInput)
            try {
                filePath = ((IStorageEditorInput) input).getStorage().getFullPath();
                filePath = filePath.makeAbsolute();
            } catch (CoreException e2) {
                PydevPlugin.log(IStatus.ERROR, "unexpected error getting path", e2);
            }
        else if (input instanceof ILocationProvider) {
            filePath = ((ILocationProvider) input).getPath(input);
            filePath = filePath.makeAbsolute();
        } else
            PydevPlugin.log(IStatus.ERROR, "unexpected type of editor input " + input.getClass().toString(), null);

        try {
            IResource res = (IResource) input.getAdapter(IResource.class);
            if (res != null)
                res.deleteMarkers(IMarker.PROBLEM, false, 1);
        } catch (CoreException e) {
            // What bad can come from removing markers? Ignore this exception
            PydevPlugin.log(IStatus.WARNING, "Unexpected error removing markers", e);
        }
        IDocument document = getDocument(input);
        int lastLine = document.getNumberOfLines();
        try {
            document.getLineInformation(lastLine - 1);
            ast = root;
            pythonModel = ModelMaker.createModel(root, document, filePath);
            fireModelChanged(pythonModel, ast);
        } catch (BadLocationException e1) {
            PydevPlugin.log(IStatus.WARNING, "Unexpected error getting document length. No model!", e1);
        }
    }

    /**
     * this event comes when parse ended in an error
     * 
     * generates an error marker on the document
     */
    public void parserError(Throwable error) {
        IEditorInput input = getEditorInput();
        IFile original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
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
                ParseException parseErr = (ParseException) error;
                // Figure out where the error is in the document, and create a
                // marker for it
                Token errorToken = parseErr.currentToken.next != null ? parseErr.currentToken.next : parseErr.currentToken;
                IRegion startLine = document.getLineInformation(errorToken.beginLine - 1);
                IRegion endLine;
                if (errorToken.endLine == 0)
                    endLine = startLine;
                else
                    endLine = document.getLineInformation(errorToken.endLine - 1);
                errorStart = startLine.getOffset() + errorToken.beginColumn - 1;
                errorEnd = endLine.getOffset() + errorToken.endColumn;
                errorLine = errorToken.beginLine;
                message = parseErr.getMessage();
            } else {
                TokenMgrError tokenErr = (TokenMgrError) error;
                IRegion startLine = document.getLineInformation(tokenErr.errorLine - 1);
                errorStart = startLine.getOffset();
                errorEnd = startLine.getOffset() + tokenErr.errorColumn;
                errorLine = tokenErr.errorLine;
                message = tokenErr.getMessage();
            }
            // map.put(IMarker.LOCATION, "Whassup?"); this is the location field
            // in task manager
            if (message != null) { // prettyprint
                message = message.replaceAll("\\r\\n", " ");
                message = message.replaceAll("\\r", " ");
                message = message.replaceAll("\\n", " ");
            }
            Map map = new HashMap();
            map.put(IMarker.MESSAGE, message);
            map.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
            map.put(IMarker.LINE_NUMBER, new Integer(errorLine));
            map.put(IMarker.CHAR_START, new Integer(errorStart));
            map.put(IMarker.CHAR_END, new Integer(errorEnd));
            map.put(IMarker.TRANSIENT, Boolean.valueOf(true));
            MarkerUtilities.createMarker(original, map, IMarker.PROBLEM);

        } catch (CoreException e1) {
            // Whatever, could not create a marker. Swallow this one
            e1.printStackTrace();
        } catch (BadLocationException e2) {
            // Whatever, could not create a marker. Swallow this one
            e2.printStackTrace();
        }
    }

    public void enableBrowserLikeLinks() {
        if (fMouseListener == null) {
            fMouseListener = new Hyperlink(getSourceViewer(), this, colorCache);
            fMouseListener.install();
        }
    }

    /**
     * Disables browser like links.
     */
    public void disableBrowserLikeLinks() {
        if (fMouseListener != null) {
            fMouseListener.uninstall();
            fMouseListener = null;
        }
    }

    /** stock listener implementation */
    public void addModelListener(IModelListener listener) {
        Assert.isNotNull(listener);
        if (!modelListeners.contains(listener))
            modelListeners.add(listener);
    }

    /** stock listener implementation */
    public void removeModelListener(IModelListener listener) {
        Assert.isNotNull(listener);
        modelListeners.remove(listener);
    }

    /**
     * stock listener implementation event is fired whenever we get a new root
     * 
     * @param root2
     */
    protected void fireModelChanged(AbstractNode root, SimpleNode root2) {
        if (modelListeners.size() > 0) {
            ArrayList list = new ArrayList(modelListeners);
            Iterator e = list.iterator();
            while (e.hasNext()) {
                IModelListener l = (IModelListener) e.next();
                l.modelChanged(root, root2);
            }
        }
    }

    /**
     * @return
     *  
     */
    public IPythonNature getPythonNature() {
        IProject project = getProject();
        return PythonNature.getPythonNature(project);
    }

    protected void initializeEditor() {
        super.initializeEditor();
        IPreferenceStore prefStore = getChainedPrefStore();
        this.setPreferenceStore(prefStore);
    }

    /**
     * @return
     */
    public SimpleNode getAST() {
        return ast;
    }
}

