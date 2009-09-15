package org.python.pydev.parser.prettyprinterv2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;

/**
 * This document is the temporary structure we create to put on the tokens and the comments.
 * 
 * It's line oriented and we should fill it with all things in the proper place (and properly indented) so
 * that we can just make a simple print later on.
 */
public class PrettyPrinterDocV2 {

    public final SortedMap<Integer, PrettyPrinterDocLineEntry> linesToColAndContents = new TreeMap<Integer, PrettyPrinterDocLineEntry>();
    
    private Stack<List<LinePart>> recordedChanges = new Stack<List<LinePart>>();
    
    public void add(int beginLine, int beginCol, String string, Object token) {
        PrettyPrinterDocLineEntry lineContents = getLine(beginLine);
        LinePart linePart = lineContents.add(beginCol, string, token);
        for(List<LinePart> lst:recordedChanges){
            lst.add(linePart);
        }
    }
    

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

    public void pushRecordChanges() {
        recordedChanges.push(new ArrayList<LinePart>());
    }

    public List<LinePart> popRecordChanges() {
        List<LinePart> ret = recordedChanges.pop();
        return ret;
    }


    public LinePart getLastPart() {
        PrettyPrinterDocLineEntry lastLine = getLastLine();
        java.util.List<LinePart> sortedParts = lastLine.getSortedParts();
        return sortedParts.get(sortedParts.size()-1);
    }


    public void replaceRecorded(List<LinePart> recordChanges, String toReplace, String newToken) {
        for(LinePart linePart:recordChanges){
            if(linePart.string.equals(toReplace)){
                linePart.string = newToken;
            }
        }
        
    }

}
