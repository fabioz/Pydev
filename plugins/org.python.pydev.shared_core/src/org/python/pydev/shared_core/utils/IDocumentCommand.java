package org.python.pydev.shared_core.utils;

public interface IDocumentCommand {

    void setText(String string);

    String getText();

    int getOffset();

    boolean getDoIt();

    int getLength();

    void setShiftsCaret(boolean b);

    void setCaretOffset(int i);

    void setLength(int i);

    void setOffset(int i);

}
