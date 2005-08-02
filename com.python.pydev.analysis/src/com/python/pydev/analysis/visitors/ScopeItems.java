/*
 * Created on 27/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ScopeItems {
    Map<String,Found> m = new HashMap<String,Found>();
    int ifSubScope = 0;
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

    public int getIfSubScope() {
        return ifSubScope;
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

}
