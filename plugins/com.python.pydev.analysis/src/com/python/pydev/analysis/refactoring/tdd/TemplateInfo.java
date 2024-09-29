package com.python.pydev.analysis.refactoring.tdd;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateException;
import org.python.pydev.core.log.Log;

public class TemplateInfo {

    public Template fTemplate;
    public TemplateContext fContext;
    public IRegion fRegion;

    public TemplateInfo(Template template, TemplateContext context, IRegion region) {
        this.fTemplate = template;
        this.fContext = context;
        this.fRegion = region;
    }

    /**
     * Returns the offset of the range in the document that will be replaced by
     * applying this template.
     *
     * @return the offset of the range in the document that will be replaced by
     *         applying this template
     * @since 3.1
     */
    protected final int getReplaceOffset() {
        int start;
        if (fContext instanceof DocumentTemplateContext) {
            DocumentTemplateContext docContext = (DocumentTemplateContext) fContext;
            start = docContext.getStart();
        } else {
            start = fRegion.getOffset();
        }
        return start;
    }

    /**
     * Returns the end offset of the range in the document that will be replaced
     * by applying this template.
     *
     * @return the end offset of the range in the document that will be replaced
     *         by applying this template
     * @since 3.1
     */
    protected final int getReplaceEndOffset() {
        int end;
        if (fContext instanceof DocumentTemplateContext) {
            DocumentTemplateContext docContext = (DocumentTemplateContext) fContext;
            end = docContext.getEnd();
        } else {
            end = fRegion.getOffset() + fRegion.getLength();
        }
        return end;
    }

    public void apply(IDocument document) {
        try {
            fContext.setReadOnly(false);
            int start;
            TemplateBuffer templateBuffer;
            try {
                // this may already modify the document (e.g. add imports)
                templateBuffer = fContext.evaluate(fTemplate);
            } catch (TemplateException e1) {
                return;
            }

            start = getReplaceOffset();
            int end = getReplaceEndOffset();

            // insert template string
            String templateString = templateBuffer.getString();
            document.replace(start, end - start, templateString);
        } catch (Exception e) {
            Log.log(e);
        }
    }

}