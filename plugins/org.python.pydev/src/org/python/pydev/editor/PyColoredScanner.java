/*
 * @author: Scott Schlesier
 * Created: March 5, 2005
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.ui.ColorCache;

/**
 * 
 * PyColoredScanner is a simple modification to RuleBasedScanner
 * that supports updating the defaultToken color based on a named
 * color in the colorCache
 */
public class PyColoredScanner extends RuleBasedScanner {
    private ColorCache colorCache;
    private String colorName;
    private int style;
    
    public PyColoredScanner(ColorCache colorCache, String colorName, int style) {
        super();
        this.colorCache = colorCache;
        this.colorName = colorName;
        this.style = style;
        updateColorAndStyle();        
    }
    
    public void setStyle(int style){
        this.style = style;
    }
    
    public void updateColorAndStyle() {
        setDefaultReturnToken(new Token(new TextAttribute(colorCache.getNamedColor(colorName), null, style)));
    }
    
    
}
