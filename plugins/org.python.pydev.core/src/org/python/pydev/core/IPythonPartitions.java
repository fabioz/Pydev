package org.python.pydev.core;

import org.eclipse.jface.text.IDocument;

public interface IPythonPartitions {

    //this is just so that we don't have to break the interface
    public final static String PY_COMMENT           = "__python_comment";
    public final static String PY_SINGLELINE_STRING1 = "__python_singleline_string1";//'
    public final static String PY_SINGLELINE_STRING2 = "__python_singleline_string2";//"
    public final static String PY_MULTILINE_STRING1  = "__python_multiline_string1";//'''
    public final static String PY_MULTILINE_STRING2  = "__python_multiline_string2";//"""
    public final static String PY_BACKQUOTES        = "__python_backquotes";
    public final static String PY_DEFAULT           = IDocument.DEFAULT_CONTENT_TYPE;
    
    public final static String[] types = {PY_COMMENT, PY_SINGLELINE_STRING1, PY_SINGLELINE_STRING2, 
        PY_MULTILINE_STRING1, PY_MULTILINE_STRING2, PY_BACKQUOTES};
    public static final String PYTHON_PARTITION_TYPE = "__PYTHON_PARTITION_TYPE";

}
