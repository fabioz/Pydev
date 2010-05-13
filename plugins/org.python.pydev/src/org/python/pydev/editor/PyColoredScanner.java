/*
 * @author: Scott Schlesier
 * Created: March 5, 2005
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.ui.ColorAndStyleCache;

/**
 * 
 * PyColoredScanner is a simple modification to RuleBasedScanner
 * that supports updating the defaultToken color based on a named
 * color in the colorCache
 */
public class PyColoredScanner extends RuleBasedScanner {
    private ColorAndStyleCache colorCache;
	private String name;

    
    public PyColoredScanner(ColorAndStyleCache colorCache, String name) {
        super();
        Assert.isNotNull(colorCache);
        this.colorCache = colorCache;
        this.name = name;
        updateColorAndStyle();        
    }
    
    public void updateColorAndStyle() {
    	TextAttribute attr;
    	if(PydevEditorPrefs.COMMENT_COLOR.equals(name)){
    		attr = colorCache.getCommentTextAttribute();
    		
    	}else if(PydevEditorPrefs.BACKQUOTES_COLOR.equals(name)){
    		attr = colorCache.getBackquotesTextAttribute();
    		
    	}else if(PydevEditorPrefs.STRING_COLOR.equals(name)){
    		attr = colorCache.getStringTextAttribute();
    		
    	}else{
    		throw new RuntimeException("Unexpected: "+name);
    	}
        setDefaultReturnToken(new Token(attr));
    }
    
    
}
