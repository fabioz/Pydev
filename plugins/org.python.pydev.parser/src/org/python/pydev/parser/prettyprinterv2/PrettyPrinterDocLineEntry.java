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
    public final int line;
    
    public PrettyPrinterDocLineEntry(int line) {
        this.line = line;
    }

    public ILinePart add(int beginCol, String string, Object token) {
        ILinePart linePart = new LinePart(beginCol, string, token, this);
        lineParts.add(linePart);
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
        this.indentDiff += 1;
        lineParts.add(new LinePartIndentMark(node.beginColumn, node, true, this));
    }

    public void dedent(SimpleNode node) {
        this.indentDiff -= 1;
        lineParts.add(new LinePartIndentMark(node.beginColumn, node, false, this));
    }

    public void indent(Token token) {
        this.indentDiff += 1;
        lineParts.add(new LinePartIndentMark(token.beginColumn, token, true, this));
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
        this.lineParts.add(new LinePartStatementMark(foundWithLowerLocation.getBeginCol(), node, true, this));
    }

    
    public void addEndStatementMark(ILinePart foundWithHigherLocation, stmtType node) {
        this.lineParts.add(new LinePartStatementMark(foundWithHigherLocation.getBeginCol(), node, false, this));
    }



}
