package org.python.pydev.django_templates.html.outline;

import java.util.StringTokenizer;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.html.parsing.DjangoTemplatesNode;

import com.aptana.editor.html.Activator;
import com.aptana.editor.html.outline.HTMLOutlineLabelProvider;
import com.aptana.parsing.IParseState;
import com.aptana.parsing.ast.ParseNode;

public class DjHTMLOutlineLabelProvider extends HTMLOutlineLabelProvider {

    private static final Image DJANGO_TEMPLATES_NODE_ICON = Activator.getImage("icons/element.gif"); //$NON-NLS-1$

    private static final int TRIM_TO_LENGTH = 20;

    private IParseState fParseState;

    public DjHTMLOutlineLabelProvider(IParseState parseState) {
        fParseState = parseState;
        addSubLanguage(IDjConstants.LANGUAGE_DJANGO_TEMPLATES, new DjOutlineLabelProvider());
    }

    @Override
    protected Image getDefaultImage(Object element) {
        if (element instanceof DjangoTemplatesNode) {
            return DJANGO_TEMPLATES_NODE_ICON;
        }
        return super.getDefaultImage(element);
    }

    @Override
    protected String getDefaultText(Object element) {
        if (element instanceof DjangoTemplatesNode) {
            return getDisplayText((DjangoTemplatesNode) element);
        }
        return super.getDefaultText(element);
    }

    private String getDisplayText(DjangoTemplatesNode script) {
        StringBuilder text = new StringBuilder();
        text.append(script.getStartTag()).append(" "); //$NON-NLS-1$
        String source = new String(fParseState.getSource());
        // locates the source
        ParseNode node = script.getNode();
        source = source.substring(node.getStartingOffset(), node.getEndingOffset()+1);
        // gets the first line of the source
        StringTokenizer st = new StringTokenizer(source, "\n\r\f"); //$NON-NLS-1$
        source = st.nextToken();
        if (source.length() <= TRIM_TO_LENGTH) {
            text.append(source);
        } else {
            text.append(source.substring(0, TRIM_TO_LENGTH - 1)).append("..."); //$NON-NLS-1$
        }
        text.append(" ").append(script.getEndTag()); //$NON-NLS-1$
        return text.toString();
    }
}
