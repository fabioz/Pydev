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
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.aliasType;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;

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
            messages.put(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE, "Undefined variable: %s");
            messages.put(IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE, "Duplicated signature: %s");
            messages.put(IAnalysisPreferences.TYPE_REIMPORT, "Import redefinition: %s");
            messages.put(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, "Unresolved import: %s");
            messages.put(IAnalysisPreferences.TYPE_NO_SELF, "Method '%s' should have %s as first parameter");
        }
        return messages.get(getType());

    }
    
    public int getSeverity() {
        return severity;
    }

    public int getType() {
        return type;
    }

    public int getStartLine(IDocument doc) {
        return generator.getLineDefinition();
    }

    /**
     * gets the start col of the message
     *  
     * @see com.python.pydev.analysis.messages.IMessage#getStartCol(org.eclipse.jface.text.IDocument)
     */
    public int getStartCol(IDocument doc) {
        int colDefinition=0;
       
        //not import...
        if(!generator.isImport()){
            colDefinition = generator.getColDefinition();
            if(colDefinition > 0){
                colDefinition = fixCol(colDefinition);
                return colDefinition;
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
                return getNameForRepresentation(i, getShortMessage().toString(), false).beginColumn;
            }
            
        }else if(ast instanceof Import){
            String shortMessage = getShortMessage().toString();
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
    private NameTok getNameForRepresentation(SimpleNode imp, String rep, boolean returnAsName){
	    	
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
    private int fixCol(int col) {
        if(generator instanceof SourceToken){
            SimpleNode ast = ((SourceToken)generator).getAst();
            if(ast instanceof ClassDef){
                return col + 6;
            }
            if(ast instanceof FunctionDef){
                return col + 4;
            }
        }
        return col;
    }

    /**
     * @see com.python.pydev.analysis.messages.IMessage#getEndLine(org.eclipse.jface.text.IDocument)
     */
    public int getEndLine(IDocument doc) {
        if(generator instanceof SourceToken){
            return ((SourceToken)generator).getLineEnd();
        }
        return -1;
    }


    /**
     * @return the end column for this message
     *  
     * @see com.python.pydev.analysis.messages.IMessage#getEndCol(org.eclipse.jface.text.IDocument)
     */
    public int getEndCol(IDocument doc) {
        if(generator.isImport()){
            //ok, it is an import... (can only be a source token)
            SourceToken s = (SourceToken) generator;
            
            SimpleNode ast = s.getAst();
            String shortMessage = getShortMessage().toString();
            if(ast instanceof ImportFrom){
                ImportFrom i = (ImportFrom) ast;
                //ok, now, this depends on the name
                NameTok it = getNameForRepresentation(i, shortMessage, true);
                if(it != null){
                    return it.beginColumn + shortMessage.length();
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
					return absoluteCol - region.getOffset() +1 ; //1 because we should return as if starting in 1 and not 0
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
                
            }else if(ast instanceof Import){
                NameTok it = getNameForRepresentation((Import) ast, shortMessage, true);
                return it.beginColumn + shortMessage.length();
            }else{
                throw new RuntimeException("It is not an import");
            }
        }
        
        //no import... make it regular
        if(generator instanceof SourceToken){
            int colEnd = ((SourceToken)generator).getColEnd();
            
            if(colEnd == -1){
                return -1;
            }
            
            return fixCol(colEnd);
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

    public String getMessage() {
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
        return StringUtils.format(typeStr, shortMessage);
    }
    
    public IToken getGenerator() {
        return generator;
    }
}
