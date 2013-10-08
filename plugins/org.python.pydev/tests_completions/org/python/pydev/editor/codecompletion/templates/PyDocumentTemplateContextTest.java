/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.templates;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.python.pydev.editor.autoedit.TestIndentPrefs;

public class PyDocumentTemplateContextTest extends TestCase {

    public void testApply() throws Exception {
        Document doc = new Document("" + "\n" + "");
        PyDocumentTemplateContext context = new PyDocumentTemplateContext(new TemplateContextType(), doc, 0, 0, null,
                new TestIndentPrefs(true, 4));

        Template template = new Template("", "", "", "if a:\n\tpass", true);
        TemplateBuffer buffer = context.evaluate(template);
        assertEquals("if a:\n    pass", buffer.getString());

        context = new PyDocumentTemplateContext(new TemplateContextType(), doc, 0, 0, null, new TestIndentPrefs(false,
                4));
        template = new Template("", "", "", "if a\n    print 'a:    '", true);
        buffer = context.evaluate(template);
        assertEquals("if a\n\tprint 'a:    '", buffer.getString());

        doc = new Document("" + "\n\t" + "");
        context = new PyDocumentTemplateContext(new TemplateContextType(), doc, doc.getLength(), 0, "\t",
                new TestIndentPrefs(false, 4));
        template = new Template("", "", "", "if a\n    print 'a:    '", true);
        buffer = context.evaluate(template);
        assertEquals("if a\n\t\tprint 'a:    '", buffer.getString());

        doc = new Document("" + "\n    " + "");
        context = new PyDocumentTemplateContext(new TemplateContextType(), doc, doc.getLength(), 0, "    ",
                new TestIndentPrefs(true, 4));
        template = new Template("", "", "", "if a\n\tprint 'a:    '", true);
        buffer = context.evaluate(template);
        assertEquals("if a\n        print 'a:    '", buffer.getString());

        //let's check if we have a template with \n and a document with \r\n (it should be applied with \r\n)
        doc = new Document("" + "\r\n    " + "");
        context = new PyDocumentTemplateContext(new TemplateContextType(), doc, doc.getLength(), 0, "    ",
                new TestIndentPrefs(true, 4));
        template = new Template("", "", "", "if a\n\tprint 'a:    '", true);
        buffer = context.evaluate(template);
        assertEquals("if a\r\n        print 'a:    '", buffer.getString());

        //let's check if we have a template with \r\n and a document with \r (it should be applied with \r)
        doc = new Document("" + "\r    " + "");
        context = new PyDocumentTemplateContext(new TemplateContextType(), doc, doc.getLength(), 0, "    ",
                new TestIndentPrefs(true, 4));
        template = new Template("", "", "", "if a\r\n\tprint 'a:    '", true);
        buffer = context.evaluate(template);
        assertEquals("if a\r        print 'a:    '", buffer.getString());

    }
}
