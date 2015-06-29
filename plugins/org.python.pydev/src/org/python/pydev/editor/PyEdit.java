/**
* Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
* Licensed under the terms of the Eclipse Public License (EPL).
* Please see the license.txt included with this distribution for details.
* Any modifications to this file must keep this entire header intact.
*/
package org.python.pydev.editor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.ITextEditorExtension2;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.python.pydev.changed_lines.ChangedLinesComputer;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ITabChangedListener;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.NotConfiguredInterpreterException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PythonPairMatcher;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.editor.actions.FirstCharAction;
import org.python.pydev.editor.actions.IExecuteLineAction;
import org.python.pydev.editor.actions.OfflineAction;
import org.python.pydev.editor.actions.OfflineActionTarget;
import org.python.pydev.editor.actions.PyBackspace;
import org.python.pydev.editor.actions.PyFormatStd;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.editor.actions.PyMoveLineDownAction;
import org.python.pydev.editor.actions.PyMoveLineUpAction;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.actions.PyOrganizeImports;
import org.python.pydev.editor.actions.PyPeerLinker;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.codefolding.CodeFoldingSetter;
import org.python.pydev.editor.codefolding.PyEditProjection;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.correctionassist.PythonCorrectionProcessor;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editor.saveactions.PydevSaveActionsPrefPage;
import org.python.pydev.editor.scripting.PyEditScripting;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.outline.ParsedModel;
import org.python.pydev.outline.PyOutlinePage;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.PyParserManager;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.fastparser.ScopesParser;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.preferences.CheckDefaultPreferencesDialog;
import org.python.pydev.plugin.preferences.PyCodeFormatterPage;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;
import org.python.pydev.shared_core.model.ErrorDescription;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;
import org.python.pydev.shared_core.parsing.BaseParserManager;
import org.python.pydev.shared_core.parsing.ChangedParserInfoForObservers;
import org.python.pydev.shared_core.parsing.ErrorParserInfoForObservers;
import org.python.pydev.shared_core.parsing.IParserObserver3;
import org.python.pydev.shared_core.parsing.IScopesParser;
import org.python.pydev.shared_core.string.ICharacterPairMatcher2;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsole;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.editor.IPyEditListener;
import org.python.pydev.shared_ui.editor_input.PydevFileEditorInput;
import org.python.pydev.shared_ui.outline.IOutlineModel;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;
import org.python.pydev.shared_ui.utils.PyMarkerUtils;
import org.python.pydev.shared_ui.utils.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.ui.ColorAndStyleCache;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

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
public class PyEdit extends PyEditProjection implements IPyEdit, IGrammarVersionProvider,
        IPySyntaxHighlightingAndCodeCompletionEditor, IParserObserver3, ITabChangedListener {

    public static final String PYDEV_EDITOR_KEYBINDINGS_CONTEXT_ID = "org.python.pydev.ui.editor.scope";

    static {
        ParseException.verboseExceptions = true;
    }

    public static final String PY_EDIT_CONTEXT = "#PyEditContext";
    public static final String PY_EDIT_RULER_CONTEXT = "#PyEditRulerContext";

    static public final String EDITOR_ID = "org.python.pydev.editor.PythonEditor";

    static public final String ACTION_OPEN = "OpenEditor";

    static private final Set<PyEdit> currentlyOpenedEditors = new HashSet<PyEdit>();
    static private final Object currentlyOpenedEditorsLock = new Object();

    /** color cache */
    private ColorAndStyleCache colorCache;

    // Listener waits for tab/spaces preferences that affect sourceViewer
    private IPropertyChangeListener prefListener;

    /** need it to support GUESS_TAB_SUBSTITUTION preference */
    private PyAutoIndentStrategy indentStrategy;

    /** need to hold onto it to support indentPrefix change through preferences */
    private PyEditConfiguration editConfiguration;

    public PyEditConfiguration getEditConfiguration() {
        return editConfiguration;
    }

    public ColorAndStyleCache getColorCache() {
        return colorCache;
    }

    /**
     * Important: keep for scripting
     */
    public PySelection createPySelection() {
        return new PySelection(this);
    }

    @Override
    public TextSelectionUtils createTextSelectionUtils() {
        return new PySelection(this);
    }

    /**
     * AST that created python model
     */
    private volatile SimpleNode ast;
    private volatile long astModificationTimeStamp = -1;

    /**
     * The last parsing error description we got.
     */
    private volatile ErrorDescription errorDescription;

    // ---------------------------- listeners stuff
    /**
     * Those are the ones that register with the PYDEV_PYEDIT_LISTENER extension point
     */
    private static List<IPyEditListener> editListeners;

    /**
     * This is the scripting engine that is binded to this interpreter.
     */
    private PyEditScripting pyEditScripting;

    public final ICallbackWithListeners<Composite> onCreatePartControl = new CallbackWithListeners<Composite>();
    public final ICallbackWithListeners<ISourceViewer> onAfterCreatePartControl = new CallbackWithListeners<ISourceViewer>();
    public final ICallbackWithListeners<PyEdit> onCreateActions = new CallbackWithListeners<PyEdit>();
    public final ICallbackWithListeners<Class<?>> onGetAdapter = new CallbackWithListeners<Class<?>>();
    public final ICallbackWithListeners<LineNumberRulerColumn> onInitializeLineNumberRulerColumn = new CallbackWithListeners<LineNumberRulerColumn>();
    public final ICallbackWithListeners<?> onDispose = new CallbackWithListeners<Object>();
    public final ICallbackWithListeners<PropertyChangeEvent> onHandlePreferenceStoreChanged = new CallbackWithListeners<PropertyChangeEvent>();
    public final ICallbackWithListeners<PySourceViewer> onCreateSourceViewer = new CallbackWithListeners<PySourceViewer>();

    public ISourceViewer getISourceViewer() {
        return getSourceViewer();
    }

    public IVerticalRuler getIVerticalRuler() {
        return getVerticalRuler();
    }

    @Override
    protected void initializeLineNumberRulerColumn(LineNumberRulerColumn rulerColumn) {
        super.initializeLineNumberRulerColumn(rulerColumn);
        this.onInitializeLineNumberRulerColumn.call(rulerColumn);
    }

    @Override
    protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
        super.handlePreferenceStoreChanged(event);
        this.onHandlePreferenceStoreChanged.call(event);
    }

    @Override
    public void createPartControl(Composite parent) {
        Composite newParent = (Composite) this.onCreatePartControl.call(parent);
        if (newParent != null) {
            parent = newParent;
        }
        super.createPartControl(parent);
        this.onAfterCreatePartControl.call(getSourceViewer());
    }

    private boolean disposed = false;
    private CodeFoldingSetter codeFoldingSetter;

    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Anyone may register to know when PyEdits are created.
     */
    public static final ICallbackWithListeners<PyEdit> onPyEditCreated = new CallbackWithListeners<PyEdit>();

    // ---------------------------- end listeners stuff

    @SuppressWarnings("unchecked")
    public PyEdit() {
        super();
        synchronized (currentlyOpenedEditorsLock) {
            currentlyOpenedEditors.add(this);
        }
        try {
            onPyEditCreated.call(this);
        } catch (Throwable e) {
            Log.log(e);
        }
        try {
            //initialize the 'save' listeners of PyEdit
            if (editListeners == null) {
                editListeners = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PYEDIT_LISTENER);
            }
            notifier.notifyEditorCreated();

            colorCache = new ColorAndStyleCache(PydevPrefs.getChainedPrefStore());

            editConfiguration = new PyEditConfiguration(colorCache, this, PydevPrefs.getChainedPrefStore());
            setSourceViewerConfiguration(editConfiguration);
            indentStrategy = editConfiguration.getPyAutoIndentStrategy(this);
            setRangeIndicator(new DefaultRangeIndicator()); // enables standard
            // vertical ruler

            //Added to set the code folding.
            this.codeFoldingSetter = new CodeFoldingSetter(this);

            CheckDefaultPreferencesDialog.askAboutSettings();

            //Ask for people to take a look in the crowdfunding for pydev:
            //http://tiny.cc/pydev-2014
            PydevShowBrowserMessage.show();
        } catch (Throwable e) {
            Log.log(e);
        }
    }

    @Override
    protected List<IPyEditListener> getAdditionalEditorListeners() {
        return editListeners;
    }

    /**
     * Overridden so that we can:
     * - Set up the cursor listener (notifies changes in the cursor position)
     * - Make the backspace handling in a way that the incremental find works (note: having the listener in the
     * textWidget does not work for that, as the event used in the IncrementalFindTarget is not the same event
     * that goes to the textWidget).
     */
    @Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        PySourceViewer viewer = (PySourceViewer) super.createSourceViewer(parent, ruler, styles);

        viewer.appendVerifyKeyListener(PyPeerLinker.createVerifyKeyListener(viewer));
        viewer.appendVerifyKeyListener(PyBackspace.createVerifyKeyListener(viewer, this));
        VerifyKeyListener createVerifyKeyListener = FirstCharAction.createVerifyKeyListener(viewer, this.getSite(),
                false);
        if (createVerifyKeyListener != null) {
            viewer.appendVerifyKeyListener(createVerifyKeyListener);
        }
        this.onCreateSourceViewer.call(viewer);

        return viewer;
    }

    /**
     * Sets the forceTabs preference for auto-indentation.
     *
     * <p>
     * This is the preference that overrides "use spaces" preference when file contains tabs (like mine do).
     * <p>
     * If the first indented line starts with a tab, then tabs override spaces.
     */
    public void resetForceTabs() {
        IDocument doc = getDocumentProvider().getDocument(getEditorInput());
        if (doc == null) {
            return;
        }

        IIndentPrefs indentPrefs = getIndentPrefs();
        if (!indentPrefs.getGuessTabSubstitution()) {
            indentPrefs.setForceTabs(false);
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
                if (text != null) {
                    if (text.startsWith("\t")) {
                        forceTabs = true;
                        break;
                    } else if (text.startsWith("  ")) {
                        forceTabs = false;
                        break;
                    }
                }
            } catch (BadLocationException e) {
                Log.log(IStatus.ERROR, "Unexpected error forcing tabs", e);
                break;
            }
            i++;
        }
        indentPrefs.setForceTabs(forceTabs);
        editConfiguration.resetIndentPrefixes();
        // display a message in the status line
        if (forceTabs) {
            updateForceTabsMessage();
        }
    }

    public void updateForceTabsMessage() {
        boolean forceTabs = getIndentPrefs().getForceTabs();
        ImageCache imageCache = PydevPlugin.getImageCache();
        ImageDescriptor desc;
        if (forceTabs) {
            desc = imageCache.getDescriptor(UIConstants.FORCE_TABS_ACTIVE);
        } else {
            desc = imageCache.getDescriptor(UIConstants.FORCE_TABS_INACTIVE);
        }
        IEditorStatusLine statusLine = (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
        if (statusLine != null) {
            statusLine.setMessage(false, forceTabs ? "Forcing tabs" : "Not forcing tabs.", desc.createImage());
        }
    }

    /**
     * @return the indentation preferences. Any action writing something should use this one in the editor because
     * we want to make sure that the indent must be the one bounded to this editor (because tabs may be forced in a given
     * editor, even if does not match the global settings).
     */
    public IIndentPrefs getIndentPrefs() {
        return indentStrategy.getIndentPrefs();
    }

    public PyAutoIndentStrategy getAutoEditStrategy() {
        return indentStrategy;
    }

    //Just making interface public
    public void resetIndentPrefixes() {
        super.updateIndentPrefixes();
    }

    /**
     * Overriden because pydev already handles spaces -> tabs
     */
    @Override
    protected void installTabsToSpacesConverter() {
        //Do nothing (pydev already handles that)
        updateIndentPrefixes();
    }

    /**
     * Overriden becaus pydev already handles spaces -> tabs
     */
    @Override
    protected void uninstallTabsToSpacesConverter() {
        //Do nothing (pydev already handles that)
        updateIndentPrefixes();
    }

    /**
     * Initializes everyone that needs document access
     *
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        try {
            super.init(site, input);

            final IDocument document = getDocument(input);

            // check the document partitioner (sanity check / fix)
            PyPartitionScanner.checkPartitionScanner(document);

            // Also adds Python nature to the project.
            // The reason this is done here is because I want to assign python
            // nature automatically to any project that has active python files.
            final IPythonNature nature = PythonNature.addNature(input);

            //we also want to initialize our shells...
            //we use 2: one for the main thread and one for the other threads.
            //just preemptively start the one for the main thread.
            final int mainThreadShellId = AbstractShell.getShellId();
            Thread thread2 = new Thread() {
                @Override
                public void run() {
                    try {
                        try {
                            AbstractShell.getServerShell(nature, mainThreadShellId);
                        } catch (RuntimeException e1) {
                        }
                    } catch (Exception e) {
                    }

                }
            };
            thread2.setName("Shell starter");
            thread2.start();

            // listen to changes in TAB_WIDTH preference
            prefListener = createPrefChangeListener(this);
            this.getIndentPrefs().addTabChangedListener(this);
            resetForceTabs();
            PydevPrefs.getChainedPrefStore().addPropertyChangeListener(prefListener);

            Runnable runnable = new Runnable() {

                public void run() {
                    try {
                        //let's do that in a thread, so that we don't have any delays in setting up the editor
                        pyEditScripting = new PyEditScripting();
                        addPyeditListener(pyEditScripting);
                    } finally {
                        //if it fails, still mark it as finished.
                        markInitFinished();
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.setName("PyEdit initializer");
            thread.start();
        } catch (Throwable e) {
            //never fail in the init
            Log.log(e);
        }
    }

    @Override
    public void onTabSettingsChanged(IIndentPrefs prefs) {
        onTabSettingsChanged(this);
    }

    private static void onTabSettingsChanged(final IPySyntaxHighlightingAndCodeCompletionEditor editor) {
        ISourceViewer sourceViewer = editor.getEditorSourceViewer();
        if (sourceViewer == null) {
            return;
        }
        IIndentPrefs indentPrefs = editor.getIndentPrefs();
        indentPrefs.regenerateIndentString();
        sourceViewer.getTextWidget().setTabs(indentPrefs.getTabWidth());
        editor.resetForceTabs();
        editor.resetIndentPrefixes();
    }

    public static IPropertyChangeListener createPrefChangeListener(
            final IPySyntaxHighlightingAndCodeCompletionEditor editor) {
        return new IPropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                try {
                    String property = event.getProperty();
                    //tab width
                    if (property.equals(PydevEditorPrefs.TAB_WIDTH)) {
                        onTabSettingsChanged(editor);

                    } else if (property.equals(PydevEditorPrefs.SUBSTITUTE_TABS)) {
                        onTabSettingsChanged(editor);

                        //auto adjust for file tabs
                    } else if (property.equals(PydevEditorPrefs.GUESS_TAB_SUBSTITUTION)) {
                        onTabSettingsChanged(editor);

                        //colors and styles
                    } else if (ColorAndStyleCache.isColorOrStyleProperty(property)) {
                        editor.getColorCache().reloadProperty(property); //all reference this cache
                        editor.getEditConfiguration().updateSyntaxColorAndStyle(); //the style needs no reloading
                        editor.getEditorSourceViewer().invalidateTextPresentation();
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        };
    }

    //Deal with notifying the user that the opened file is invalid -----------------------------------------------------

    private static final String INVALID_MODULE_MARKER_TYPE = "org.python.pydev.invalidpythonfilemarker";

    private void checkAddInvalidModuleNameMarker(IDocument doc, IFile file) {
        try {
            String name = file.getName();
            int i = name.lastIndexOf('.');
            if (i > 0) {
                String modName = name.substring(0, i);
                if (!PythonPathHelper.isValidModuleLastPart(modName)) {
                    addInvalidModuleMarker(doc, file, "Invalid name for Python module: " + modName
                            + " (it'll not be analyzed)");
                    return;

                } else if (!PythonPathHelper.isValidSourceFile(name)) {
                    addInvalidModuleMarker(doc, file, "Module: " + modName
                            + " does not have a valid Python extension (it'll not be analyzed).");
                    return;
                }
            }
            //if it still hasn't returned, remove any existing marker (i.e.: rename operation)
            removeInvalidModuleMarkers(file);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    private void removeInvalidModuleMarkers(IFile file) {
        try {
            if (file.exists()) {
                file.deleteMarkers(INVALID_MODULE_MARKER_TYPE, true, IResource.DEPTH_ZERO);
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    private void addInvalidModuleMarker(IDocument doc, IFile fileAdapter, String msg) {
        MarkerInfo markerInfo = new PyMarkerUtils.MarkerInfo(doc, msg, INVALID_MODULE_MARKER_TYPE,
                IMarker.SEVERITY_WARNING, false, true, 0, 0, 0, 0, null);
        ArrayList<MarkerInfo> lst = new ArrayList<MarkerInfo>();
        lst.add(markerInfo);
        PyMarkerUtils.replaceMarkers(lst, fileAdapter, INVALID_MODULE_MARKER_TYPE, true, new NullProgressMonitor());
    }

    /**
     * When we have the editor input re-set, we have to change the parser and the partition scanner to
     * the new document. This happens in 3 cases:
     * - when the editor has been created
     * - when the editor is reused in the search window
     * - when we create a file, and make a save as, to change its name
     *
     * there were related bugs in each of these cases:
     * https://sourceforge.net/tracker/?func=detail&atid=577329&aid=1250307&group_id=85796
     * https://sourceforge.net/tracker/?func=detail&atid=577329&aid=1251271&group_id=85796
     *
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#doSetInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {

        IEditorInput oldInput = this.getEditorInput();

        //Remove markers from the old
        if (oldInput != null) {
            IFile oldFile = oldInput.getAdapter(IFile.class);
            if (oldFile != null) {
                removeInvalidModuleMarkers(oldFile);
            }
        }

        synchronized (lockHandle) {
            releaseCurrentHandle();
        }

        super.doSetInput(input);
        try {
            IDocument document = getDocument(input);
            if (input != null) {
                IFile newFile = input.getAdapter(IFile.class);
                if (newFile != null) {
                    //Add invalid module name markers to the new.
                    checkAddInvalidModuleNameMarker(document, newFile);
                }

                //see if we have to change the encoding of the file on load
                fixEncoding(input, document);

                PyParserManager.getPyParserManager(PydevPrefs.getPreferences()).attachParserTo(this);
                if (document != null) {
                    PyPartitionScanner.checkPartitionScanner(document, this.getGrammarVersionProvider());
                }
            }

            notifier.notifyInputChanged(oldInput, input);
            notifier.notifyOnSetDocument(document);
        } catch (Throwable e) {
            Log.log(e);
        }
        try {
            PyEditTitle.invalidateTitle(this, input);
        } catch (Throwable e) {
            Log.log(e);
        }

        try {
            if (this.isCythonFile()) {
                this.setTitleImage(PydevPlugin.getImageCache().get(UIConstants.CYTHON_FILE_ICON));
                this.getAutoEditStrategy().setCythonFile(true);
            } else {
                this.getAutoEditStrategy().setCythonFile(false);
            }
        } catch (Throwable e) {
            Log.log(e);
        }
    }

    /* default */void setEditorTitle(String title) {
        setPartName(title);
        firePropertyChange(PROP_DIRTY);
    }

    /* default */void setEditorImage(Image image) {
        setTitleImage(image);
    }

    /**
     * @param input the input from where we want to get the document
     * @return the document for the passed input
     */
    private IDocument getDocument(final IEditorInput input) {
        return getDocumentProvider().getDocument(input);
    }

    /**
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#performSave(boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
        final IDocument document = getDocument();

        boolean keepOn;
        try {
            keepOn = true;
            if (PydevSaveActionsPrefPage.getAutoformatOnlyWorkspaceFiles(this)) {
                if (getIFile() == null) { //not a workspace file and user has chosen to only auto-format workspace files.
                    keepOn = false;
                }
            }
        } catch (Exception e1) {
            Log.log(e1);
            // Shouldn't happen: let's skip the save actions...
            keepOn = false;
        }

        // Save actions before code-formatting (so that we apply the formatting to it afterwards).
        try {
            if (keepOn) {
                executeSaveActions(document);
            }
        } catch (final Throwable e) {
            Log.log(e);
        }

        //Before saving, let's see if the auto-code formatting is turned on.
        try {

            //TODO CYTHON: support code-formatter.
            if (keepOn && PydevSaveActionsPrefPage.getFormatBeforeSaving(this) && !isCythonFile()) {
                IStatusLineManager statusLineManager = this.getStatusLineManager();
                IDocumentProvider documentProvider = getDocumentProvider();
                int[] regionsForSave = null;

                if (PyCodeFormatterPage.getFormatOnlyChangedLines(this)) {
                    if (documentProvider instanceof PyDocumentProvider) {
                        PyDocumentProvider pyDocumentProvider = (PyDocumentProvider) documentProvider;
                        ITextFileBuffer fileBuffer = pyDocumentProvider.getFileBuffer(getEditorInput());
                        if (fileBuffer != null) {
                            regionsForSave = ChangedLinesComputer.calculateChangedLines(fileBuffer,
                                    progressMonitor == null ? new NullProgressMonitor() : progressMonitor);
                        }
                    } else {
                        Log.log("Was expecting PyDocumentProvider. Found: " + documentProvider);
                    }
                }

                if (regionsForSave == null || regionsForSave.length > 0) {
                    //Note: auto-format should only take place if we're always formatting everything or
                    //if we have some region to update (regionsForSave.length == 0 means that we only
                    //had deleted lines, in which case we can't really do anything).
                    ITextSelection selection = (ITextSelection) this.getSelectionProvider().getSelection();
                    PySelection ps = new PySelection(document, selection);

                    if (!hasSyntaxError(ps.getDoc())) {
                        PyFormatStd std = new PyFormatStd();
                        boolean throwSyntaxError = true;
                        try {
                            std.applyFormatAction(this, ps, regionsForSave, throwSyntaxError,
                                    this.getSelectionProvider());
                            statusLineManager.setErrorMessage(null);
                        } catch (SyntaxErrorException e) {
                            statusLineManager.setErrorMessage(e.getMessage());
                        }
                    }
                }
            }
        } catch (Throwable e) {
            //can never fail
            Log.log(e);
        }

        try {
            fixEncoding(getEditorInput(), document);
        } catch (Throwable e) {
            //can never fail
            Log.log(e);
        }

        //will provide notifications
        super.performSave(overwrite, progressMonitor);
    }

    private void executeSaveActions(IDocument document) throws BadLocationException {
        if (PydevSaveActionsPrefPage.getDateFieldActionEnabled(this)) {
            final String contents = document.get();
            final String fieldName = PydevSaveActionsPrefPage.getDateFieldName(this);
            final String fieldPattern = String
                    .format("^%s(\\s*)=(\\s*[ur]{0,2}['\"]{1,3})(.+?)(['\"]{1,3})", fieldName);
            final Pattern pattern = Pattern.compile(fieldPattern, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(contents);
            if (matcher.find()) {
                final MatchResult matchResult = matcher.toMatchResult();
                if (matchResult.groupCount() == 4) {
                    final String spBefore = matchResult.group(1);
                    final String spAfterQuoteBegin = matchResult.group(2);
                    final String dateStr = matchResult.group(3);
                    final String quoteEnd = matchResult.group(4);
                    final String dateFormat = PydevSaveActionsPrefPage.getDateFieldFormat(this);
                    final Date nowDate = new Date();
                    final SimpleDateFormat ft = new SimpleDateFormat(dateFormat);
                    try {
                        final Date fieldDate = ft.parse(dateStr);
                        // don't touch future dates
                        if (fieldDate.before(nowDate)) {
                            final String newDateStr = ft.format(nowDate);
                            final String replacement = fieldName + spBefore + "=" + spAfterQuoteBegin + newDateStr
                                    + quoteEnd;
                            document.replace(matchResult.start(), matchResult.end() - matchResult.start(), replacement);
                        }
                    } catch (final java.text.ParseException pe) {
                        // do nothing
                    }
                }
            }
        }

        if (PydevSaveActionsPrefPage.getSortImportsOnSave(this)) {
            boolean automatic = true;
            PyOrganizeImports organizeImports = new PyOrganizeImports(automatic);
            try {
                organizeImports.formatAll(getDocument(), this, getIFile(), true, true);
            } catch (SyntaxErrorException e) {
                Log.log(e);
            }
        }
    }

    @Override
    protected BaseParserManager getParserManager() {
        return PyParserManager.getPyParserManager(null);
    }

    /**
     * Checks if there's a syntax error at the document... if there is, returns false.
     *
     * Note: This function will also set the status line error message if there's an error message.
     * Note: This function will actually do a parse operation when called (so, it should be called with care).
     */
    public boolean hasSyntaxError(IDocument doc) throws MisconfigurationException {
        ParseOutput reparse = PyParser.reparseDocument(new PyParser.ParserInfo(doc, this, false));
        if (reparse.error != null) {
            this.getStatusLineManager().setErrorMessage(reparse.error.getMessage());
            return true;
        }
        return false;
    }

    /**
     * Just to make it public.
     */
    @Override
    public void doSave(IProgressMonitor progressMonitor) {
        super.doSave(progressMonitor);
    }

    /**
     * Forces the encoding to the one specified in the file
     *
     * @param input
     * @param document
     */
    private void fixEncoding(final IEditorInput input, IDocument document) {
        if (input instanceof FileEditorInput) {
            final IFile file = ((FileEditorInput) input).getAdapter(IFile.class);
            try {
                final String encoding = FileUtilsFileBuffer.getPythonFileEncoding(document, file.getFullPath()
                        .toOSString());
                if (encoding != null) {
                    try {
                        if (encoding.equals(file.getCharset()) == false) {

                            new Job("Change encoding") {

                                @Override
                                protected IStatus run(IProgressMonitor monitor) {
                                    try {
                                        file.setCharset(encoding, monitor);
                                        ((TextFileDocumentProvider) getDocumentProvider()).setEncoding(input, encoding);
                                        //refresh it...
                                        file.refreshLocal(IResource.DEPTH_INFINITE, null);
                                    } catch (CoreException e) {
                                        Log.log(e);
                                    }
                                    return Status.OK_STATUS;
                                }

                            }.schedule();
                        }
                    } catch (CoreException e) {
                        Log.log(e);
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * @return the File being edited
     */
    @Override
    public File getEditorFile() {
        File editorFile = super.getEditorFile();
        if (editorFile == null) {
            IEditorInput editorInput = this.getEditorInput();
            if (editorInput instanceof PydevFileEditorInput) {
                PydevFileEditorInput pyEditorInput = (PydevFileEditorInput) editorInput;
                return pyEditorInput.getPath().toFile();
            }
        }
        return editorFile;
    }

    // cleanup
    @Override
    public void dispose() {
        synchronized (lockHandle) {
            releaseCurrentHandle();
        }
        if (!this.disposed) {
            this.disposed = true;

            synchronized (currentlyOpenedEditorsLock) {
                currentlyOpenedEditors.remove(this);
            }
            this.outlinePage = null;
            this.codeFoldingSetter = null;

            try {
                IFile iFile = this.getIFile();
                if (iFile != null) {
                    removeInvalidModuleMarkers(iFile);
                }
            } catch (Throwable e1) {
                Log.log(e1);
            }

            try {
                this.onDispose.call(null);

                notifier.notifyOnDispose();

                PydevPrefs.getChainedPrefStore().removePropertyChangeListener(prefListener);
                PyParserManager.getPyParserManager(null).notifyEditorDisposed(this);

                colorCache.dispose();
                pyEditScripting = null;
                cache.clear();
                cache = null;

                if (this.resourceManager != null) {
                    this.resourceManager.dispose();
                    this.resourceManager = null;
                }

                synchronized (registeredEditListeners) {
                    registeredEditListeners.clear();
                }

            } catch (Throwable e) {
                Log.log(e);
            }
        }
        super.dispose();

    }

    public static class MyResources extends ListResourceBundle {
        @Override
        public Object[][] getContents() {
            return contents;
        }

        static final Object[][] contents = { { "CorrectionAssist", "CorrectionAssist" },
                { "ContentAssistProposal", "ContentAssistProposal" }, { "TemplateProposals", "TemplateProposals" }, };
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#createActions()
     *
     * TODO: Fix content assist to work in emacs mode:
     * http://wiki.eclipse.org/index.php/FAQ_How_do_I_add_Content_Assist_to_my_editor%3F
     * http://www.eclipse.org/newsportal/article.php?id=61744&group=eclipse.platform#61744
     */
    @Override
    protected void createActions() {
        super.createActions();
        try {
            MyResources resources = new MyResources();
            IAction action;

            //Quick-Assist: it's added to the platform as of Eclipse 3.2, so, we do not have to put the binding here

            // -------------------------------------------------------------------------------------
            // This action will fire a CONTENTASSIST_PROPOSALS operation
            // when executed
            action = new ContentAssistAction(resources, "ContentAssistProposal.", this);
            action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
            setAction("ContentAssistProposal", action);
            markAsStateDependentAction("ContentAssistProposal", true);

            // -------------------------------------------------------------------------------------
            //open action
            IAction openAction = new PyOpenAction();
            setAction(ACTION_OPEN, openAction);

            // -------------------------------------------------------------------------------------
            // Offline action
            action = new OfflineAction(resources, "Pyedit.ScriptEngine.", this);
            action.setActionDefinitionId("org.python.pydev.editor.actions.scriptEngine");
            action.setId("org.python.pydev.editor.actions.scriptEngine");
            setAction("PyDevScriptEngine", action);

            // -------------------------------------------------------------------------------------
            //move lines
            if (this.getIndentPrefs().getSmartLineMove()) {
                //Don't even bind the action if the smart line move is not set.
                //This means 2 things:
                //- Uses the default action when asked
                //- An editor restart will be needed to have it applied
                action = new PyMoveLineUpAction(resources, "Pyedit.MoveLinesUp.", this);
                action.setActionDefinitionId(ITextEditorActionDefinitionIds.MOVE_LINES_UP);
                action.setId("org.python.pydev.editor.actions.moveLineUp");
                setAction(ITextEditorActionConstants.MOVE_LINE_UP, action);

                action = new PyMoveLineDownAction(resources, "Pyedit.MoveLinesDown.", this);
                action.setActionDefinitionId(ITextEditorActionDefinitionIds.MOVE_LINES_DOWN);
                action.setId("org.python.pydev.editor.actions.moveLineDown");
                setAction(ITextEditorActionConstants.MOVE_LINE_DOWN, action);
            }

            notifier.notifyOnCreateActions(resources);
            onCreateActions.call(this);
        } catch (Throwable e) {
            Log.log(e);
        }
    }

    @Override
    protected void initializeKeyBindingScopes() {
        setKeyBindingScopes(new String[] { PYDEV_EDITOR_KEYBINDINGS_CONTEXT_ID });
    }

    /**
     * Used to: request a reparse / add listener / remove listener
     * @return the parser that is being used in this editor.
     */
    public PyParser getParser() {
        return (PyParser) PyParserManager.getPyParserManager(null).getParser(this);
    }

    /**
     * Returns the status line manager of this editor.
     * @return the status line manager of this editor
     * @since 2.0
     *
     * copied from superclass, as it is private there...
     */
    @Override
    public IStatusLineManager getStatusLineManager() {
        return EditorUtils.getStatusLineManager(this);
    }

    /**
     * This is the 'offline' action
     */
    protected OfflineActionTarget fOfflineActionTarget;
    private PyOutlinePage outlinePage;

    /**
     * @return an outline view
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        if (OfflineActionTarget.class.equals(adapter)) {
            if (fOfflineActionTarget == null) {
                IStatusLineManager manager = getStatusLineManager();
                if (manager != null) {
                    fOfflineActionTarget = (getSourceViewer() == null ? null : new OfflineActionTarget(
                            getSourceViewer(), manager, this));
                }
            }
            return fOfflineActionTarget;
        }

        if (IProject.class.equals(adapter)) {
            return this.getProject();
        }

        if (ICodeScannerKeywords.class.equals(adapter)) {
            return new PyEditBasedCodeScannerKeywords(this);
        }

        if (IContentOutlinePage.class.equals(adapter)) {
            return getOutlinePage();
        } else {

            Object adaptable = this.onGetAdapter.call(adapter);
            if (adaptable != null) {
                return adaptable;
            }

            return super.getAdapter(adapter);
        }
    }

    @Override
    public IOutlineModel createOutlineModel() {
        return new ParsedModel(this);
    }

    private IContentOutlinePage getOutlinePage() {
        if (this.outlinePage == null) {
            this.outlinePage = new PyOutlinePage(this);
        }
        return this.outlinePage;
    }

    @Override
    public void setSelection(int offset, int length) {
        super.setSelection(offset, length);
    }

    /**
     * Selects more than one node, making a selection from the 1st node to the last node passed.
     */
    @Override
    public void revealModelNodes(ISimpleNode[] nodes) {
        if (nodes == null) {
            return; // nothing to see here
        }

        IDocument document = getDocumentProvider().getDocument(getEditorInput());
        if (document == null) {
            return;
        }

        try {
            int startOffset = -1, endOffset = -1;
            PySelection selection = new PySelection(this);

            for (ISimpleNode inode : nodes) {
                SimpleNode node = (SimpleNode) inode;
                int nodeStartoffset = selection.getLineOffset(node.beginLine - 1) + node.beginColumn - 1;
                int[] colLineEnd = NodeUtils.getColLineEnd(node);

                int nodeEndOffset = selection.getLineOffset(colLineEnd[0] - 1) + colLineEnd[1] - 1;

                if (startOffset == -1 || nodeStartoffset < startOffset) {
                    startOffset = nodeStartoffset;
                }
                if (endOffset == -1 || nodeEndOffset > endOffset) {
                    endOffset = nodeEndOffset;
                }
            }

            setSelection(startOffset, endOffset - startOffset);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * Shows some node in the editor.
     * @param node the node to be shown.
     */
    public void revealModelNode(SimpleNode node) {
        if (node == null) {
            return; // nothing to see here
        }

        IDocument document = getDocumentProvider().getDocument(getEditorInput());
        if (document == null) {
            return;
        }

        int offset, length, endOffset;

        try {
            PySelection selection = new PySelection(this);
            offset = selection.getLineOffset(node.beginLine - 1) + node.beginColumn - 1;
            int[] colLineEnd = NodeUtils.getColLineEnd(node);

            endOffset = selection.getLineOffset(colLineEnd[0] - 1) + colLineEnd[1] - 1;
            length = endOffset - offset;
            setSelection(offset, length);
        } catch (Exception e) {
            Log.log(e);
        }

    }

    private Tuple3<Integer, IModulesManager, String> handle;
    private final Object lockHandle = new Object();

    /**
     * Note that the lockHandle must be already synched before this method is called.
     */
    private void releaseCurrentHandle() {
        if (this.handle != null) {
            this.handle.o2.popTemporaryModule(this.handle.o3, this.handle.o1);
            this.handle = null;
        }
    }

    /**
     * This event comes when document was parsed (with or without errors)
     *
     * Removes all the error markers
     */
    @Override
    public void parserChanged(ChangedParserInfoForObservers info) {

        if (info.errorInfo != null) {
            errorDescription = PyParser.createParserErrorMarkers(info.errorInfo.error, info.file, info.doc);
        } else {
            errorDescription = null;
        }

        ast = (SimpleNode) info.root;
        astModificationTimeStamp = info.docModificationStamp;

        try {
            IPythonNature pythonNature = this.getPythonNature();
            if (pythonNature != null) {
                ICodeCompletionASTManager astManager = pythonNature.getAstManager();
                if (astManager != null) {
                    IModulesManager modulesManager = astManager.getModulesManager();
                    if (modulesManager != null) {
                        File editorFile = this.getEditorFile();
                        if (editorFile != null) {
                            String moduleName = pythonNature.resolveModule(editorFile);
                            if (moduleName != null) {
                                synchronized (lockHandle) {
                                    releaseCurrentHandle();
                                    int modHandle = modulesManager.pushTemporaryModule(moduleName, new SourceModule(
                                            moduleName, editorFile, ast, null));

                                    this.handle = new Tuple3<Integer, IModulesManager, String>(modHandle,
                                            modulesManager, moduleName);
                                }
                            }
                        }
                    }
                }
            }
        } catch (MisconfigurationException e) {
            Log.log(e);
        }

        fireModelChanged(ast);
        invalidateTextPresentationAsync();
    }

    private void invalidateTextPresentationAsync() {
        //Trying to fix issue where it seems that the text presentation is not properly updated after markers are
        //changed (i.e.: red lines remain there when they shouldn't).
        //I couldn't really reproduce this issue, so, this may not fix it...
        //
        //Details: https://sourceforge.net/projects/pydev/forums/forum/293649/topic/4477776
        RunInUiThread.async(new Runnable() {

            public void run() {
                if (!isDisposed()) {
                    getSourceViewer().invalidateTextPresentation();
                }
            }
        });
    }

    @Override
    public void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc, long docModificationStamp) {
        throw new AssertionError("Implementing IParserObserver3: this should not be called anymore");
    }

    /**
     * This event comes when parse ended in an error
     *
     * Generates an error marker on the document
     */
    @Override
    public void parserError(Throwable error, IAdaptable original, IDocument doc) {
        throw new AssertionError("Implementing IParserObserver3: this should not be called anymore");
    }

    @Override
    public void parserError(ErrorParserInfoForObservers info) {
        //Note: if the ast was not generated, just the error, we have to make sure we're properly set
        //(even if it was set in the ast too).
        errorDescription = PyParser.createParserErrorMarkers(info.error, info.file, info.doc);

        fireParseErrorChanged(errorDescription);
    }

    /**
     * @return the last ast generated in this editor (even if we had some other error after that)
     * Note: could be null!
     */
    public SimpleNode getAST() {
        return ast;
    }

    public long getAstModificationTimeStamp() {
        return astModificationTimeStamp;
    }

    /**
     * @return a list of tuples, where the 1st element in the tuple is a String and the 2nd element is
     * an icon, ordered so that the 1st item is the topmost and the last is the innermost.
     */
    public List<String[]> getInnerStructureFromLine(int line) {
        ArrayList<String[]> ret = new ArrayList<String[]>();

        List<stmtType> parseToKnowGloballyAccessiblePath = FastParser.parseToKnowGloballyAccessiblePath(getDocument(),
                line);

        for (stmtType stmtType : parseToKnowGloballyAccessiblePath) {
            String rep = NodeUtils.getRepresentationString(stmtType);
            String image;
            if (stmtType instanceof ClassDef) {
                image = UIConstants.CLASS_ICON;
            } else if (stmtType instanceof FunctionDef) {
                image = UIConstants.METHOD_ICON;
            } else {
                image = UIConstants.ERROR;
            }
            ret.add(new String[] { rep, image });
        }

        return ret;
    }

    /**
     * This function will open an editor given the passed parameters
     *
     * @param projectName
     * @param path
     * @param innerStructure
     * @throws MisconfigurationException
     */
    public static void openWithPathAndInnerStructure(String projectName, IPath path, List<String> innerStructure)
            throws MisconfigurationException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject project = workspace.getRoot().getProject(projectName);
        if (project != null) {
            IFile file = project.getFile(path);
            if (file != null) {
                IEditorPart editor = PyOpenEditor.doOpenEditor(file);
                if (editor instanceof PyEdit) {
                    PyEdit pyEdit = (PyEdit) editor;
                    IPythonNature nature = pyEdit.getPythonNature();
                    AbstractModule mod = AbstractModule.createModuleFromDoc(nature.resolveModule(file), file
                            .getLocation().toFile(), pyEdit.getDocument(), nature, false);

                    StringBuffer tok = new StringBuffer(80);
                    for (String s : innerStructure) {
                        if (tok.length() > 0) {
                            tok.append('.');
                        }
                        tok.append(s);
                    }

                    try {
                        IDefinition[] definitions = mod.findDefinition(CompletionStateFactory.getEmptyCompletionState(
                                tok.toString(), nature, new CompletionCache()), -1, -1, nature);
                        List<ItemPointer> pointers = new ArrayList<ItemPointer>();
                        PyRefactoringFindDefinition.getAsPointers(pointers, definitions);
                        if (pointers.size() > 0) {
                            new PyOpenAction().run(pointers.get(0));
                        }
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
            }
        }
    }

    /**
     * @return the last error description found (may be null)
     */
    public ErrorDescription getErrorDescription() {
        return errorDescription;
    }

    /**
     * Only used if we weren't able
     */
    public int getGrammarVersion() throws MisconfigurationException {
        if (isCythonFile()) {
            return IPythonNature.GRAMMAR_PYTHON_VERSION_CYTHON;
        }
        IPythonNature nature;
        nature = getPythonNature();
        if (nature != null) {
            return nature.getGrammarVersion();
        }
        File editorFile = getEditorFile();
        if (editorFile == null) {
            throw new MisconfigurationException();
        }
        Tuple<IPythonNature, String> infoForFile = PydevPlugin.getInfoForFile(editorFile);
        if (infoForFile == null || infoForFile.o1 == null) {
            throw new MisconfigurationException();
        }
        return infoForFile.o1.getGrammarVersion();
    }

    public IGrammarVersionProvider getGrammarVersionProvider() {
        return new IGrammarVersionProvider() {

            public int getGrammarVersion() throws MisconfigurationException {
                //Always calculate at the present time based on the editor configuration.
                return PyEdit.this.getGrammarVersion();
            }
        };
    }

    public boolean isCythonFile() {
        IFile iFile = getIFile();
        String fileName = null;
        if (iFile != null) {
            fileName = iFile.getName();
        } else {
            File editorFile = getEditorFile();
            if (editorFile != null) {
                fileName = editorFile.getName();
            }
        }
        return FileTypesPreferencesPage.isCythonFile(fileName);
    }

    /**
     * @return the python nature associated with this editor.
     * @throws NotConfiguredInterpreterException
     */
    public IPythonNature getPythonNature() throws MisconfigurationException {
        IProject project = getProject();
        if (project == null || !project.isOpen()) {
            return null;
        }
        IPythonNature pythonNature = PythonNature.getPythonNature(project);
        if (pythonNature != null) {
            return pythonNature;
        }

        //if it's an external file, there's the possibility that it won't be added even here.
        pythonNature = PythonNature.addNature(this.getEditorInput());

        if (pythonNature != null) {
            return pythonNature;
        }

        File editorFile = getEditorFile();
        if (editorFile == null) {
            return null;
        }
        Tuple<IPythonNature, String> infoForFile = PydevPlugin.getInfoForFile(editorFile);
        if (infoForFile == null) {
            NotConfiguredInterpreterException e = new NotConfiguredInterpreterException();
            ErrorDialog.openError(EditorUtils.getShell(), "Error: no interpreter configured",
                    "Interpreter not configured\n(Please, Configure it under window->preferences->PyDev)",
                    PydevPlugin.makeStatus(IStatus.ERROR, e.getMessage(), e));
            throw e;

        }
        pythonNature = infoForFile.o1;
        return pythonNature;
    }

    @Override
    protected void initializeEditor() {
        super.initializeEditor();
        try {
            this.setPreferenceStore(PydevPrefs.getChainedPrefStore());
            setEditorContextMenuId(PY_EDIT_CONTEXT);
            setRulerContextMenuId(PY_EDIT_RULER_CONTEXT);
            setDocumentProvider(PyDocumentProvider.instance);
        } catch (Throwable e) {
            Log.log(e);
        }
    }

    //------------------------------------------------------------------- START: actions that are activated after Ctrl+2
    OfflineActionsManager offlineActionsManager = new OfflineActionsManager();

    public Collection<ActionInfo> getOfflineActionDescriptions() {
        return offlineActionsManager.getOfflineActionDescriptions();
    }

    public void addOfflineActionListener(String key, IAction action) {
        offlineActionsManager.addOfflineActionListener(key, action);
    }

    public void addOfflineActionListener(String key, IAction action, String description, boolean needsEnter) {
        offlineActionsManager.addOfflineActionListener(key, action, description, needsEnter);
    }

    public boolean activatesAutomaticallyOn(String key) {
        return offlineActionsManager.activatesAutomaticallyOn(key);
    }

    public boolean hasOfflineAction(String key) {
        return offlineActionsManager.hasOfflineAction(key);
    }

    /**
     * @return if an action was binded and was successfully executed
     */
    public boolean onOfflineAction(String requestedStr, OfflineActionTarget target) {
        return offlineActionsManager.onOfflineAction(requestedStr, target);
    }

    private LocalResourceManager resourceManager;

    public synchronized LocalResourceManager getResourceManager() {
        if (resourceManager == null) {
            resourceManager = new LocalResourceManager(JFaceResources.getResources());
        }
        return resourceManager;
    }

    /**
     * Used in the script pyedit_list_bindings.py
     */
    public Font getFont(FontData descriptor) throws DeviceResourceException {
        Font font = getResourceManager().createFont(FontDescriptor.createFrom(descriptor));

        //        Old implementation (for Eclipse 3.3)
        //        Font font = (Font) SWTResourceUtil.getFontTable().get(descriptor);
        //        if (font == null) {
        //            font = new Font(Display.getCurrent(), descriptor);
        //            SWTResourceUtil.getFontTable().put(descriptor, font);
        //        }
        return font;
    }

    //--------------------------------------------------------------------- END: actions that are activated after Ctrl+2

    public static void checkValidateState(IEditorPart iEditorPart) {
        if (iEditorPart instanceof ITextEditorExtension2) {
            ITextEditorExtension2 editor = (ITextEditorExtension2) iEditorPart;
            editor.validateEditorInputState();

        }
    }

    public static Object iterOpenEditorsUntilFirstReturn(ICallback<Object, PyEdit> callback) {
        HashSet<PyEdit> hashSet;
        synchronized (currentlyOpenedEditorsLock) {
            hashSet = new HashSet<>(currentlyOpenedEditors);
        }
        // Iterate in unsynchronized copy
        for (PyEdit edit : hashSet) {
            Object ret = callback.call(edit);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static boolean isEditorOpenForResource(IResource r) {
        HashSet<PyEdit> hashSet;
        synchronized (currentlyOpenedEditorsLock) {
            hashSet = new HashSet<>(currentlyOpenedEditors);
        }
        // Iterate in unsynchronized copy
        for (PyEdit edit : hashSet) {
            IEditorInput input = edit.getEditorInput();
            if (input != null) {
                Object adapter = input.getAdapter(IResource.class);
                if (adapter != null && r.equals(adapter)) {
                    return true;
                }
            }
        }
        return false;
    }

    public FormatStd getFormatStd() {
        return PyFormatStd.getFormat(this);
    }

    /**
     * Important: keep for scripting
     */
    public void setMessage(boolean error, String message) {
        IEditorStatusLine statusLine = (IEditorStatusLine) this.getAdapter(IEditorStatusLine.class);
        statusLine.setMessage(error, message, null);
    }

    /**
     * Important: keep for scripting
     */
    public void showInformationDialog(String title, String message) {
        MessageDialog.openInformation(getSite().getShell(), title, message);
    }

    /**
     * Important: keep for scripting
     */
    public int getPrintMarginColums() {
        return PydevPrefs.getChainedPrefStore()
                .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
    }

    /**
     * Important: keep for scripting
     */
    public void asyncExec(Runnable runnable) {
        RunInUiThread.async(runnable);
    }

    /**
     * Important: keep for scripting
     */
    public Class<Action> getActionClass() {
        return Action.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<IPyCompletionProposal> getIPyCompletionProposalClass() {
        return IPyCompletionProposal.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<PyCompletionProposal> getPyCompletionProposalClass() {
        return PyCompletionProposal.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<UIConstants> getUIConstantsClass() {
        return UIConstants.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<ScriptConsole> getScriptConsoleClass() {
        return ScriptConsole.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<Display> getDisplayClass() {
        return Display.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<Runnable> getRunnableClass() {
        return Runnable.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<PySelection> getPySelectionClass() {
        return PySelection.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<UIJob> getUIJobClass() {
        return UIJob.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<IDocumentListener> getIDocumentListenerClass() {
        return IDocumentListener.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<PythonCorrectionProcessor> getPythonCorrectionProcessorClass() {
        return PythonCorrectionProcessor.class;
    }

    /**
     * Important: keep for scripting
     */
    public Class<IExecuteLineAction> getIExecuteLineActionClass() {
        return IExecuteLineAction.class;
    }

    /**
     * Important: keep for scripting
     */
    public IStatus getOkStatus() {
        return Status.OK_STATUS;
    }

    @Override
    public String toString() {
        return "PyEdit[" + getEditorFile() + "]";
    }

    @Override
    public ICharacterPairMatcher2 getPairMatcher() {
        return new PythonPairMatcher();
    }

    @Override
    public IScopesParser createScopesParser() {
        return new ScopesParser();
    }
}
