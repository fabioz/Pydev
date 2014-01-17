/******************************************************************************
* Copyright (C) 2013  Jeremy Carroll
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jeremy Carroll <jjc@syapse.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.core;

/**
 * This file is intended for constants that are shared
 * between plugins, to avoid otherwise unneeded dependencies
 * particularly cicrular ones, or spurious extension points.
 * 
 * @author Jeremy J Carroll
 *
 */
public interface IMiscConstants {

    String PYDEV_ANALYSIS_PROBLEM_MARKER = "com.python.pydev.analysis.pydev_analysis_problemmarker";
    String PYDEV_ANALYSIS_TYPE = "PYDEV_TYPE";
    String ANALYSIS_PARSER_OBSERVER_FORCE = "AnalysisParserObserver:force";
    String ANALYSIS_PARSER_OBSERVER_FORCE_IN_THIS_THREAD = "AnalysisParserObserver:force:inThisThread";
    int TYPE_UNUSED_IMPORT = 1;
}
