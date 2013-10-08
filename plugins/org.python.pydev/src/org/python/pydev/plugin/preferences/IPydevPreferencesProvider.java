/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.graphics.RGB;

public interface IPydevPreferencesProvider {

    IPreferenceStore[] getPreferenceStore();

    TextAttribute getSelfTextAttribute();

    TextAttribute getCodeTextAttribute();

    TextAttribute getDecoratorTextAttribute();

    TextAttribute getNumberTextAttribute();

    TextAttribute getClassNameTextAttribute();

    TextAttribute getFuncNameTextAttribute();

    TextAttribute getCommentTextAttribute();

    TextAttribute getBackquotesTextAttribute();

    TextAttribute getStringTextAttribute();

    TextAttribute getKeywordTextAttribute();

    boolean isColorOrStyleProperty(String property);

    TextAttribute getConsoleErrorTextAttribute();

    TextAttribute getConsoleOutputTextAttribute();

    TextAttribute getConsoleInputTextAttribute();

    TextAttribute getConsolePromptTextAttribute();

    TextAttribute getHyperlinkTextAttribute();

    RGB getConsoleBackgroundRGB();

    TextAttribute getParensTextAttribute();

    TextAttribute getOperatorsTextAttribute();

    TextAttribute getDocstringMarkupTextAttribute();

}
