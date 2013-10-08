/******************************************************************************
* Copyright (C) 2013  André Berg
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     André Berg <andre.bergmedia@googlemail.com> - initial API and implementation
******************************************************************************/
/**
 * 
 */
package org.python.pydev.editor.saveactions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * Implements validation on updating the name of the date field.
 * 
 * @author André Berg
 * @version 0.1
 */
public class PydevDateFieldNameEditor extends StringFieldEditor {
        
    public PydevDateFieldNameEditor(String name, String labelText, int width, Composite parent) {
        super(name, labelText, width, parent);
    }
    
    @Override
    public boolean isValid() {
        final String curVal = this.getStringValue();
        boolean valid = true;
        final Pattern pattern = Pattern.compile("^__([^_]+?)__$");
        final Matcher matcher = pattern.matcher(curVal);
        if (!matcher.matches()) {
            valid = false;
            this.setErrorMessage("Field name must be enclosed in double underscore.");
            this.showErrorMessage();
        } else {
            this.setErrorMessage(null);
        }
        return valid;
    }

}
