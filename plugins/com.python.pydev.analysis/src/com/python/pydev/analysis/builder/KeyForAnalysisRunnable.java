/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.builder;

/**
 * Immutable.
 * 
 * Auto hash code and equals.
 * 
 * Used as the entry for the analysis runnable (to know if there is already some runnable analyzing a module
 * we want to analyze later)
 * 
 * @author Fabio
 */
public class KeyForAnalysisRunnable {

    public final String projectName;
    public final String moduleName;

    public KeyForAnalysisRunnable(String projectName, String moduleName) {
        this.projectName = projectName;
        this.moduleName = moduleName;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((moduleName == null) ? 0 : moduleName.hashCode());
        result = prime * result + ((projectName == null) ? 0 : projectName.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KeyForAnalysisRunnable other = (KeyForAnalysisRunnable) obj;
        if (moduleName == null) {
            if (other.moduleName != null)
                return false;
        } else if (!moduleName.equals(other.moduleName))
            return false;
        if (projectName == null) {
            if (other.projectName != null)
                return false;
        } else if (!projectName.equals(other.projectName))
            return false;
        return true;
    }

}
