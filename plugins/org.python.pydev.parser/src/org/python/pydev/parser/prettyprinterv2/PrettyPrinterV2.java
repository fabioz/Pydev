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
    
    private final int LEVEL_PARENS = 0; //()
    private final int LEVEL_BRACKETS = 1; //[]
    private final int LEVEL_BRACES = 2; //{} 
    
    private final int[] LEVELS = new int[]{0,0,0};
    
    private int statementLevel=0;

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
            List<ILinePart> sortedParts = line.getSortedParts();
            int indentDiff = line.getIndentDiff();
            
            
            List<ILinePart2> sortedPartsWithILinePart2 = new ArrayList<ILinePart2>(); 
            for(ILinePart p:sortedParts){
                if(p instanceof ILinePart2){
                    sortedPartsWithILinePart2.add((ILinePart2) p);
                }
            }
            
            boolean lastWasComment=false;
            boolean writtenComment = false;
            if(sortedParts.size() == 0){
                continue;
            }
            if(sortedPartsWithILinePart2.size() == 1){
                //Ok, we need a special treatment for lines that only contain comments.
                //As it doesn't belong in the actual AST (it's just spit out in the middle of the parsing),
                //it can happen that it doesn't belong in the current indentation (and rather to the last indentation
                //found), so, we have to go on and check how we should indent it based on the previous line(s)
                ILinePart linePart = sortedPartsWithILinePart2.get(0);
                if(linePart.getToken() instanceof commentType && linePart instanceof ILinePart2){
                    ILinePart2 iLinePart2 = (ILinePart2) linePart;
                    commentType commentType = (commentType) linePart.getToken();
                    int col = commentType.beginColumn;
                    if(col == 0){ //yes, our indexing starts at 1.
                        writtenComment=true;
                        writeStateV2.writeRaw(iLinePart2.getString());
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
                            lastWasComment=true;
                            writtenComment=true;
                            writeStateV2.writeRaw(found.o2);
                            writeStateV2.writeRaw(iLinePart2.getString());
                        }
                    }
                }
            }
            
            
            if(!writtenComment){
                for(ILinePart linePart:sortedParts){
                    if(linePart instanceof ILinePart2){
                        //Note: on a write, if the last thing was a new line, it'll indent.
                        String tok = ((ILinePart2)linePart).getString();
                        
                        if(tok.length() == 1){
                            if(tok.charAt(0) == ';'){
                                writeStateV2.writeNewLine();
                                continue;
                            }
                            Tuple<Integer, Boolean> newLevel = updateLevels(tok);
                            if(newLevel != null){
                                if(newLevel.o2){
                                    writeStateV2.indent();
                                }else{
                                    writeStateV2.dedent();
                                }
                            }
                        }
                        
                        writeStateV2.write(prefs.getReplacement(tok));
                        if(linePart.getToken() instanceof commentType){
                            lastWasComment=true;
                        }
                        
                    }else if(linePart instanceof ILinePartStatementMark){
                        ILinePartStatementMark statementMark = (ILinePartStatementMark) linePart;
                        if(statementMark.isStart()){
                            statementLevel++;
                        }else{
                            statementLevel--;
                        }
                    }
                }
            }
            
            
            previousLines.add(new Tuple<PrettyPrinterDocLineEntry, String>(line, writeStateV2.getIndentString()));
            
            
            if(indentDiff > 0){
                while(indentDiff != 0){
                    indentDiff --;
                    writeStateV2.indent();
                }
                statementLevel = 0; //when we indent, we are at the body of a statement, so, lets start it over
            }else if(indentDiff < 0){
                while(indentDiff != 0){
                    indentDiff ++;
                    writeStateV2.dedent();
                }
                statementLevel = 0;
            }
            
            if(statementLevel != 0 && !lastWasComment){
                if(!isInLevel()){
                    continue;//don't write the new line if in a statement and not within paranthesis.
                }
            }
            writeStateV2.writeNewLine();
        }
        
        return writerEraserV2.getBuffer().toString();
    }
    
    
    private boolean isInLevel(){
        for(int i=0;i<3;i++){
            if(this.LEVELS[i] != 0){
                return true;
            }
        }
        return false;
    }
    

    /**
     * Updates the level for parens, brackets and braces based on the passed token and returns the new level and whether
     * it was increased (or null if nothing happened).
     */
    private Tuple<Integer, Boolean> updateLevels(String tok) {
        int use=-1;
        boolean increaseLevel=true;
        
        switch(tok.charAt(0)){
            case '(':
            case ')':
                use = this.LEVEL_PARENS;
            break;
            
            case '[':
            case ']':
                use = this.LEVEL_BRACKETS;
                break;
                
            case '{':
            case '}':
                use = this.LEVEL_BRACES;
                break;
        
        };
        if(use != -1){
            switch(tok.charAt(0)){
            case ']':
            case ')':
            case '}':
                increaseLevel = false;
            };
            
            if(increaseLevel){
                this.LEVELS[use] ++;
            }else{
                this.LEVELS[use] --;
            }
            return new Tuple<Integer, Boolean>(LEVELS[use], increaseLevel);
        }else{
            return null;
        }
    }
    

}
