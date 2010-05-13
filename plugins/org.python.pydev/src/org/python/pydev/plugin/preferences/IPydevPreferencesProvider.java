package org.python.pydev.plugin.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;

public interface IPydevPreferencesProvider {

	IPreferenceStore getPreferenceStore();

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


}
