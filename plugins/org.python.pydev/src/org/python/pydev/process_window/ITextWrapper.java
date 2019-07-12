package org.python.pydev.process_window;

import org.eclipse.swt.widgets.Control;

public interface ITextWrapper {

    void setText(String string);

    char[] getTextChars();

    void append(String substring);

    String getText();

    Control getControl();

}
