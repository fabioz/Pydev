package org.python.pydev.editor.codecompletion.templates;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.python.pydev.editor.TestIndentPrefs;

public class PyDocumentTemplateContextTest extends TestCase {

    public void testApply() throws Exception {
        Document doc = new Document("" +
        		"\n" +
        		"");
        PyDocumentTemplateContext context = new PyDocumentTemplateContext(new TemplateContextType(), doc, 0, 0, null, new TestIndentPrefs(true, 4));

        
        Template template = new Template("", "", "", "if a:\n\tpass", true);
        TemplateBuffer buffer = context.evaluate(template);
        assertEquals("if a:\n    pass", buffer.getString());
        
        context = new PyDocumentTemplateContext(new TemplateContextType(), doc, 0, 0, null, new TestIndentPrefs(false, 4));
        template = new Template("", "", "", "if a\n    print 'a:    '", true);
        buffer = context.evaluate(template);
        assertEquals("if a\n\tprint 'a:    '", buffer.getString());
        
        doc = new Document("" +
            "\n\t" +
        "");
        context = new PyDocumentTemplateContext(new TemplateContextType(), doc, doc.getLength(), 0, "\t", new TestIndentPrefs(false, 4));
        template = new Template("", "", "", "if a\n    print 'a:    '", true);
        buffer = context.evaluate(template);
        assertEquals("if a\n\t\tprint 'a:    '", buffer.getString());
        
        doc = new Document("" +
                "\n    " +
        "");
        context = new PyDocumentTemplateContext(new TemplateContextType(), doc, doc.getLength(), 0, "    ", new TestIndentPrefs(true, 4));
        template = new Template("", "", "", "if a\n\tprint 'a:    '", true);
        buffer = context.evaluate(template);
        assertEquals("if a\n        print 'a:    '", buffer.getString());
        
        
    }
}
