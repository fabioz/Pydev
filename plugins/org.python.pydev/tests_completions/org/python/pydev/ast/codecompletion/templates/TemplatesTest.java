package org.python.pydev.ast.codecompletion.templates;

import org.python.pydev.editor.templates.PyTemplatesDefault;
import org.python.pydev.shared_core.string.StringUtils;

import junit.framework.TestCase;

public class TemplatesTest extends TestCase {

    public void testIsoDate() throws Exception {
        String found = PyTemplatesDefault.IsoDate().resolveAll(null)[0];
        assertEquals(StringUtils.count(found, '-'), 2);

        found = PyTemplatesDefault.IsoDate1().resolveAll(null)[0];
        assertEquals(StringUtils.count(found, '-'), 2);
        assertEquals(StringUtils.count(found, ':'), 1);

        found = PyTemplatesDefault.IsoDate2().resolveAll(null)[0];
        assertEquals(StringUtils.count(found, '-'), 2);
        assertEquals(StringUtils.count(found, ':'), 2);
    }

}
