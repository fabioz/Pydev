/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 27/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.python.pydev.core.IToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.Tuple;

public final class ScopeItems {

    /**
     * This is the class that is used to wrap the try..except node (so that we can add additional info to it).
     */
    public static class TryExceptInfo {
        public TryExcept except;
        private Map<String, List<Found>> importsMapInTryExcept = new HashMap<String, List<Found>>();

        public TryExceptInfo(TryExcept except) {
            this.except = except;
        }

        /**
         * When we add a new import found within a try..except ImportError, we mark the previous import
         * with the same name as used (as this one will redefine it in an expected way).
         */
        public void addFoundImportToTryExcept(Found found) {
            if (!found.isImport()) {
                return;
            }
            String rep = found.getSingle().generator.getRepresentation();
            List<Found> importsListInTryExcept = importsMapInTryExcept.get(rep);
            if (importsListInTryExcept == null) {
                importsListInTryExcept = new ArrayList<Found>();
                importsMapInTryExcept.put(rep, importsListInTryExcept);

            } else if (importsListInTryExcept.size() > 0) {
                importsListInTryExcept.get(importsListInTryExcept.size() - 1).setUsed(true);
            }

            importsListInTryExcept.add(found);
        }
    }

    /**
     * @return the TryExcept from a try..except ImportError if we are currently within such a scope
     * (otherwise will return null;.
     */
    public ScopeItems.TryExceptInfo getTryExceptImportError() {
        for (ScopeItems.TryExceptInfo except : getCurrTryExceptNodes()) {
            for (excepthandlerType handler : except.except.handlers) {
                if (handler.type != null) {
                    String rep = NodeUtils.getRepresentationString(handler.type);
                    if (rep != null && rep.equals("ImportError")) {
                        return except;
                    }
                }
            }
        }
        return null;
    }

    private final Map<String, List<Found>> m = new HashMap<String, List<Found>>();

    /**
     * Stack for names that should not generate warnings, such as builtins, method names, etc.
     */
    public final Map<String, Tuple<IToken, Found>> namesToIgnore = new HashMap<String, Tuple<IToken, Found>>();

    public final FastStack<If> ifNodes = new FastStack<>(10);
    public int statementSubScope = 0;
    public final FastStack<TryExceptInfo> tryExceptSubScope = new FastStack<TryExceptInfo>(10);
    private final int scopeId;
    private final int scopeType;
    public final int globalClassOrMethodScopeType;
    public final SimpleNode scopeNode;

    public ScopeItems(int scopeId, int scopeType, int globalClassOrMethodScopeType, SimpleNode node) {
        this.scopeId = scopeId;
        this.scopeType = scopeType;
        this.globalClassOrMethodScopeType = globalClassOrMethodScopeType;
        this.scopeNode = node;
    }

    public Found getLastAppearance(String rep) {
        List<Found> foundItems = m.get(rep);
        if (foundItems == null || foundItems.size() == 0) {
            return null;
        }
        return foundItems.get(foundItems.size() - 1);
    }

    public void setAllUsed() {
        for (List<Found> list : m.values()) {
            int len = list.size();
            for (int i = 0; i < len; i++) {
                list.get(i).setUsed(true);
            }
        }
    }

    /**
     * @return a list with all the found items in this scope
     */
    public Collection<List<Found>> getAll() {
        return m.values();
    }

    /**
     * @return all the found items that match the given representation.
     */
    public List<Found> getAll(String rep) {
        List<Found> r = m.get(rep);
        if (r == null) {
            return new ArrayList<Found>(0);
        }
        return r;
    }

    public void put(String rep, Found found) {
        List<Found> foundItems = m.get(rep);
        if (foundItems == null) {
            foundItems = new ArrayList<Found>();
            m.put(rep, foundItems);
        }

        foundItems.add(found);
    }

    // Cache on whether it's currently type-checking.
    private Boolean inTypeChecking = null;

    public boolean isInTypeChecking() {
        if (statementSubScope == 0 || this.ifNodes.size() == 0) {
            return false;
        }

        if (inTypeChecking != null) {
            return inTypeChecking;
        }

        inTypeChecking = false;
        for (ListIterator<If> it = this.ifNodes.iterator(); it.hasNext();) {
            If ifStatement = it.next();
            if (isTypeCheckingIfStatement(ifStatement)) {
                inTypeChecking = true;
                break;
            }
        }
        return inTypeChecking;
    }

    private boolean isTypeCheckingIfStatement(If ifStatement) {
        String rep = NodeUtils.getFullRepresentationString(ifStatement.test);
        if (rep != null && (rep.equals("typing.TYPE_CHECKING") || rep.equals("TYPE_CHECKING"))) {
            return true;
        }
        return false;
    }

    public void addIfSubScope(If node) {
        this.ifNodes.push(node);
        statementSubScope++;
        inTypeChecking = null;
    }

    public void removeIfSubScope() {
        this.ifNodes.pop();
        statementSubScope--;
        inTypeChecking = null;
    }

    public void addStatementSubScope() {
        statementSubScope++;
    }

    public void removeStatementSubScope() {
        statementSubScope--;
    }

    public void addTryExceptSubScope(TryExcept node) {
        tryExceptSubScope.push(new TryExceptInfo(node));
        statementSubScope++;
    }

    public void removeTryExceptSubScope() {
        tryExceptSubScope.pop();
        statementSubScope--;
    }

    public FastStack<TryExceptInfo> getCurrTryExceptNodes() {
        return tryExceptSubScope;
    }

    public boolean getIsInStatementSubScope() {
        return statementSubScope != 0;
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
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("ScopeItem (type:");
        buffer.append(Scope.getScopeTypeStr(scopeType));
        buffer.append(")\n");
        for (Map.Entry<String, List<Found>> entry : m.entrySet()) {
            buffer.append(entry.getKey());
            buffer.append(": contains ");
            buffer.append(entry.getValue().toString());
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
                if (f.isUsed()) {
                    found.add(new Tuple<String, Found>(entry.getKey(), f));
                }
            }
        }
        return found;
    }

}
