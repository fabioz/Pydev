package com.python.pydev.analysis.search;

public interface ICustomMatch {

    int getOriginalOffset();

    int getOriginalLength();

    ICustomLineElement getLineElement();

}
