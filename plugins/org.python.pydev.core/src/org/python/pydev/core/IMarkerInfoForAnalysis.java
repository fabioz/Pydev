package org.python.pydev.core;

public interface IMarkerInfoForAnalysis {

    Object getPyLintMessageIdAttribute();

    Integer getPyDevAnalisysType();

    boolean hasPosition();

    int getOffset();

    int getLength();

    void delete();

    Object getFlake8MessageId();

    Object getMessage();
}
