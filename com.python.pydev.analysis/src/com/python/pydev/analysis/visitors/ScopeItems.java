/*
 * Created on 27/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.pydev.core.IToken;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.ast.TryExcept;

public class ScopeItems {
    private Map<String,List<Found>> m = new HashMap<String,List<Found>>();
    
    /**
     * Stack for names that should not generate warnings, such as builtins, method names, etc.
     */
    public Map<String, Tuple<IToken, Found>> namesToIgnore = new HashMap<String, Tuple<IToken, Found>>();
    
    public int ifSubScope = 0;
    public FastStack<TryExcept> tryExceptSubScope = new FastStack<TryExcept>();
    private int scopeId;
    private int scopeType;

    public ScopeItems(int scopeId, int scopeType) {
        this.scopeId = scopeId;
        this.scopeType = scopeType;
    }


    public Found getLastAppearance(String rep) {
        List<Found> foundItems = m.get(rep);
        if(foundItems == null || foundItems.size() == 0){
            return null;
        }
        return foundItems.get(foundItems.size()-1);
    }

    public List<Found> getAll(String rep){
        List<Found> r = m.get(rep);
        if(r == null){
            return new ArrayList<Found>(0);
        }
        return r;
    }
    
    public void put(String rep, Found found) {
        List<Found> foundItems = m.get(rep);
        if(foundItems == null){
            foundItems = new ArrayList<Found>();
            m.put(rep, foundItems);
        }
        
        foundItems.add(found);
    }

    public Collection<Found> values() {
        ArrayList<Found> ret = new ArrayList<Found>();
        for (List<Found> foundItems : m.values()) {
            ret.addAll(foundItems);
        }
        return ret;
    }

    public void addIfSubScope() {
        ifSubScope++;
    }

    public void removeIfSubScope() {
        ifSubScope--;
    }

    public void addTryExceptSubScope(TryExcept node) {
    	tryExceptSubScope.push(node);
    }
    
    public void removeTryExceptSubScope() {
    	tryExceptSubScope.pop();
    }
    
	public FastStack<TryExcept> getCurrTryExceptNodes() {
		return tryExceptSubScope;
	}

	public boolean getIsInSubSubScope() {
		return ifSubScope != 0 || tryExceptSubScope.size() != 0;
	}

    /**
     * @return Returns the scopeId.
     */
    public int getScopeId() {
        return scopeId;
    }
    
    /**
     * @return Returns the scopeType.
     */
    public int getScopeType() {
        return scopeType;
    }
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("ScopeItem (type:");
        buffer.append(Scope.getScopeTypeStr(scopeType));
        buffer.append(")\n");
        for (Map.Entry<String, List<Found>> entry : m.entrySet()) {
            buffer.append(entry.getKey());
            buffer.append(": contains ");
            buffer.append(entry.getValue());
            buffer.append("\n");
        }
        return buffer.toString();
    }
    

    /**
     * @return all the used items
     */
    public List<Tuple<String, Found>> getUsedItems() {
        ArrayList<Tuple<String, Found>> found = new ArrayList<Tuple<String, Found>>();
        for (Map.Entry<String, List<Found>> entry : m.entrySet()) {
            for (Found f : entry.getValue()) {
                if(f.isUsed()){
                    found.add(new Tuple<String, Found>(entry.getKey(), f));
                }
            }
        }
        return found;
    }



}
