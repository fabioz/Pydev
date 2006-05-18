/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IToken;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;

import com.python.pydev.analysis.IAnalysisPreferences;


public abstract class AbstractMessage implements IMessage{

    public static final Map<Integer, String> messages = new HashMap<Integer, String>();

    private int type;

    private int severity;

    private IToken generator;

    private List<String> additionalInfo;

    public AbstractMessage(int type, IToken generator, IAnalysisPreferences prefs) {
        this.severity = prefs.getSeverityForType(type);
        this.type = type;
        this.generator = generator;
    }
    
    

    private String getTypeStr() {
        if (messages.size() == 0) {
            messages.put(IAnalysisPreferences.TYPE_UNUSED_IMPORT, "Unused import: %s");
            messages.put(IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT, "Unused in wild import: %s");
            messages.put(IAnalysisPreferences.TYPE_UNUSED_VARIABLE, "Unused variable: %s");
            messages.put(IAnalysisPreferences.TYPE_UNUSED_PARAMETER, "Unused parameter: %s");
            messages.put(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE, "Undefined variable: %s");
            messages.put(IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE, "Duplicated signature: %s");
            messages.put(IAnalysisPreferences.TYPE_REIMPORT, "Import redefinition: %s");
            messages.put(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, "Unresolved import: %s");
            messages.put(IAnalysisPreferences.TYPE_NO_SELF, "Method '%s' should have %s as first parameter");
            messages.put(IAnalysisPreferences.TYPE_UNDEFINED_IMPORT_VARIABLE, "Undefined variable from import: %s");
        }
        return messages.get(getType());

    }
    
    public int getSeverity() {
        return severity;
    }

    public int getType() {
        return type;
    }

    int line = -1;
    public int getStartLine(IDocument doc) {
    	if(line < 0){
    		line = getStartLine(generator, doc);
    	}
    	return line;
    }

    public static int getStartLine(IToken generator, IDocument doc) {
    	return generator.getLineDefinition();
    }
    
    int startCol = -1;
    /**
     * gets the start col of the message
     *  
     * @see com.python.pydev.analysis.messages.IMessage#getStartCol(org.eclipse.jface.text.IDocument)
     */
    public int getStartCol(IDocument doc) {
    	if(startCol >= 0){
    		return startCol;
    	}
    	startCol = getStartCol(generator, doc, getShortMessage().toString());
    	return startCol;
    	
    }
    
    public static int getStartCol(IToken generator, IDocument doc) {
    	return getStartCol(generator, doc, generator.getRepresentation());
    }
    
    public static int getStartCol(IToken generator, IDocument doc, String shortMessage) {
        int colDefinition=0;
       
        //not import...
        if(!generator.isImport()){
            colDefinition = generator.getColDefinition();
            if(colDefinition > 0){
                return fixCol(generator, colDefinition);
            }
        }
        
        //ok, it is an import... (can only be a source token)
        SourceToken s = (SourceToken) generator;
        
        SimpleNode ast = s.getAst();
        if(ast instanceof ImportFrom){
            ImportFrom i = (ImportFrom) ast;
            //if it is a wild import, it starts on the module name
            if(AbstractVisitor.isWildImport(i)){
                return i.module.beginColumn;
            }else{
                //no wild import, let's check the 'as name'
            	return getNameForRepresentation(i, shortMessage, false).beginColumn;
            }
            
        }else if(ast instanceof Import){
            NameTok it = getNameForRepresentation(ast, shortMessage, false);
            return it.beginColumn;
            
        }else{
            throw new RuntimeException("It is not an import");
        }
    }

    /**
     * @param imp this is the import ast
     * @param fullRep this is the representation we are looking for
     * @param returnAsName defines if we should return the asname or only the name (depending on what we are
     * analyzing -- the start or the end of the representation).
     * 
     * @return the name tok for the representation in a given import
     */
    private static NameTok getNameForRepresentation(SimpleNode imp, String rep, boolean returnAsName){
	    	
        aliasType[] names;
        if(imp instanceof Import){
            names = ((Import)imp).names;
        }else if(imp instanceof ImportFrom){
            names = ((ImportFrom)imp).names;
        }else{
            throw new RuntimeException("import expected");
        }
        
        for (aliasType alias : names) {
            if(alias.asname != null){
                if(((NameTok)alias.asname).id.equals(rep)){
                    if(returnAsName){
                        return (NameTok)alias.asname;
                    }else{
                        return (NameTok) alias.name;
                    }
                }
            }else{ //let's check for the name

            	String fullRepNameId = ((NameTok)alias.name).id;
            	
            	//we have to get all representations, since an import such as import os.path would 
            	//have to match os and os.path
            	for(String repId : new FullRepIterable(fullRepNameId)){
	
					if(repId.equals(rep)){
	            		return (NameTok) alias.name;
	            	}
            	}
            }
        }
        return null;
    }

    /**
     * Fix the column for a class or function def
     */
    private static int fixCol(IToken generator, int col) {
        if(generator instanceof SourceToken){
            SimpleNode ast = ((SourceToken)generator).getAst();
            if(ast instanceof ClassDef){
            	ClassDef d = (ClassDef) ast;
                return d.name.beginColumn;
            }
            if(ast instanceof FunctionDef){
            	FunctionDef d = (FunctionDef) ast;
            	return d.name.beginColumn;
            }
        }
        return col;
    }

    /**
     * @see com.python.pydev.analysis.messages.IMessage#getEndLine(org.eclipse.jface.text.IDocument)
     */
    int endLine = -1;
    public int getEndLine(IDocument doc) {
    	return getEndLine(doc, true);
    }
    public int getEndLine(IDocument doc, boolean getOnlyToFirstDot) {
    	if(endLine < 0){
			endLine = getEndLine(generator, doc, getOnlyToFirstDot);
    	}
    	return endLine;
    	
    }
    public static int getEndLine(IToken generator, IDocument doc, boolean getOnlyToFirstDot) {
    	if(generator instanceof SourceToken){
    		return ((SourceToken)generator).getLineEnd(getOnlyToFirstDot);
    	}else{
    		return -1;
    	}
    }


    int endCol = -1;
    public int getEndCol(IDocument doc) {
    	return getEndCol(doc, true);
    }
    public int getEndCol(IDocument doc, boolean getOnlyToFirstDot) {
    	if(endCol >= 0){
    		return endCol;
    	}
    	endCol = getEndCol(generator, doc, getShortMessage().toString(), getOnlyToFirstDot);
    	return endCol;
    	
    }
    /**
     * @param generator is the token that generated this message
     * @param doc is the document where this message will be put
     * @param shortMessage is used when it is an import ( = foundTok.getRepresentation())
     * 
     * @return the end column for this message
     *  
     * @see com.python.pydev.analysis.messages.IMessage#getEndCol(org.eclipse.jface.text.IDocument)
     */
    public static int getEndCol(IToken generator, IDocument doc, String shortMessage, boolean getOnlyToFirstDot) {
    	int endCol = -1;
        if(generator.isImport()){
            //ok, it is an import... (can only be a source token)
            SourceToken s = (SourceToken) generator;
            
            SimpleNode ast = s.getAst();
            
            if(ast instanceof ImportFrom){
                ImportFrom i = (ImportFrom) ast;
                //ok, now, this depends on the name
                NameTok it = getNameForRepresentation(i, shortMessage, true);
                if(it != null){
                	endCol = it.beginColumn + shortMessage.length();
                	return endCol;
                }
                
                //if still not returned, it is a wild import... find the '*'
                try {
                    IRegion lineInformation = doc.getLineInformation(i.module.beginLine-1);
                    //ok, we have the line... now, let's find the absolute offset
                    int absolute = lineInformation.getOffset() + i.module.beginColumn-1;
                    while(doc.getChar(absolute) != '*'){
                        absolute ++;
                    }
                    int absoluteCol = absolute +1; //1 for the *
                    IRegion region = doc.getLineInformationOfOffset(absoluteCol);
                    endCol = absoluteCol - region.getOffset() +1 ; //1 because we should return as if starting in 1 and not 0
					return endCol;
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
                
            }else if(ast instanceof Import){
                NameTok it = getNameForRepresentation((Import) ast, shortMessage, true);
                endCol = it.beginColumn + shortMessage.length();
                return endCol;
            }else{
                throw new RuntimeException("It is not an import");
            }
        }
        
        //no import... make it regular
        if(generator instanceof SourceToken){
            int colEnd = ((SourceToken)generator).getColEnd(getOnlyToFirstDot);
            
            if(colEnd == -1){
                return -1;
            }
            endCol = fixCol(generator, colEnd);
            return endCol;
        }
        return -1;
    }


    public String toString() {
        return getMessage();
    }
    
    public List<String> getAdditionalInfo(){
        return additionalInfo;
    }
    
    public void addAdditionalInfo(String info){
        if(this.additionalInfo == null){
            this.additionalInfo = new ArrayList<String>();
        }
        this.additionalInfo.add(info);
    }

    String message=null;
    public String getMessage() {
    	if(message != null){
    		return message;
    	}
    	
        String typeStr = getTypeStr();
        if(typeStr == null){
            throw new AssertionError("Unable to get message for type: "+getSeverity());
        }
        Object shortMessage = getShortMessage();
        if(shortMessage == null){
            throw new AssertionError("Unable to get shortMessage ("+typeStr+")");
        }
        if( shortMessage instanceof Object[]){
            Object[] o = (Object[]) shortMessage;
            
            //if we have the same number of %s as objects in the array, make the format
            int countPercS = StringUtils.countPercS(typeStr);
            if(countPercS == o.length){
                return StringUtils.format(typeStr, o);
                
            }else if(countPercS == 1){
                //if we have only 1, all parameters should be concatenated in a single string
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < o.length; i++) {
                    buf.append(o[i].toString());
                    if(i != o.length-1){
                        buf.append(" ");
                    }
                }
                shortMessage = buf.toString();
                
            }else{
                throw new AssertionError("The number of %s is not the number of passed parameters nor 1");
            }
        }
        message = StringUtils.format(typeStr, shortMessage);
        return message;
    }
    
    public IToken getGenerator() {
        return generator;
    }
}
