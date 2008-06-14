/**
 * 
 */
package org.python.pydev.editor.codecompletion.templates;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.python.pydev.core.docutils.DocUtils;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.autoedit.IIndentPrefs;

/**
 * Makes a custom evaluation of the template buffer to be created (to put it in the correct indentation and 
 * change tabs to spaces -- if needed). 
 * 
 * @author Fabio
 */
public final class PyDocumentTemplateContext extends DocumentTemplateContext {
    
    private final String indentTo;
    private IIndentPrefs indentPrefs;

    public PyDocumentTemplateContext(TemplateContextType type, IDocument document, int offset, int length, String indentTo, IIndentPrefs indentPrefs) {
        super(type, document, offset, length);
        this.indentTo = indentTo;
        this.indentPrefs = indentPrefs;
    }
    
    public PyDocumentTemplateContext(TemplateContextType type, IDocument document, int offset, int length, String indentTo, ITextViewer viewer) {
        this(type, document, offset, length, indentTo, getIndentPrefs(viewer));
    }

    /**
     * @return the indent preferences to be used.
     */
    private static IIndentPrefs getIndentPrefs(ITextViewer viewer) {
        if(viewer instanceof PyEdit){
            PyEdit pyEdit = (PyEdit) viewer;
            return pyEdit.getIndentPrefs();
        }else{
            return DefaultIndentPrefs.get();
        }
    }
    
    @Override
    public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
        
        if (!canEvaluate(template)){
            return null;
        }
        
        
         String spacesIndentString = DocUtils.createSpaceString(indentPrefs.getTabWidth());        
        
        
        
        //indent to needed level and
        //replace any \t for the indentation string 
        String pattern = template.getPattern();
        List<String> splitted = StringUtils.splitInLines(pattern);
        
        boolean changed = false;
        if(indentPrefs.getUseSpaces()){
            if(pattern.indexOf("\t") != -1){
                template = createNewTemplate(template, StringUtils.replaceAll(pattern, "\t", spacesIndentString));
                changed = true;
            }
        }else{
            if(pattern.indexOf(spacesIndentString) != -1){
                FastStringBuffer newPattern = new FastStringBuffer();
                FastStringBuffer newTabsIndent = new FastStringBuffer();
                
                for(int i=0; i<splitted.size();i++){
                    String string = splitted.get(i);
                    
                    int spacesFound = 0;
                    while(string.length() > 0 && string.charAt(0) == ' '){
                        string = string.substring(1);
                        spacesFound += 1;
                    }
                    
                    int tabsToAdd = 0;
                    if(spacesFound > 0){
                        tabsToAdd = spacesFound / spacesIndentString.length();
                        if(spacesFound % spacesIndentString.length() != 0){
                            tabsToAdd += 1;
                        }
                        newTabsIndent.clear();
                        for(int j = 0; j< tabsToAdd; j++){
                            newTabsIndent.append("\t");
                        }
                        newPattern.append(newTabsIndent);
                    }
                    newPattern.append(string);
                }
                template = createNewTemplate(template, newPattern.toString());
                changed = true;
            }
        }
        
        //recreate it (if needed). 
        if(changed){
            pattern = template.getPattern();
            splitted = StringUtils.splitInLines(pattern);
        }
        
        if(splitted.size() > 1 && indentTo != null && indentTo.length() > 0){
            FastStringBuffer buffer = new FastStringBuffer(splitted.get(0), 128);
            for (int i=1; i<splitted.size();i++) { //we don't want to get the first line
                buffer.append(indentTo);
                buffer.append(splitted.get(i));
            }
            //just to change the pattern...
            template = createNewTemplate(template, buffer.toString());
            
        }
        
        TemplateTranslator translator= new TemplateTranslator();
        TemplateBuffer templateBuffer= translator.translate(template);

        getContextType().resolve(templateBuffer, this);

        return templateBuffer;
    }

    private Template createNewTemplate(Template template, String newString) {
        return new Template(template.getName(), template.getDescription(), template.getContextTypeId(), newString, template.isAutoInsertable());
    }
}