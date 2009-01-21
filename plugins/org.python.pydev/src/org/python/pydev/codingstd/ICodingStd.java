package org.python.pydev.codingstd;

public interface ICodingStd {

    /**
     * @return whether the locals and attributes should be camel-case (otherwise they are separated with '_')
     */
    boolean localsAndAttrsCamelcase();
}
