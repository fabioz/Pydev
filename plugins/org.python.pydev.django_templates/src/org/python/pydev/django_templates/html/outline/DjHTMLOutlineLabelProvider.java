package org.python.pydev.django_templates.html.outline;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.outline.DjLanguageOutlineLabelProvider;
import org.python.pydev.django_templates.outline.DjOutlineLabelProvider;

import com.aptana.editor.html.outline.HTMLOutlineLabelProvider;
import com.aptana.parsing.IParseState;

public class DjHTMLOutlineLabelProvider extends HTMLOutlineLabelProvider {

    private DjOutlineLabelProvider labelProvider;

    public DjHTMLOutlineLabelProvider(IParseState parseState) {
        labelProvider = new DjOutlineLabelProvider(parseState);
        addSubLanguage(IDjConstants.LANGUAGE_DJANGO_TEMPLATES_HTML, new DjLanguageOutlineLabelProvider());
    }

    @Override
    public Image getImage(Object element) {
        Image image = labelProvider.getImage(element);
        if(image != null){
            return image;
        }
        return super.getImage(element);
    }

    @Override
    public String getText(Object element) {
        String text = labelProvider.getText(element);
        if(text != null){
            return text;
        }
        return super.getText(element);
    }

}
