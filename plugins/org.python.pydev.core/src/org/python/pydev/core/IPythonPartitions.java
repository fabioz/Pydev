/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import org.eclipse.jface.text.IDocument;

public interface IPythonPartitions {

    //this is just so that we don't have to break the interface
    public final static String PY_COMMENT = "__python_comment";

    public final static String PY_SINGLELINE_BYTES1 = "__python_singleline_string1";//'
    public final static String PY_SINGLELINE_BYTES2 = "__python_singleline_string2";//"

    public final static String PY_MULTILINE_BYTES1 = "__python_multiline_string1";//'''
    public final static String PY_MULTILINE_BYTES2 = "__python_multiline_string2";//"""

    public final static String PY_SINGLELINE_UNICODE1 = "__python_singleline_unicode1";//'
    public final static String PY_SINGLELINE_UNICODE2 = "__python_singleline_unicode2";//"

    public final static String PY_MULTILINE_UNICODE1 = "__python_multiline_unicode1";//'''
    public final static String PY_MULTILINE_UNICODE2 = "__python_multiline_unicode";//"""

    public final static String PY_SINGLELINE_BYTES_OR_UNICODE1 = "__python_singleline_bytes_or_unicode1";//'
    public final static String PY_SINGLELINE_BYTES_OR_UNICODE2 = "__python_singleline_bytes_or_unicode2";//"

    public final static String PY_MULTILINE_BYTES_OR_UNICODE1 = "__python_multiline_bytes_or_unicode1";//'''
    public final static String PY_MULTILINE_BYTES_OR_UNICODE2 = "__python_multiline_bytes_or_unicode2";//"""

    public final static String PY_BACKQUOTES = "__python_backquotes";
    public final static String PY_DEFAULT = IDocument.DEFAULT_CONTENT_TYPE;

    public final static String[] types = {
            PY_COMMENT,

            PY_SINGLELINE_BYTES1,
            PY_SINGLELINE_BYTES2,
            PY_MULTILINE_BYTES1,
            PY_MULTILINE_BYTES2,

            PY_SINGLELINE_UNICODE1,
            PY_SINGLELINE_UNICODE2,
            PY_MULTILINE_UNICODE1,
            PY_MULTILINE_UNICODE2,

            PY_SINGLELINE_BYTES_OR_UNICODE1,
            PY_SINGLELINE_BYTES_OR_UNICODE2,
            PY_MULTILINE_BYTES_OR_UNICODE1,
            PY_MULTILINE_BYTES_OR_UNICODE2,

            PY_BACKQUOTES
    };
    public static final String PYTHON_PARTITION_TYPE = "__PYTHON_PARTITION_TYPE";

}
