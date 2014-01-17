/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Jul 25, 2003
 */
package org.python.pydev.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.parser.IPyParser;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.grammar24.PythonGrammar24;
import org.python.pydev.parser.grammar25.PythonGrammar25;
import org.python.pydev.parser.grammar26.PythonGrammar26;
import org.python.pydev.parser.grammar27.PythonGrammar27;
import org.python.pydev.parser.grammar30.PythonGrammar30;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.model.ErrorDescription;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.out_of_memory.OnExpectedOutOfMemory;
import org.python.pydev.shared_core.parsing.BaseParser;
import org.python.pydev.shared_core.parsing.ChangedParserInfoForObservers;
import org.python.pydev.shared_core.parsing.ErrorParserInfoForObservers;
import org.python.pydev.shared_core.parsing.IParserObserver;
import org.python.pydev.shared_core.parsing.IParserObserver2;
import org.python.pydev.shared_core.parsing.IParserObserver3;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.LowMemoryArrayList;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;

/**
 * PyParser uses org.python.parser to parse the document (lexical analysis) It
 * is attached to PyEdit (a view), and it listens to document changes On every
 * document change, the syntax tree is regenerated The reparsing of the document
 * is done on a ParsingThread
 *
 * Clients that need to know when new parse tree has been generated should
 * register as parseListeners.
 */

@SuppressWarnings("restriction")
public class PyParser extends BaseParser implements IPyParser {

    /**
     * Just for tests: show whenever we're not able to parse some file.
     */
    public static boolean DEBUG_SHOW_PARSE_ERRORS = false;

    /**
     * Defines whether we should use the fast stream or not
     */
    public static boolean USE_FAST_STREAM = true;

    /**
     * used to enable tracing in the grammar
     */
    public static boolean ENABLE_TRACING = false;

    /**
     * This is the version of the grammar that should be used for this parser
     */
    private final IGrammarVersionProvider grammarVersionProvider;

    public static String getGrammarVersionStr(int grammarVersion) {
        if (grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4) {
            return "grammar: Python 2.4";

        } else if (grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5) {
            return "grammar: Python 2.5";

        } else if (grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6) {
            return "grammar: Python 2.6";

        } else if (grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7) {
            return "grammar: Python 2.7";

        } else if (grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
            return "grammar: Python 3.x";

        } else if (grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_CYTHON) {
            return "grammar: Cython";

        } else {
            return "grammar: unrecognized: " + grammarVersion;
        }
    }

    public int getGrammarVersion() throws MisconfigurationException {
        return grammarVersionProvider.getGrammarVersion();
    }

    /**
     * Should only be called for testing. Does not register as a thread.
     */
    public PyParser(IGrammarVersionProvider grammarVersionProvider) {
        super(PyParserManager.getPyParserManager(new PreferenceStore()));
        if (grammarVersionProvider == null) {
            grammarVersionProvider = new IGrammarVersionProvider() {
                public int getGrammarVersion() {
                    return IPythonNature.LATEST_GRAMMAR_VERSION;
                }
            };
        }
        this.grammarVersionProvider = grammarVersionProvider;
    }

    /**
     * Ok, create the parser for an editor
     *
     * @param editorView
     */
    public PyParser(IPyEdit editorView) {
        this(getGrammarProviderFromEdit(editorView));
    }

    /**
     * @param editorView this is the editor that we're getting in the parser
     * @return a provider signaling the grammar to be used for the parser.
     * @throws MisconfigurationException
     */
    private static IGrammarVersionProvider getGrammarProviderFromEdit(IPyEdit editorView) {
        return editorView.getGrammarVersionProvider();
    }

    @Override
    public void notifySaved() {
        //force parse on save
        forceReparse();
    }

    /**
     * @return false if we asked a reparse and it will not be scheduled because a reparse is already in action.
     */
    @Override
    public boolean forceReparse(Object... argsToReparse) {
        if (disposed) {
            return true; //reparse didn't happen, but no matter what happens, it won't happen anyways
        }
        return scheduler.parseNow(true, argsToReparse);
    }

    public static interface IPostParserListener {

        public void participantsNotified(Object... argsToReparse);
    }

    private final List<IPostParserListener> postParserListeners = new LowMemoryArrayList<>();
    private final Object lockPostParserListeners = new Object();

    public void addPostParseListener(IPostParserListener iParserObserver) {
        synchronized (lockPostParserListeners) {
            postParserListeners.add(iParserObserver);
        }
    }

    public void removePostParseListener(IPostParserListener iPostParserListener) {
        synchronized (lockPostParserListeners) {
            postParserListeners.remove(iPostParserListener);
        }
    }

    /**
     * stock listener implementation event is fired whenever we get a new root
     * @param original
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void fireParserChanged(ChangedParserInfoForObservers info) {
        super.fireParserChanged(info);

        List<IParserObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PARSER_OBSERVER);
        for (IParserObserver observer : participants) {
            try {
                if (observer instanceof IParserObserver3) {
                    ((IParserObserver3) observer).parserChanged(info);

                } else if (observer instanceof IParserObserver2) {
                    ((IParserObserver2) observer).parserChanged(info.root, info.file, info.doc, info.argsToReparse);

                } else {
                    observer.parserChanged(info.root, info.file, info.doc, info.docModificationStamp);
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * stock listener implementation event is fired when parse fails
     * @param original
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void fireParserError(ErrorParserInfoForObservers info) {
        super.fireParserError(info);
        List<IParserObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PARSER_OBSERVER);
        for (IParserObserver observer : participants) {
            if (observer instanceof IParserObserver3) {
                ((IParserObserver3) observer).parserError(info);

            } else if (observer instanceof IParserObserver2) {
                ((IParserObserver2) observer).parserError(info.error, info.file, info.doc, info.argsToReparse);

            } else {
                observer.parserError(info.error, info.file, info.doc);
            }
        }
    }

    /**
     * Parses the document, generates error annotations
     *
     * @param argsToReparse: will be passed to fireParserError / fireParserChanged so that the IParserObserver2
     * can check it. This is useful when the reparse was done with some specific thing in mind, so that its requestor
     * can pass some specific thing to the parser observers
     *
     * @return a tuple with the SimpleNode root(if parsed) and the error (if any).
     *         if we are able to recover from a reparse, we have both, the root and the error.
     */
    @Override
    public ParseOutput reparseDocument(Object... argsToReparse) {

        //get the document ast and error in object
        int version;
        try {
            version = grammarVersionProvider.getGrammarVersion();
        } catch (MisconfigurationException e1) {
            //Ok, we cannot get it... let's put on the default
            version = IGrammarVersionProvider.LATEST_GRAMMAR_VERSION;
        }
        long documentTime = System.currentTimeMillis();
        ParseOutput obj = reparseDocument(new ParserInfo(document, version, true));

        IFile original = null;
        IAdaptable adaptable = null;

        if (input == null) {
            return obj;
        }

        original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
        if (original != null) {
            adaptable = original;

        } else {
            //probably an external file, may have some location provider mechanism
            //it may be org.eclipse.ui.internal.editors.text.JavaFileEditorInput
            adaptable = (IAdaptable) input;
        }

        //delete the markers
        if (original != null) {
            try {
                deleteErrorMarkers(original);
            } catch (ResourceException e) {
                //ok, if it is a resource exception, it may have happened because the resource does not exist anymore
                //so, there is no need to log this failure
                if (original.exists()) {
                    Log.log(e);
                }
            } catch (CoreException e) {
                Log.log(e);
            }

        } else if (adaptable == null) {
            //ok, we have nothing... maybe we are in tests...
            if (!PyParser.ACCEPT_NULL_INPUT_EDITOR) {
                throw new RuntimeException("Null input editor received in parser!");
            }
        }
        //end delete the markers

        if (disposed) {
            //if it was disposed in this time, don't fire any notification nor return anything valid.
            return new ParseOutput();
        }

        ErrorParserInfoForObservers errorInfo = null;
        if (obj.error instanceof ParseException || obj.error instanceof TokenMgrError) {
            errorInfo = new ErrorParserInfoForObservers(obj.error, adaptable, document, argsToReparse);
        }

        if (obj.ast != null) {
            //Ok, reparse successful, lets erase the markers that are in the editor we just parsed
            //Note: we may get the ast even if errors happen (and we'll notify in that case too).
            ChangedParserInfoForObservers info = new ChangedParserInfoForObservers(obj.ast, obj.modificationStamp,
                    adaptable, document, documentTime, errorInfo, argsToReparse);
            fireParserChanged(info);
        }

        if (errorInfo != null) {
            fireParserError(errorInfo);
        }

        if (postParserListeners.size() > 0) {
            ArrayList<IPostParserListener> tempList = new ArrayList<>(postParserListeners);
            for (IPostParserListener iParserObserver : tempList) {
                iParserObserver.participantsNotified(argsToReparse);
            }

        }

        return obj;
    }

    //static methods that can be used to get the ast (and error if any) --------------------------------------

    public final static class ParserInfo {
        public IDocument document;

        /**
         * A set with the lines that were changed when trying to make the document parseable
         */
        public final Set<Integer> linesChanged = new HashSet<Integer>();

        /**
         * This is the version of the grammar to be used
         * @see IPythonNature.GRAMMAR_XXX constants
         */
        public final int grammarVersion;

        /**
         * The module name of the contents parsed (may be null)
         */
        public final String moduleName;

        /**
         * The file that's been parsed (may be null)
         */
        public final File file;

        /**
         * Whether we should generate the tree as a parse result or we're just interested in errors.
         */
        public final boolean generateTree;

        /**
         * @param grammarVersion: see IPythonNature.GRAMMAR_XXX constants
         */
        public ParserInfo(IDocument document, int grammarVersion) {
            this(document, grammarVersion, null, null, true);
        }

        public ParserInfo(IDocument document, IGrammarVersionProvider nature) throws MisconfigurationException {
            this(document, nature.getGrammarVersion());
        }

        public ParserInfo(IDocument document, IGrammarVersionProvider nature, String moduleName, File file)
                throws MisconfigurationException {
            this(document, nature.getGrammarVersion(), moduleName, file, true);
        }

        public ParserInfo(IDocument document, int grammarVersion, String name, File f, boolean generateTree) {
            this.document = document;
            this.grammarVersion = grammarVersion;
            this.moduleName = name;
            this.file = f;
            this.generateTree = generateTree;
        }

        public ParserInfo(IDocument document, IGrammarVersionProvider grammarProvider, boolean generateTree)
                throws MisconfigurationException {
            this(document, grammarProvider.getGrammarVersion(), null, null, generateTree);
        }

        public ParserInfo(IDocument document, int grammarVersion, boolean generateTree) {
            this(document, grammarVersion, null, null, generateTree);
        }

        @Override
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("ParserInfo [");
            buf.append("file:");
            buf.append(file);
            buf.append("\nmoduleName:");
            buf.append(moduleName);
            if (!generateTree) {
                buf.append(" NOT GENERATING TREE");
            }
            buf.append("]");
            return buf.toString();
        }
    }

    /**
     * This list of callbacks is mostly used for testing, so that we can check what's been parsed.
     */
    public final static List<ICallback<Object, Tuple3<ISimpleNode, Throwable, ParserInfo>>> successfulParseListeners = new ArrayList<ICallback<Object, Tuple3<ISimpleNode, Throwable, ParserInfo>>>();

    /**
     * Create the char array to parse based on the initial document and our parser limitations.
     */
    private static char[] createCharArrayToParse(String startDoc) {
        int length = startDoc.length();
        int skipAtStart = 0;
        if (startDoc.startsWith(FileUtils.BOM_UTF8)) {
            skipAtStart = FileUtils.BOM_UTF8.length();
        } else if (startDoc.startsWith(FileUtils.BOM_UNICODE)) {
            skipAtStart = FileUtils.BOM_UNICODE.length();
        }

        int addAtEnd = 0;
        if (!startDoc.endsWith("\n") && !startDoc.endsWith("\r")) {
            addAtEnd = 1;
        }

        char[] charArray = new char[length - skipAtStart + addAtEnd];
        startDoc.getChars(skipAtStart, length, charArray, 0);
        if (addAtEnd > 0) {
            charArray[charArray.length - 1] = '\n';
        }
        return charArray;
    }

    /**
     * Actually creates the grammar.
     * @param generateTree whether we should generate the AST or not.
     */
    private static IGrammar createGrammar(boolean generateTree, int grammarVersion, char[] charArray) {
        IGrammar grammar;
        FastCharStream in = new FastCharStream(charArray);
        switch (grammarVersion) {
            case IPythonNature.GRAMMAR_PYTHON_VERSION_2_4:
                grammar = new PythonGrammar24(generateTree, in);
                break;
            case IPythonNature.GRAMMAR_PYTHON_VERSION_2_5:
                grammar = new PythonGrammar25(generateTree, in);
                break;
            case IPythonNature.GRAMMAR_PYTHON_VERSION_2_6:
                grammar = new PythonGrammar26(generateTree, in);
                break;
            case IPythonNature.GRAMMAR_PYTHON_VERSION_2_7:
                grammar = new PythonGrammar27(generateTree, in);
                break;
            case IPythonNature.GRAMMAR_PYTHON_VERSION_3_0:
                grammar = new PythonGrammar30(generateTree, in);
                break;
            //case CYTHON: not treated here (only in reparseDocument).
            default:
                throw new RuntimeException("The grammar specified for parsing is not valid: " + grammarVersion);
        }

        if (ENABLE_TRACING) {
            //grammar has to be generated with debugging info for this to make a difference
            grammar.enable_tracing();
        }
        return grammar;
    }

    /**
     * Note: this method should generally not be needed. Use reparseDocument on most situation (this
     * is mostly for tests or profilings).
     */
    public static Tuple<SimpleNode, IGrammar> reparseDocumentInternal(IDocument doc, boolean generateTree,
            int grammarVersion)
            throws ParseException {
        char[] charArray = createCharArrayToParse(doc.get());
        IGrammar grammar = createGrammar(generateTree, grammarVersion, charArray);
        return new Tuple<SimpleNode, IGrammar>(grammar.file_input(), grammar); // parses the file
    }

    /**
     * @return a tuple with the SimpleNode root(if parsed) and the error (if any).
     *         if we are able to recover from a reparse, we have both, the root and the error.
     */
    public static ParseOutput reparseDocument(ParserInfo info) {
        if (info.grammarVersion == IPythonNature.GRAMMAR_PYTHON_VERSION_CYTHON) {
            IDocument doc = info.document;
            return new ParseOutput(createCythonAst(doc), ((IDocumentExtension4) info.document).getModificationStamp());
        }

        // create a stream with document's data

        //Note: safer could be locking, but if for some reason we get the modification stamp and the document changes
        //right after that, at least any cache will check against the old stamp to be reconstructed (which is the main
        //reason for this stamp).
        long modifiedTime = ((IDocumentExtension4) info.document).getModificationStamp();
        String startDoc = info.document.get();

        if (startDoc.trim().length() == 0) {
            //If empty, don't bother to parse!
            return new ParseOutput(new Module(new stmtType[0]), null, modifiedTime);
        }
        char[] charArray;
        try {
            charArray = createCharArrayToParse(startDoc);
        } catch (OutOfMemoryError e1) {
            OnExpectedOutOfMemory.clearCacheOnOutOfMemory.call(null);
            charArray = createCharArrayToParse(startDoc); //retry now with caches cleared...
        }

        startDoc = null; //it can be garbage-collected now.

        Tuple<ISimpleNode, Throwable> returnVar = new Tuple<ISimpleNode, Throwable>(null, null);
        IGrammar grammar = null;
        try {
            grammar = createGrammar(info.generateTree, info.grammarVersion, charArray);
            SimpleNode newRoot;
            try {
                newRoot = grammar.file_input();
            } catch (OutOfMemoryError e) {
                OnExpectedOutOfMemory.clearCacheOnOutOfMemory.call(null);
                newRoot = grammar.file_input(); //retry now with caches cleared...
            }
            returnVar.o1 = newRoot;

            //only notify successful parses
            if (successfulParseListeners.size() > 0) {
                Tuple3<ISimpleNode, Throwable, ParserInfo> param = new Tuple3<ISimpleNode, Throwable, ParserInfo>(
                        returnVar.o1, returnVar.o2, info);

                for (ICallback<Object, Tuple3<ISimpleNode, Throwable, ParserInfo>> callback : successfulParseListeners) {
                    callback.call(param);
                }
            }

            returnVar.o2 = grammar.getErrorOnParsing();

        } catch (Throwable e) {
            //ok, some error happened when trying the parse... let's go and clear the local info before doing
            //another parse.
            if (DEBUG_SHOW_PARSE_ERRORS) {
                e.printStackTrace();
            }

            //If the grammar was not created, the problem wasn't in the parsing... so, let's just rethrow the error
            if (grammar == null) {
                throw new RuntimeException(e);
            }

            //We have to change it for the 1st error we got (the one in catch is the last one).
            Throwable errorOnParsing = grammar.getErrorOnParsing();
            if (errorOnParsing != null) {
                e = errorOnParsing;
            } else if (DEBUG_SHOW_PARSE_ERRORS) {
                System.out.println("Unhandled error");
                e.printStackTrace();
            }

            grammar = null;

            if (e instanceof ParseException || e instanceof TokenMgrError) {
                returnVar = new Tuple<ISimpleNode, Throwable>(null, e);

            } else if (e.getClass().getName().indexOf("LookaheadSuccess") != -1) {
                //don't log this kind of error...
            } else {
                Log.log(e);
            }

        }

        if (DEBUG_SHOW_PARSE_ERRORS) {
            if (returnVar.o1 == null) {
                System.out.println("Unable to parse " + info);
            }
        }
        //        System.out.println("Output grammar: "+returnVar);
        return new ParseOutput(returnVar, modifiedTime);
    }

    public static Tuple<ISimpleNode, Throwable> createCythonAst(IDocument doc) {
        List<stmtType> classesAndFunctions = FastParser.parseCython(doc);
        return new Tuple<ISimpleNode, Throwable>(new Module(
                classesAndFunctions.toArray(new stmtType[classesAndFunctions
                        .size()])), null);
    }

    /**
     * Adds the error markers for some error that was found in the parsing process.
     *
     * @param error the error find while parsing the document
     * @param resource the resource that should have the error added
     * @param doc the document with the resource contents
     * @return the error description (or null)
     *
     * @throws BadLocationException
     * @throws CoreException
     */
    public static ErrorDescription createParserErrorMarkers(Throwable error, IAdaptable resource, IDocument doc) {
        ErrorDescription errDesc;
        errDesc = createErrorDesc(error, doc);

        //Create marker only if possible...
        if (resource != null) {
            IResource fileAdapter = (IResource) resource.getAdapter(IResource.class);
            if (fileAdapter != null) {
                try {
                    Map<String, Object> map = new HashMap<String, Object>();

                    map.put(IMarker.MESSAGE, errDesc.message);
                    map.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                    map.put(IMarker.LINE_NUMBER, errDesc.errorLine);
                    map.put(IMarker.CHAR_START, errDesc.errorStart);
                    map.put(IMarker.CHAR_END, errDesc.errorEnd);
                    map.put(IMarker.TRANSIENT, true);
                    MarkerUtilities.createMarker(fileAdapter, map, IMarker.PROBLEM);
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        return errDesc;
    }

    /**
     * Creates the error description for a given error in the parse.
     * 
     * Must return an error!
     */
    private static ErrorDescription createErrorDesc(Throwable error, IDocument doc) {
        try {
            int errorStart = -1;
            int errorEnd = -1;
            int errorLine = -1;
            String message = null;
            int tokenBeginLine = -1;

            if (error instanceof ParseException) {
                ParseException parseErr = (ParseException) error;
                message = parseErr.getMessage();

                // Figure out where the error is in the document, and create a
                // marker for it
                if (parseErr.currentToken == null) {
                    try {
                        IRegion endLine = doc.getLineInformationOfOffset(doc.getLength());
                        errorStart = endLine.getOffset();
                        errorEnd = endLine.getOffset() + endLine.getLength();
                    } catch (BadLocationException e) {
                        //ignore (can have changed in the meanwhile)
                    }

                } else {
                    Token errorToken = parseErr.currentToken.next != null ? parseErr.currentToken.next
                            : parseErr.currentToken;
                    if (errorToken != null) {
                        tokenBeginLine = errorToken.beginLine - 1;
                        try {
                            IRegion startLine = doc.getLineInformation(getDocPosFromAstPos(errorToken.beginLine));
                            IRegion endLine;
                            if (errorToken.endLine == 0) {
                                endLine = startLine;
                            } else {
                                endLine = doc.getLineInformation(getDocPosFromAstPos(errorToken.endLine));
                            }
                            errorStart = startLine.getOffset() + getDocPosFromAstPos(errorToken.beginColumn);
                            errorEnd = endLine.getOffset() + errorToken.endColumn;
                        } catch (BadLocationException e) {
                            //ignore (can have changed in the meanwhile)
                        }
                    }
                }

            } else if (error instanceof TokenMgrError) {
                TokenMgrError tokenErr = (TokenMgrError) error;
                message = tokenErr.getMessage();
                tokenBeginLine = tokenErr.errorLine - 1;

                try {
                    IRegion startLine = doc.getLineInformation(tokenErr.errorLine - 1);
                    errorStart = startLine.getOffset();
                    errorEnd = startLine.getOffset() + tokenErr.errorColumn;
                } catch (BadLocationException e) {
                    //ignore (can have changed in the meanwhile)
                }
            } else {
                Log.log("Error, expecting ParseException or TokenMgrError. Received: " + error);
                return new ErrorDescription("Internal PyDev Error", 0, 0, 0);
            }
            try {
                errorLine = doc.getLineOfOffset(errorStart);
            } catch (BadLocationException e) {
                errorLine = tokenBeginLine;
            }

            // map.put(IMarker.LOCATION, "Whassup?"); this is the location field
            // in task manager
            if (message != null) { // prettyprint
                message = StringUtils.replaceNewLines(message, " ");
            }

            return new ErrorDescription(message, errorLine, errorStart, errorEnd);

        } catch (Exception e) {
            Log.log(e);
            return new ErrorDescription("Internal PyDev Error", 0, 0, 0);
        }
    }

    /**
     * The ast position starts at 1 and the document starts at 0 (but it could be that we had nothing valid
     * and received an invalid position, so, we must treat that).
     */
    private static int getDocPosFromAstPos(int astPos) {
        if (astPos > 0) {
            astPos--;
        }
        return astPos;
    }

}
