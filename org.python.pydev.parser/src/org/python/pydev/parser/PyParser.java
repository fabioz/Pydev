/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.DocUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.parser.IParserObserver;
import org.python.pydev.core.parser.IParserObserver2;
import org.python.pydev.core.parser.IPyParser;
import org.python.pydev.parser.grammar24.PythonGrammar24;
import org.python.pydev.parser.grammar25.PythonGrammar25;
import org.python.pydev.parser.jython.CharStream;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.IParserHost;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.ReaderCharStream;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.commentType;

/**
 * PyParser uses org.python.parser to parse the document (lexical analysis) It
 * is attached to PyEdit (a view), and it listens to document changes On every
 * document change, the syntax tree is regenerated The reparsing of the document
 * is done on a ParsingThread
 * 
 * Clients that need to know when new parse tree has been generated should
 * register as parseListeners.
 */

public class PyParser implements IPyParser {

    /**
     * just for tests, when we don't have any editor
     */
    public static boolean ACCEPT_NULL_INPUT_EDITOR = false;
    
    /**
     * Defines whether we should use the fast stream or not
     */
    public static boolean USE_FAST_STREAM = true;
    
    /**
     * To know whether we should try to do some reparse changing the input
     */
    public static boolean TRY_REPARSE = true; 

    /**
     * this is the document we should parse 
     */
    private volatile IDocument document;

    /**
     * ast for the last succesful parsing
     */
    private SimpleNode root = null; 

    /**
     * listens to changes in the document
     */
    private IDocumentListener documentListener; 

    /**
     * listeners that get notified of succesfull or unsuccessful parser achievements
     */
    private ArrayList<IParserObserver> parserListeners; 

    /**
     * used to enable tracing in the grammar
     */
    public static boolean ENABLE_TRACING = false;

    /**
     * this is the object that will keep parser schedules for us (and will call us for doing parsing when requested)
     */
    private ParserScheduler scheduler;
    
    /**
     * indicates we should do analysis only on doc save
     */
    private boolean useAnalysisOnlyOnDocSave;

    /**
     * This is the version of the grammar that should be used for this parser
     */
    private IGrammarVersionProvider grammarVersionProvider;

    
    public static String getGrammarVersionStr(int grammarVersion){
        if(grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4){
            return "grammar: Python 2.4";
            
        }else if(grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5){
            return "grammar: Python 2.5";
            
        }else{
            return "grammar: unrecognized";
        }
    }

    /**
     * Should only be called for testing. Does not register as a thread.
     */
    PyParser(IGrammarVersionProvider grammarVersionProvider) {
        if(grammarVersionProvider == null){
            grammarVersionProvider = new IGrammarVersionProvider(){
                public int getGrammarVersion() {
                    return IPythonNature.LATEST_GRAMMAR_VERSION;
                }
            };
        }
        this.grammarVersionProvider = grammarVersionProvider;
        parserListeners = new ArrayList<IParserObserver>();
        scheduler = new ParserScheduler(this);

        documentListener = new IDocumentListener() {

            public void documentChanged(DocumentEvent event) {
                String text = event.getText();
                
                boolean parseNow = true;
                if (event == null || text == null ) {
                	parseNow = false;
                }
                if(parseNow){
                	if(text.indexOf("\n") == -1 && text.indexOf("\r") == -1){
                		parseNow = false;
                		
                	}
                }
                		
                if(!parseNow){
                    // carriage return in changed text means parse now, anything
                    // else means parse later
                    if(!useAnalysisOnlyOnDocSave){
                        scheduler.parseLater();
                    }
                } else {
                    if(!useAnalysisOnlyOnDocSave){
                        scheduler.parseNow();
                    }
                }
            }

            public void documentAboutToBeChanged(DocumentEvent event) {
            }
        };

    }
    
    /**
     * Ok, create the parser for an editor
     * 
     * @param editorView
     */
    public PyParser(IPyEdit editorView) {
        this(editorView.getPythonNature());
    }


    /**
     * should be called when the editor is disposed
     */
    public void dispose() {
        // remove the listeners
        if (document != null){
            document.removeDocumentListener(documentListener);
        }
        synchronized(parserListeners){
        	parserListeners.clear();
        }
    }

    public SimpleNode getRoot() {
        return root;
    }

    public void notifySaved() {
        //force parse on save
        forceReparse();
    }
    
    public void forceReparse(Object ... argsToReparse){
    	scheduler.parseNow(true, argsToReparse);
    }
    

    /**
     * This is the input from the editor that we're using in the parse
     */
    private IEditorInput input;
    
    public void setDocument(IDocument document, IEditorInput input) {
    	setDocument(document, true, input);
    }
    
    public synchronized void setDocument(IDocument doc, boolean addToScheduler, IEditorInput input) {
        this.input = input;
        // Cleans up old listeners
        if (this.document != null) {
            this.document.removeDocumentListener(documentListener);
        }

        // Set up new listener
        this.document = doc;
        if (doc == null) {
            System.err.println("No document in PyParser::setDocument?");
            return;
        }

        doc.addDocumentListener(documentListener);
        
        if(addToScheduler){
	        // Reparse document on the initial set (force it)
	        scheduler.parseNow(true);
        }
    }

    // ---------------------------------------------------------------------------- listeners
    /** stock listener implementation */
    public void addParseListener(IParserObserver listener) {
        Assert.isNotNull(listener);
        synchronized(parserListeners){
	        if (!parserListeners.contains(listener)){
	            parserListeners.add(listener);
	        }
        }
    }

    /** stock listener implementation */
    public void removeParseListener(IParserObserver listener) {
        Assert.isNotNull(listener);
        synchronized(parserListeners){
        	parserListeners.remove(listener);
        }
    }

    
    // ---------------------------------------------------------------------------- notifications
    /**
     * stock listener implementation event is fired whenever we get a new root
     * @param original 
     */
    @SuppressWarnings("unchecked")
	protected void fireParserChanged(SimpleNode root, IAdaptable file, IDocument doc, Object ... argsToReparse) {
        this.root = root;
        synchronized(parserListeners){
        	for (IParserObserver l : new ArrayList<IParserObserver>(parserListeners)) { //work on a copy (because listeners may want to remove themselves and we cannot afford concurrent modifications here)
        		if(l instanceof IParserObserver2){
        			((IParserObserver2)l).parserChanged(root, file, doc, argsToReparse);
        		}else{
        			l.parserChanged(root, file, doc);
        		}
			}

        	List<IParserObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PARSER_OBSERVER);
            for (IParserObserver observer : participants) {
        		if(observer instanceof IParserObserver2){
        			((IParserObserver2)observer).parserChanged(root, file, doc, argsToReparse);
        		}else{
        			observer.parserChanged(root, file, doc);
        		}
            }
        }
    }

    /**
     * stock listener implementation event is fired when parse fails
     * @param original 
     */
    @SuppressWarnings("unchecked")
	protected void fireParserError(Throwable error, IAdaptable file, IDocument doc, Object ... argsToReparse) {
        synchronized(parserListeners){
        	for (IParserObserver l : new ArrayList<IParserObserver>(parserListeners)) {//work on a copy (because listeners may want to remove themselves and we cannot afford concurrent modifications here)
        		if(l instanceof IParserObserver2){
        			((IParserObserver2)l).parserError(error, file, doc, argsToReparse);
        		}else{
        			l.parserError(error, file, doc);
        		}
            }
            List<IParserObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PARSER_OBSERVER);
            for (IParserObserver observer : participants) {
            	if(observer instanceof IParserObserver2){
            		((IParserObserver2)observer).parserError(error, file, doc, argsToReparse);
            	}else{
            		observer.parserError(error, file, doc);
            	}
            }
        }
    }

    // ---------------------------------------------------------------------------- parsing

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
    public Tuple<SimpleNode, Throwable> reparseDocument(Object ... argsToReparse) {
        
        //get the document ast and error in object
        Tuple<SimpleNode, Throwable> obj = reparseDocument(new ParserInfo(document, true, grammarVersionProvider.getGrammarVersion()));
        
        IFile original = null;
        IAdaptable adaptable = null;
        
        if (input == null){
            return obj;
        }
        
        original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
        if(original != null){
            adaptable = original;
            
        }else{
            //probably an external file, may have some location provider mechanism
            //it may be org.eclipse.ui.internal.editors.text.JavaFileEditorInput
            adaptable = input;
        }
        
        //delete the markers
        if (original != null){
        	try {
        		IMarker[] markers = original.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
        		if(markers.length > 0){
        			original.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
        		}
        	} catch (ResourceException e) {
        	    //ok, if it is a resource exception, it may have happened because the resource does not exist anymore
        	    //so, there is no need to log this failure
                if(original.exists()){
                    Log.log(e);
                }
        	} catch (CoreException e) {
        		Log.log(e);
        	}
        	
        }else if(adaptable == null){
        	//ok, we have nothing... maybe we are in tests...
        	if (!PyParser.ACCEPT_NULL_INPUT_EDITOR){
        		throw new RuntimeException("Null input editor received in parser!");
        	}
        }
        //end delete the markers
        
        if(obj.o1 != null){
            //ok, reparse succesful, lets erase the markers that are in the editor we just parsed
            fireParserChanged(obj.o1, adaptable, document, argsToReparse);
        }
        
        if(obj.o2 != null && obj.o2 instanceof ParseException){
            fireParserError((ParseException) obj.o2, adaptable, document, argsToReparse);
        }
        
        if(obj.o2 != null && obj.o2 instanceof TokenMgrError){
            fireParserError((TokenMgrError) obj.o2, adaptable, document, argsToReparse);
        }
        
        return obj;
    }
    

    
    //static methods that can be used to get the ast (and error if any) --------------------------------------
    

    public static class ParserInfo{
        public IDocument document;
        public boolean stillTryToChangeCurrentLine=true; 
        public int currentLine=-1;
        public String initial = null;
        public List<Integer> linesChanged = new ArrayList<Integer>();
        public ParseException parseErr;
        public boolean tryReparse = TRY_REPARSE;
        public int grammarVersion;
        
        public ParserInfo(IDocument document, boolean changedCurrentLine, int grammarVersion){
            this.document = document;
            this.stillTryToChangeCurrentLine = changedCurrentLine;
            this.grammarVersion = grammarVersion;
        }
        
        public ParserInfo(IDocument document, boolean changedCurrentLine, IPythonNature nature){
            this(document, changedCurrentLine, nature.getGrammarVersion());
        }
        
        public ParserInfo(IDocument document, boolean changedCurrentLine, IPythonNature nature, int currentLine){
            this(document, changedCurrentLine, nature);
            this.currentLine = currentLine;
        }
    }
    
    
    /**
     * Removes comments at the end of the document
     * @param doc this is the document from where the comments must be removed
     * @return a tuple with: StringBuffer with the comments that have been removed,
     * beginLine for the comments beginColumn for the comments
     * (both starting at 1)
     */
    public static List<commentType> removeEndingComments(IDocument doc){
        StringBuffer comments = new StringBuffer();
        int lines = doc.getNumberOfLines();
        String delimiter = PySelection.getDelimiter(doc);
        
        for (int i = lines-1; i >= 0; i--) {
            String line = PySelection.getLine(doc, i);
            String trimmed = line.trim();
            if(trimmed.length() > 0 && trimmed.charAt(0) != '#'){
                return makeListOfComments(comments, line.length()+2, i+1);
            }
            comments.insert(0,line);
            comments.insert(0,delimiter);
            try {
                if(line.length() > 0){
                    PySelection.deleteLine(doc, i);
                }
            } catch (Exception e) {
                //ignore
            }
        }
        
        return makeListOfComments(comments,0,0);
    }

    private static List<commentType> makeListOfComments(StringBuffer comments, int beginCol, int beginLine) {
        ArrayList<commentType> ret = new ArrayList<commentType>();
        int len = comments.length();

        char c;
        StringBuffer buf = null;

        int col=0;
        int line=0;
        int startCol=-1;
        for (int i = 0; i < len; i++) {
            c = comments.charAt(i);

            if(buf == null && c == '#'){
                buf = new StringBuffer();
                startCol = col;
            }
            

            if (c == '\r') {
                if (i < len - 1 && comments.charAt(i + 1) == '\n') {
                    i++;
                }
                addCommentLine(ret, buf, beginCol, beginLine, startCol, line);
                buf = null;
                col = 0;
                line++;
                startCol = -1;
            }
            if (c == '\n') {
                addCommentLine(ret, buf, beginCol, beginLine, startCol, line);
                buf = null;
                col = 0;
                line++;
                startCol = -1;
            }
            
            if(buf != null){
                buf.append(c);
            }
            col++;
        }
        
        if (buf != null && buf.length() != 0) {
            addCommentLine(ret, buf, beginCol, beginLine, startCol, line);
        }
        return ret;
    }

    private static void addCommentLine(ArrayList<commentType> ret, StringBuffer buf, int beginCol, int beginLine, int col, int line) {
        if(buf != null){
            commentType comment = new commentType(buf.toString());
            comment.beginLine = beginLine+line;
            if(line == 0){
                comment.beginColumn = beginCol+col;
            }else{
                comment.beginColumn = col;
            }
            ret.add(comment);
        }
    }
    
    
    /**
     * @return a tuple with the SimpleNode root(if parsed) and the error (if any).
     *         if we are able to recover from a reparse, we have both, the root and the error.
     */
    public static Tuple<SimpleNode, Throwable> reparseDocument(ParserInfo info) {
        // create a stream with document's data
        String startDoc = info.document.get();
        if(info.initial == null){
            info.initial = startDoc;
        }

        IDocument newDoc = new Document(startDoc);
        List<commentType> comments = removeEndingComments(newDoc);
        try {
            //make sure it ends with a new line
            newDoc.replace(newDoc.getLength(), 0, "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        
        CharStream in = null;
        if(USE_FAST_STREAM){
            //we don't want to keep the string from being released, so, just get the char array from the string
            char[] cs = newDoc.get().toCharArray();
        	in = new FastCharStream(cs);
        }else{
        	//this should be deprecated in the future (it is still here so that we can evaluate
        	//the changes done by the change of the reader).
            String initialDoc = newDoc.get();
	        StringReader inString = new StringReader(initialDoc);
	        in = new ReaderCharStream(inString);
            throw new RuntimeException("This char stream reader was deprecated (was maintained only for testing purposes).");
        }
        
        IParserHost host = new CompilerAPI();
        IGrammar grammar = null;
        if(info.grammarVersion == IPythonNature.GRAMMAR_PYTHON_VERSION_2_4){
            grammar = new PythonGrammar24(in, host);
        }else if(info.grammarVersion == IPythonNature.GRAMMAR_PYTHON_VERSION_2_5){
            grammar = new PythonGrammar25(in, host);
        }else{
            throw new RuntimeException("The grammar specified for parsing is not valid: "+info.grammarVersion);
        }

        try {
        	
        	if(ENABLE_TRACING){
        		//grammar has to be generated with debugging info for this to make a difference
        		grammar.enable_tracing();
        	}
            SimpleNode newRoot = grammar.file_input(); // parses the file
            if(newRoot != null){
                Module m = (Module) newRoot;
                for (commentType comment : comments) {
                    m.addSpecial(comment, true);
                }
            }
            return new Tuple<SimpleNode, Throwable>(newRoot,null);
		

        } catch (ParseException parseErr) {
            SimpleNode newRoot = null;

            if(info.parseErr == null){
                info.parseErr = parseErr;
            }
            
            if(info.tryReparse){
                if (info.stillTryToChangeCurrentLine){
                    newRoot = tryReparseAgain(info, info.parseErr);
                } else {
                    info.currentLine = -1;
                    info.document = new Document(info.initial);
                    newRoot = tryReparseAgain(info, info.parseErr);
                }
            }
            return new Tuple<SimpleNode, Throwable>(newRoot, parseErr);
            
        
        } catch (TokenMgrError tokenErr) {
            SimpleNode newRoot = null;
            
            if(info.tryReparse){
                if (info.stillTryToChangeCurrentLine){
                    newRoot = tryReparseAgain(info, tokenErr);
                }
            }
            
            return new Tuple<SimpleNode, Throwable>(newRoot, tokenErr);

        } catch (Exception e) {
            Log.log(e);
            return new Tuple<SimpleNode, Throwable>(null, null);
        
        } catch (Throwable e) {
			//PythonGrammar$LookaheadSuccess error: this happens sometimes when the file is
			//not parseable
			if(e.getClass().getName().indexOf("LookaheadSuccess") != -1){
				//don't log this kind of error...
			}else{
				Log.log(e);
			}
			return new Tuple<SimpleNode, Throwable>(null, null);
        }
    }

    /**
     * @param tokenErr
     */
    private static SimpleNode tryReparseAgain(ParserInfo info, TokenMgrError tokenErr) {
        int line = -1;
        
        if(info.currentLine > -1){
            line = info.currentLine;
        }else{
            line = tokenErr.errorLine;
        }
        
        return tryReparseChangingLine(info, line);
    }

    /**
     * This method tries to reparse the code again, changing the current line to
     * a 'pass'
     * 
     * Any new errors are ignored, and the error passed as a parameter is fired
     * anyway, so, the utility of this function is trying to make a real model
     * without any problems, so that we can update the outline and ModelUtils
     * with a good aproximation of the code.
     * 
     * @param tokenErr
     */
    private static SimpleNode tryReparseAgain(ParserInfo info, ParseException tokenErr) {
        int line = -1;
        
        if(info.currentLine > -1){
            line = info.currentLine;
        
        }else{
            if(tokenErr.currentToken != null){
                line = tokenErr.currentToken.beginLine-2;
            
    	        boolean okToGo = false;
    	        
    	        while(! okToGo){
    		        if(! lineIn(info.linesChanged, line)){
    		            info.linesChanged.add(new Integer(line));
    		            okToGo = true;
    		            
    		        } else if(info.linesChanged.size() < 10){
    		            line += 1;
    		            
    		        } else{
    		            return null;
    		        }
                }
	        }else{
             return null;   
            }
        }

        return tryReparseChangingLine(info, line);
    }

    /**
     * @param linesChanged
     * @param line
     * @return
     */
    private static boolean lineIn(List<Integer> linesChanged, int line) {
        for (Iterator<Integer> iter = linesChanged.iterator(); iter.hasNext();) {
            Integer i = iter.next();
            if (i.intValue() == line){
                return true;
            }
        }
        return false;
    }

    /**
     * Try a reparse changing the offending line.
     * 
     * @param document: this is the document to be changed
     * @param line: offending line to be changed to try a reparse.
     * 
     */
    private static SimpleNode tryReparseChangingLine(ParserInfo info, int line) {
        String docToParse = DocUtils.getDocToParseFromLine(info.document, line);
        if(docToParse != null){

            Document doc = new Document(docToParse);
            info.document = doc;
            info.stillTryToChangeCurrentLine = false;
	        return reparseDocument(info).o1;
        }
        return null;
    }

    public void resetTimeoutPreferences(boolean useAnalysisOnlyOnDocSave) {
        this.useAnalysisOnlyOnDocSave = useAnalysisOnlyOnDocSave;
    }

    public List<IParserObserver> getObservers() {
        return new ArrayList<IParserObserver>(this.parserListeners);
    }


    
}

