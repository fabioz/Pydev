package org.python.pydev.parser.prettyprinterv2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.stmtType;

/**
 * This document is the temporary structure we create to put on the tokens and the comments.
 * 
 * It's line oriented and we should fill it with all things in the proper place (and properly indented) so
 * that we can just make a simple print later on.
 */
public class PrettyPrinterDocV2 {
    
    private static class Addition{

        public int beginLine;
        public int beginCol;
        public String string;
        public Object token;

        public Addition(int beginLine, int beginCol, String string, Object token) {
            this.beginLine=beginLine;
            this.beginCol=beginCol;
            this.string=string;
            this.token=token;
        }
        
    }

    public final SortedMap<Integer, PrettyPrinterDocLineEntry> linesToColAndContents = new TreeMap<Integer, PrettyPrinterDocLineEntry>();
    
    private Map<Integer, List<ILinePart>> recordedChanges = new HashMap<Integer, List<ILinePart>>();
    private int lastRecordedChangesId=0;
    
    private List<Addition> sequentialRecording = null;
    private int sequentialRecordingStackSize = 0;
    
    
    public void add(int beginLine, int beginCol, String string, Object token) {
        if(sequentialRecording != null){
            sequentialRecording.add(new Addition(beginLine, beginCol, string, token));
            return;
        }
        
        PrettyPrinterDocLineEntry lineContents = getLine(beginLine);
        ILinePart linePart = lineContents.add(beginCol, string, token);
        for(List<ILinePart> lst:recordedChanges.values()){
            lst.add(linePart);
        }
    }
    
    
    //---------------- Mark that a statement has started (new lines need a '\')
    
    public void addStartStatementMark(ILinePart foundWithLowerLocation, stmtType node) {
        getLine(foundWithLowerLocation.getLine()).addStartStatementMark(foundWithLowerLocation, node);
    }


    public void addEndStatementMark(ILinePart foundWithHigherLocation, stmtType node) {
        getLine(foundWithHigherLocation.getLine()).addEndStatementMark(foundWithHigherLocation, node);
    }
    
    
    //---------------- Make things in the order we say!
    

    @SuppressWarnings("unchecked")
    public void startWriteSequentialOnSameLine() {
        sequentialRecordingStackSize+=1;
        if(sequentialRecordingStackSize == 1){
            sequentialRecording = new ArrayList();
        }
    }


    public void endWriteSequentialOnSameLine() {
        sequentialRecordingStackSize-=1;
        if(sequentialRecordingStackSize == 0){
            List<Addition> sequential = sequentialRecording;
            sequentialRecording = null;
            Addition previous = null;
            for(Addition addition:sequential){
                if(previous != null){
                    if(addition.beginLine == previous.beginLine){
                        //put them in order
                        if(addition.beginCol <= previous.beginCol){
                            addition.beginCol = previous.beginCol+1;
                        }
                    }
                }
                add(addition.beginLine, addition.beginCol, addition.string, addition.token);
                previous = addition;
            }
        }
    }

    
    //------------ Get information

    PrettyPrinterDocLineEntry getLine(int beginLine) {
        PrettyPrinterDocLineEntry lineContents = linesToColAndContents.get(beginLine);
        if(lineContents == null){
            lineContents = new PrettyPrinterDocLineEntry(beginLine);
            linesToColAndContents.put(beginLine, lineContents);
        }
        return lineContents;
    }

    int getLastLineKey() {
        return linesToColAndContents.lastKey();
    }
    
    PrettyPrinterDocLineEntry getLastLine() {
        Integer lastKey = linesToColAndContents.lastKey();
        if(lastKey != null){
            return linesToColAndContents.get(lastKey);
        }
        return null;
    }
    
    
    
    public ILinePart getLastPart() {
        PrettyPrinterDocLineEntry lastLine = getLastLine();
        java.util.List<ILinePart> sortedParts = lastLine.getSortedParts();
        return sortedParts.get(sortedParts.size()-1);
    }

    
    
    
    
    //------------ Indentation
    public void addIndent(SimpleNode node) {
        PrettyPrinterDocLineEntry line = getLine(node.beginLine);
        line.indent(node);
        
    }
    
    public void addIndent(Token token) {
        PrettyPrinterDocLineEntry line = getLine(token.beginLine);
        line.indent(token);
    }    


    public void addDedent(SimpleNode node) {
        PrettyPrinterDocLineEntry lastLine = getLastLine();
        lastLine.dedent(node);
    }
    
    
    
    
    

    //------------ toString

    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("PrettyPrinterDocV2[\n");
        Set<Entry<Integer, PrettyPrinterDocLineEntry>> entrySet = linesToColAndContents.entrySet();
        for(Entry<Integer, PrettyPrinterDocLineEntry> entry:entrySet){
            buf.append(entry.getKey()+": "+entry.getValue()+"\n");
        }
        return "PrettyPrinterDocV2["+buf+"]";
    }
    
    
    
    
    //------------ Changes Recording

    public int pushRecordChanges() {
        lastRecordedChangesId++;
        recordedChanges.put(lastRecordedChangesId, new ArrayList<ILinePart>());
        return lastRecordedChangesId;
    }

    public List<ILinePart> popRecordChanges(int id) {
        List<ILinePart> ret = recordedChanges.remove(id);
        return ret;
    }


    public void replaceRecorded(List<ILinePart> recordChanges, String ... replacements) {
        Assert.isTrue(replacements.length % 2 == 0);
        for(ILinePart linePart:recordChanges){
            if(linePart instanceof ILinePart2){
                ILinePart2 iLinePart2 = (ILinePart2) linePart;
                for(int i=0;i<replacements.length;i+=2){
                    String toReplace = replacements[i]; 
                    String newToken = replacements[i+1];
                    if(iLinePart2.getString().equals(toReplace)){
                        iLinePart2.setString(newToken);
                    }
                }
            }
        }
        
    }


    public Tuple<ILinePart, ILinePart> getLowerAndHigerFound(List<ILinePart> recordChanges) {
        Tuple<ILinePart, ILinePart> lowerAndHigher = null;
        ILinePart foundWithLowerLocation=null;
        ILinePart foundWithHigherLocation=null;
        
        for(ILinePart p:recordChanges){
            if(p.getToken() instanceof commentType){
                continue;
            }
            if(foundWithHigherLocation==null){
                foundWithHigherLocation = p;
                
            }else if(p.getLine() > foundWithHigherLocation.getLine()){
                foundWithHigherLocation = p;
                
            }else if(p.getLine() == foundWithHigherLocation.getLine() && p.getBeginCol() > foundWithHigherLocation.getBeginCol()){
                foundWithHigherLocation = p;
            }
            
            
            if(foundWithLowerLocation==null){
                foundWithLowerLocation = p;
                
            }else if(p.getLine() < foundWithLowerLocation.getLine()){
                foundWithLowerLocation = p;
                
            }else if(p.getLine() == foundWithLowerLocation.getLine() && p.getBeginCol() < foundWithLowerLocation.getBeginCol()){
                foundWithLowerLocation = p;
            }
        }
        if(foundWithLowerLocation != null && foundWithHigherLocation != null){
            lowerAndHigher = new Tuple<ILinePart, ILinePart>(foundWithLowerLocation, foundWithHigherLocation);
        }
        return lowerAndHigher;
    }


    
}
