/*
 * Created on Feb 16, 2006
 */
package org.python.pydev.editor.autoedit;

import org.eclipse.jface.text.DocumentCommand;

public class DocCmd extends DocumentCommand{
    public DocCmd(int offset, int length, String text){
        this.offset = offset;
        this.length = length;
        this.text   = text;
    }

}