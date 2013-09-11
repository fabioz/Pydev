/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 10/12/2005
 */
package com.python.pydev.analysis.additionalinfo.dependencies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is created to encapsulate structural changes that happen to the code.
 * 
 * Its information contains:
 * - The module that was just changed
 * - The tokens that were added to the module
 * - The tokens that were removed from the module
 * 
 * If the module was just added or removed, its name will appear in the 'added' or 'removed' tokens
 * 
 * @author Fabio
 */
public class PyStructuralChange {

    private String module;
    private List<String> addedTokens = new ArrayList<String>();
    private List<String> removedTokens = new ArrayList<String>();

    public void setModule(String module) {
        this.module = module;
    }

    public void addToken(String token) {
        this.addedTokens.add(token);
    }

    public void addRemovedToken(String token) {
        this.removedTokens.add(token);
    }

    public Set<String> getChangedTokens() {
        HashSet<String> toks = new HashSet<String>();

        toks.addAll(addedTokens);
        toks.addAll(removedTokens);

        return toks;
    }

    public String getModule() {
        return this.module;
    }

}
