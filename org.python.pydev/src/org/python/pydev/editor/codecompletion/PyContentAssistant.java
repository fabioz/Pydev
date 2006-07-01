/*
 * Created on Aug 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.contentassist.ContentAssistant;

/**
 * @author Fabio Zadrozny
 */
public class PyContentAssistant extends ContentAssistant{

    public PyContentAssistant(){
        this.enableAutoInsert(true);
    }

}
