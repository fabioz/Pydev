package org.python.pydev.parser.prettyprinterv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.ast.stmtType;

public class PrettyPrinterDocLineEntry {
    
    private ArrayList<ILinePart> lineParts = new ArrayList<ILinePart>();
    private int indentDiff;
    private int emptyLinesRequiredAfterDedent;
    public final int line;
    
    public PrettyPrinterDocLineEntry(int line) {
        this.line = line;
    }

    public ILinePart add(int beginCol, String string, Object token) {
        ILinePart linePart = new LinePart(beginCol, string, token, this);
        lineParts.add(linePart);
        return linePart;
    }
    
    public ILinePart addBefore(int beginCol, String string, Object token) {
        ILinePart linePart = new LinePart(beginCol, string, token, this);
        
        //Now, on the start, we want to add it before any existing in the same column.
        for(int i=0;i<this.lineParts.size();i++){
            if(beginCol == this.lineParts.get(i).getBeginCol()){
                this.lineParts.add(i, linePart);
                return linePart;
            }
        }
        this.lineParts.add(linePart);
        return linePart;
    }

    
    @Override
    public String toString() {
        sortLineParts();
        FastStringBuffer buf = new FastStringBuffer();
        for(ILinePart c:lineParts){
            if(c instanceof ILinePart2){
                buf.append(((ILinePart2)c).getString());
            }else{
                buf.append(c.toString());
            }
            buf.append(" ");
        }
        return buf.toString();
    }

    
    private void sortLineParts() {
        Collections.sort(lineParts, new Comparator<ILinePart>() {

            public int compare(ILinePart o1, ILinePart o2) {
                return (o1.getBeginCol()<o2.getBeginCol() ? -1 : (o1.getBeginCol()==o2.getBeginCol() ? 0 : 1));
            }
        });
    }

    public List<ILinePart> getSortedParts() {
        sortLineParts();
        return this.lineParts;
    }


    public void indent(SimpleNode node) {
        indent(node, false);
    }
    
    public LinePartIndentMark indent(SimpleNode node, boolean requireNewLine) {
        this.indentDiff += 1;
        LinePartIndentMark linePartIndentMark = new LinePartIndentMark(node.beginColumn, node, true, this);
        linePartIndentMark.setRequireNewLine(requireNewLine);
        lineParts.add(linePartIndentMark);
        return linePartIndentMark;
    }

    public void dedent(int emptyLinesRequiredAfterDedent) {
        if(this.emptyLinesRequiredAfterDedent < emptyLinesRequiredAfterDedent){
            this.emptyLinesRequiredAfterDedent = emptyLinesRequiredAfterDedent;
        }
        this.indentDiff -= 1;
        LinePartIndentMark dedentMark = new LinePartIndentMark(Integer.MAX_VALUE, "", false, this);
        dedentMark.setEmptyLinesRequiredAfterDedent(emptyLinesRequiredAfterDedent);
        lineParts.add(dedentMark);
    }

    public void indent(Token token, boolean requireNewLine) {
        this.indentDiff += 1;
        LinePartIndentMark linePartIndentMark = new LinePartIndentMark(token.beginColumn, token, true, this);
        linePartIndentMark.setRequireNewLine(requireNewLine);
        lineParts.add(linePartIndentMark);
    }
    
    public void indentAfter(ILinePart after, boolean requireNewLine) {
        this.indentDiff += 1;
        LinePartIndentMark linePartIndentMark = new LinePartIndentMark(after.getBeginCol(), after.getToken(), true, this);
        linePartIndentMark.setRequireNewLine(requireNewLine);
        lineParts.add(lineParts.indexOf(after)+1, linePartIndentMark);
    }


    public int getIndentDiff() {
        return this.indentDiff;
    }

    public int getFirstCol() {
        sortLineParts();
        if(this.lineParts.size() > 0){
            return this.lineParts.get(0).getBeginCol();
        }
        return -1;
    }

    public void addStartStatementMark(ILinePart foundWithLowerLocation, stmtType node) {
        int beginCol = foundWithLowerLocation.getBeginCol();
        sortLineParts();
        
        //Now, on the start, we want to add it before any existing in the same column.
        for(int i=0;i<this.lineParts.size();i++){
            if(beginCol == this.lineParts.get(i).getBeginCol()){
                this.lineParts.add(i, new LinePartStatementMark(beginCol, node, true, this));
                return;
            }
        }
        this.lineParts.add(new LinePartStatementMark(beginCol, node, true, this));
    }

    
    public void addEndStatementMark(ILinePart foundWithHigherLocation, stmtType node) {
        this.lineParts.add(new LinePartStatementMark(foundWithHigherLocation.getBeginCol(), node, false, this));
    }

    public int getNewLinesRequired() {
        return this.emptyLinesRequiredAfterDedent;
    }

    public LinePartRequireMark addRequireMark(int beginColumn, String string) {
        LinePartRequireMark mark = new LinePartRequireMark(beginColumn, string, this);
        this.lineParts.add(mark);
        return mark;
    }
    
    public LinePartRequireMark addRequireMark(int beginColumn, String ... string) {
        LinePartRequireMark mark = new LinePartRequireMark(beginColumn, this, string);
        this.lineParts.add(mark);
        return mark;
    }

    public LinePartRequireIndentMark addRequireIndentMark(int beginColumn, String string) {
        LinePartRequireIndentMark ret = new LinePartRequireIndentMark(beginColumn, string, this);
        this.lineParts.add(ret);
        return ret;
    }

    public LinePartRequireMark addRequireMarkBefore(ILinePart o1, String string) {
        LinePartRequireMark linePart = new LinePartRequireMark(o1.getBeginCol(), string, this);
        for(int i=0;i<this.lineParts.size();i++){
            if(o1 == this.lineParts.get(i)){
                this.lineParts.add(i, linePart);
                return linePart;
            }
        }
        this.lineParts.add(linePart);
        return linePart;
    }

    public LinePartRequireMark addRequireMarkAfterBefore(ILinePart o1, String string) {
        LinePartRequireMark linePart = new LinePartRequireMark(o1.getBeginCol(), string, this);
        for(int i=0;i<this.lineParts.size();i++){
            if(o1 == this.lineParts.get(i)){
                this.lineParts.add(i+1, linePart);
                return linePart;
            }
        }
        this.lineParts.add(linePart);
        return linePart;
    }






}
