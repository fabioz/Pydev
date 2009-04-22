package org.python.pydev.navigator.elements;

public interface ISortedElement {

    int RANK_ERROR = -1;
    int RANK_SOURCE_FOLDER = 0;
    int RANK_PYTHON_FOLDER = 1;
    int RANK_PYTHON_FILE = 2;
    int RANK_PYTHON_RESOURCE = 3;
    int RANK_PYTHON_NODE = 4;
    

    /**
     * @return the ranking for the object. Lower values have higher priorities
     */
    int getRank();
}
