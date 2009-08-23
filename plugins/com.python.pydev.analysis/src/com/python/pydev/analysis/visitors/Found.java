/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.IToken;
import org.python.pydev.core.structure.FastStringBuffer;

import com.python.pydev.analysis.visitors.ImportChecker.ImportInfo;

public class Found implements Iterable<GenAndTok>{
    
    private List<GenAndTok> found = new ArrayList<GenAndTok>();
    
    /**
     * Identifies if the current token has been used or not
     */
    private boolean used = false;

    /**
     * If this is an import, it may be resolved to some module and some token within that module...
     */
    public ImportInfo importInfo;
    
    public Found(IToken tok, IToken generator, int scopeId, ScopeItems scopeFound){
        this.found.add(new GenAndTok(generator, tok, scopeId, scopeFound));
    }

    /**
     * @param used The used to set.
     */
    public void setUsed(boolean used) {
        this.used = used;
    }

    /**
     * @return Returns the used.
     */
    public boolean isUsed() {
        return used;
    }

    public Iterator<GenAndTok> iterator() {
        return this.found.iterator();
    }

    public void addGeneratorToFound(IToken generator2, IToken tok2, int scopeId, ScopeItems scopeFound) {
        this.found.add(new GenAndTok(generator2, tok2, scopeId, scopeFound));
    }
    
    public void addGeneratorsFromFound(Found found2) {
        this.found.addAll(found2.found);
    }


    public GenAndTok getSingle() {
        return found.get(found.size() -1); //always returns the last (this is the one that is binded at the current place in the scope)
    }
    
    public List<GenAndTok> getAll() {
        return found;
    }

    public boolean isImport() {
        return getSingle().generator.isImport();
    }
    
    @Override
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("Found { (used:");
        buffer.append(used);
        buffer.append(") [");
        
        for (GenAndTok g : found) {
            buffer.appendObject(g);
            buffer.append("  ");
        }
        buffer.append(" ]}");
        return buffer.toString();
    }

    public boolean isWildImport() {
        return getSingle().generator.isWildImport();
    }


}