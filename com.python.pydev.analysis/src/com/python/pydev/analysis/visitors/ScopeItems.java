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
import java.util.Stack;

import org.python.pydev.core.IToken;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.ast.TryExcept;

public class ScopeItems {
    private Map<String,Found> m = new HashMap<String,Found>();
    
    /**
     * Stack for names that should not generate warnings, such as builtins, method names, etc.
     */
    public Map<String, Tuple<IToken, Found>> namesToIgnore = new HashMap<String, Tuple<IToken, Found>>();
    
    public int ifSubScope = 0;
    public Stack<TryExcept> tryExceptSubScope = new Stack<TryExcept>();
    private int scopeId;
    private int scopeType;

    public ScopeItems(int scopeId, int scopeType) {
        this.scopeId = scopeId;
        this.scopeType = scopeType;
    }

    public Found get(String rep) {
        return m.get(rep);
    }

    public void put(String rep, Found found) {
        m.put(rep, found);
    }

    public Collection<Found> values() {
        return m.values();
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
    
	public Stack<TryExcept> getCurrTryExceptNodes() {
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
        for (Map.Entry<String, Found> entry : m.entrySet()) {
            buffer.append(entry.getKey());
            buffer.append(": contains ");
            buffer.append(entry.getValue());
            buffer.append("\n");
        }
        return buffer.toString();
    }
    
    public Set<Map.Entry<String, Found>> entrySet(){
    	return m.entrySet();
    }

    /**
     * @return all the used items
     */
    public List<Tuple<String, Found>> getUsedItems() {
        ArrayList<Tuple<String, Found>> found = new ArrayList<Tuple<String, Found>>();
        for (Map.Entry<String, Found> entry : m.entrySet()) {
            if(entry.getValue().isUsed()){
                found.add(new Tuple<String, Found>(entry.getKey(), entry.getValue()));
            }
        }
        return found;
    }



}
