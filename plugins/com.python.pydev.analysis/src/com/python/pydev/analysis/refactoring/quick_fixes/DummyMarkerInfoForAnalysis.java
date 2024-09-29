package com.python.pydev.analysis.refactoring.quick_fixes;

import org.python.pydev.core.IMarkerInfoForAnalysis;

public class DummyMarkerInfoForAnalysis implements IMarkerInfoForAnalysis {

    public Object message;
    public Object flake8MessageId;
    public int length;
    public int offset;
    public Integer pyDevAnalysisType;
    public Object pyLintMessageId;

    public DummyMarkerInfoForAnalysis(int pyDevAnalysisType, int offset, int length) {
        this.pyDevAnalysisType = pyDevAnalysisType;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public Object getPyLintMessageIdAttribute() {
        return pyLintMessageId;
    }

    @Override
    public Integer getPyDevAnalisysType() {
        return pyDevAnalysisType;
    }

    @Override
    public boolean hasPosition() {
        return true;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public void delete() {

    }

    @Override
    public Object getFlake8MessageId() {
        return flake8MessageId;
    }

    @Override
    public Object getMessage() {
        return message;
    }

}
