package org.python.pydev.debug.ui;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IHyperlink;

public interface ILinkContainer {

    void addLink(IHyperlink link, int offset, int length);

    String getContents(int lineOffset, int lineLength) throws BadLocationException;

}
