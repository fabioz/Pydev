package org.python.pydev.parser.prettyprinterv2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Map.Entry;

import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.prettyprinter.IPrettyPrinterPrefs;

/**
 * The initial pretty printer approach consisted of going to a scope and then printing things
 * in that scope as it walked the structure, but this approach doesn't seem to work well
 * because of comments, as it depends too much on how the parsing was done and the comments
 * found (and javacc just spits them out and the parser tries to put them in good places, but
 * this is often not what happens)
 * 
 * So, a different approach will be tested:
 * Instead of doing everything in a single pass, we'll traverse the structure once to create
 * a new (flat) structure, in a 2nd step that structure will be filled with comments and in
 * a final step, that intermediary structure will be actually written.
 * 
 * This will also enable the parsing to be simpler (and faster) as it'll not have to move comments
 * around to try to find a suitable position.
 */
public class PrettyPrinterV2 {

    private IPrettyPrinterPrefs prefs;

    public PrettyPrinterV2(IPrettyPrinterPrefs prefs) {
        this.prefs = prefs;
    }

    public String print(SimpleNode m) throws IOException {
        PrettyPrinterDocV2 doc = new PrettyPrinterDocV2();
        PrettyPrinterVisitorV2 visitor = new PrettyPrinterVisitorV2(prefs, doc);
        try{
            m.accept(visitor);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        
        
        WriterEraserV2 writerEraserV2 = new WriterEraserV2();
        WriteStateV2 writeStateV2 = new WriteStateV2(writerEraserV2, prefs);
        
        //Now that the doc is filled, let's make a string from it.
        Set<Entry<Integer, PrettyPrinterDocLineEntry>> entrySet = doc.linesToColAndContents.entrySet();
        
        List<Tuple<PrettyPrinterDocLineEntry, String>> previousLines = new ArrayList<Tuple<PrettyPrinterDocLineEntry, String>>();
        
        for(Entry<Integer, PrettyPrinterDocLineEntry> entry:entrySet){
            PrettyPrinterDocLineEntry line = entry.getValue();
            List<LinePart> sortedParts = line.getSortedParts();
            
            
            boolean writtenComment = false;
            if(sortedParts.size() == 1){
                //Ok, we need a special treatment for lines that only contain comments.
                //As it doesn't belong in the actual AST (it's just spit out in the middle of the parsing),
                //it can happen that it doesn't belong in the current indentation (and rather to the last indentation
                //found), so, we have to go on and check how we should indent it based on the previous line(s)
                LinePart linePart = sortedParts.get(0);
                if(linePart.token instanceof commentType){
                    commentType commentType = (commentType) linePart.token;
                    int col = commentType.beginColumn;
                    if(col == 0){
                        writtenComment=true;
                        writeStateV2.writeRaw(linePart.toString());
                    }else{
                        Tuple<PrettyPrinterDocLineEntry, String> found = null;
                        //Let's go backward in the lines to see one that matches the current indentation.
                        ListIterator<Tuple<PrettyPrinterDocLineEntry, String>> it = previousLines.listIterator(previousLines.size());
                        while(it.hasPrevious() && found == null){
                            Tuple<PrettyPrinterDocLineEntry, String> previous = it.previous();
                            int firstCol = previous.o1.getFirstCol();
                            if(firstCol != -1){
                                if(firstCol == col){
                                    found = previous;
                                }
                            }
                        }
                        
                        if(found != null){
                            writtenComment=true;
                            writeStateV2.writeRaw(found.o2);
                            writeStateV2.writeRaw(linePart.string);
                        }
                    }
                }
            }
            
            
            if(!writtenComment){
                for(LinePart linePart:sortedParts){
                    //Note: on a write, if the last thing was a new line, it'll indent.
                    writeStateV2.write(prefs.getReplacement(linePart.string));
                }
            }
            
            
            previousLines.add(new Tuple<PrettyPrinterDocLineEntry, String>(line, writeStateV2.getIndentString()));
            
            int indentDiff = line.getIndentDiff();
            if(indentDiff > 0){
                while(indentDiff != 0){
                    indentDiff --;
                    writeStateV2.indent();
                }
            }else if(indentDiff < 0){
                while(indentDiff != 0){
                    indentDiff ++;
                    writeStateV2.dedent();
                }
            }
            writeStateV2.writeNewLine();
            
        }
        
        return writerEraserV2.getBuffer().toString();
    }
    

}
