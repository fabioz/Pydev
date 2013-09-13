/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.structure;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a class that can be 'decoratable' with additional information
 */
public class DecoratableObject {
    /**
     * This is used so that specific refactoring engines can add information regarding its specifics in
     * the request.
     */
    private Map<String, Object> additionalRefactoringInfo;

    /**
     * Only initialize on request
     */
    private Map<String, Object> getAdditionalRefactoringInfo() {
        if (additionalRefactoringInfo == null) {
            additionalRefactoringInfo = new HashMap<String, Object>();
        }
        return additionalRefactoringInfo;
    }

    /**
     * @param key this is the key for which we have some additional value relative to the
     * refactoring request using it
     * @param defaultValue this is the default value that should be returned if there
     * is currently no value for the given key
     * @return the additional info (if available) or the default specified
     */
    public Object getAdditionalInfo(String key, Object defaultValue) {
        Object val = this.getAdditionalRefactoringInfo().get(key);
        if (val == null) {
            return defaultValue;
        }
        return val;
    }

    /**
     * Set some value for some additional info for this request.
     */
    public void setAdditionalInfo(String key, Object value) {
        this.getAdditionalRefactoringInfo().put(key, value);
    }

}
