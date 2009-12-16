package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.text.TextPresentation;
import org.python.pydev.editor.PyInformationPresenter.PyStyleRange;

import junit.framework.TestCase;

public class PyInformationPresenterTest extends TestCase {

    public void testStyleRanges() throws Exception {
        PyInformationPresenter presenter = new PyInformationPresenter();
        TextPresentation presentation = new TextPresentation();
        String str = "<pydev_hint_bold>bold</pydev_hint_bold> <pydev_link link=\"itemPointer\">link</pydev_link>";
        String handled = presenter.handlePydevTags(presentation, str);
        assertEquals("bold link", handled);
        Iterator it = presentation.getAllStyleRangeIterator();
        ArrayList<String> tagsReplaced = new ArrayList<String>();
        
        ArrayList<String> expected = new ArrayList<String>();
        expected.add("<pydev_hint_bold>");
        expected.add("<pydev_link link=\"itemPointer\">");
        
        while(it.hasNext()){
            PyStyleRange next = (PyStyleRange) it.next();
            tagsReplaced.add(next.tagReplaced);
        }
        assertEquals(expected, tagsReplaced);
    }
}
