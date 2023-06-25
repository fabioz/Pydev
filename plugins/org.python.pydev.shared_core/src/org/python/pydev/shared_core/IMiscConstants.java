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
package org.python.pydev.shared_core;

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

    String PYLINT_PROBLEM_MARKER = "org.python.pydev.pylintproblemmarker";
    String PYLINT_MESSAGE_ID = "pylint_message_id";

    String MYPY_PROBLEM_MARKER = "org.python.pydev.mypyproblemmarker";
    String MYPY_MESSAGE_ID = "mypy_message_id";

    String RUFF_PROBLEM_MARKER = "org.python.pydev.ruffproblemmarker";
    String RUFF_MESSAGE_ID = "ruff_message_id";

    String FLAKE8_PROBLEM_MARKER = "org.python.pydev.flake8problemmarker";
    String FLAKE8_MESSAGE_ID = "flake8_message_id";

    int TYPE_UNUSED_IMPORT = 1;
    int TYPE_UNUSED_VARIABLE = 2;
    int TYPE_UNDEFINED_VARIABLE = 3;
    int TYPE_DUPLICATED_SIGNATURE = 4;
    int TYPE_REIMPORT = 5;
    int TYPE_UNRESOLVED_IMPORT = 6;
    int TYPE_NO_SELF = 7;
    int TYPE_UNUSED_WILD_IMPORT = 8;
    int TYPE_UNDEFINED_IMPORT_VARIABLE = 9;
    int TYPE_UNUSED_PARAMETER = 10;
    int TYPE_NO_EFFECT_STMT = 11;
    int TYPE_INDENTATION_PROBLEM = 12;
    int TYPE_UNDEFINED_VARIABLE_IN_SELF = 13; //Generated on demand by the tdd actions
    int TYPE_ASSIGNMENT_TO_BUILT_IN_SYMBOL = 14;
    int TYPE_PEP8 = 15;
    int TYPE_ARGUMENTS_MISATCH = 16;
    int TYPE_FSTRING_SYNTAX_ERROR = 17;
    int TYPE_INVALID_ENCODING = 18;

    public static final String PYDEV_ADD_RELAUNCH_IPROCESS_ATTR = "PYDEV_ADD_RELAUNCH_IPROCESS_ATTR";
    public static final String PYDEV_ADD_RELAUNCH_IPROCESS_ATTR_TRUE = "true";
}
