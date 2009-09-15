package org.python.pydev.parser.prettyprinterv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;

public class PrettyPrinterDocLineEntry {
    
    private ArrayList<LinePart> lineParts = new ArrayList<LinePart>();
    private int indentDiff;
    public final int line;
    
    public PrettyPrinterDocLineEntry(int line) {
        this.line = line;
    }

    public LinePart add(int beginCol, String string, Object token) {
        LinePart linePart = new LinePart(beginCol, string, token, this);
        lineParts.add(linePart);
        return linePart;
    }
    
    @Override
    public String toString() {
        sortLineParts();
        FastStringBuffer buf = new FastStringBuffer();
        if(indentDiff > 0){
            buf.append("INDENT ");
        }
        if(indentDiff < 0){
            buf.append("DEDENT ");
        }
        for(LinePart c:lineParts){
            buf.append(c.string);
            buf.append(" ");
        }
        return buf.toString();
    }

    
    private void sortLineParts() {
        Collections.sort(lineParts, new Comparator<LinePart>() {

            @Override
            public int compare(LinePart o1, LinePart o2) {
                return (o1.beginCol<o2.beginCol ? -1 : (o1.beginCol==o2.beginCol ? 0 : 1));
            }
        });
    }

    public List<LinePart> getSortedParts() {
        sortLineParts();
        return this.lineParts;
    }


    public void indent(SimpleNode node) {
        this.indentDiff += 1;
    }

    public void dedent(SimpleNode node) {
        this.indentDiff -= 1;
    }

    public void indent(Token token) {
        this.indentDiff += 1;
    }

    public int getIndentDiff() {
        return this.indentDiff;
    }

    public int getFirstCol() {
        sortLineParts();
        if(this.lineParts.size() > 0){
            return this.lineParts.get(0).beginCol;
        }
        return -1;
    }

}
