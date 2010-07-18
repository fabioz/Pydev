package org.python.pydev.red_core;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.graphics.RGB;
import org.python.pydev.plugin.preferences.IPydevPreferencesProvider;

import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.theme.IThemeManager;
import com.aptana.theme.Theme;
import com.aptana.theme.ThemePlugin;

/**
 * Adds the colors of the Aptana theming to the pydev syntax tokens.
 */
public class AddRedCorePreferences implements IPydevPreferencesProvider{

	
	public IPreferenceStore[] getPreferenceStore() {
		if(!AddRedCoreThemeAvailable.isRedCoreAvailable()){
			return null;
		}
		return new IPreferenceStore[]{
		        ThemePlugin.getDefault().getPreferenceStore(),
		        CommonEditorPlugin.getDefault().getPreferenceStore(),
		};
	}
	
	public boolean isColorOrStyleProperty(String property) {
		if(!AddRedCoreThemeAvailable.isRedCoreAvailable()){
			return false;
		}
        if(property.equals(IThemeManager.THEME_CHANGED)){
        	return true;
        }
        return false;
	}

	private TextAttribute getFromTheme(String name) {
		if(!AddRedCoreThemeAvailable.isRedCoreAvailable()){
			return null;
		}
		Theme currentTheme = ThemePlugin.getDefault().getThemeManager().getCurrentTheme();
		return currentTheme.getTextAttribute(name);
	}
	
	public TextAttribute getKeywordTextAttribute() {
		return getFromTheme("keyword.py");
	}
	
	public TextAttribute getSelfTextAttribute() {
		return getFromTheme("keyword.other.self.py");
	}

	public TextAttribute getCodeTextAttribute() {
		return getFromTheme("source.py");
	}

	public TextAttribute getDecoratorTextAttribute() {
		return getFromTheme("storage.type.annotation.py");
	}

	public TextAttribute getNumberTextAttribute() {
		
		return getFromTheme("constant.numeric.py");
	}

	public TextAttribute getClassNameTextAttribute() {
		
		return getFromTheme("entity.name.class.py");
	}

	public TextAttribute getFuncNameTextAttribute() {
		
		return getFromTheme("entity.name.function.py");
	}

	public TextAttribute getCommentTextAttribute() {
		
		return getFromTheme("comment.py");
	}

	public TextAttribute getBackquotesTextAttribute() {
		
		return getFromTheme("support.type.py");
	}

	public TextAttribute getStringTextAttribute() {
		
		return getFromTheme("string.py");
	}

	public TextAttribute getConsoleErrorTextAttribute() {
		return getFromTheme("console.error.py");
	}

	public TextAttribute getConsoleOutputTextAttribute() {
		return getFromTheme("console.output.py");
	}

	public TextAttribute getConsoleInputTextAttribute() {
		return getFromTheme("console.input.py");
	}

	public TextAttribute getConsolePromptTextAttribute() {
		return getFromTheme("console.prompt.py");
	}

	public RGB getConsoleBackgroundRGB() {
	    if(!AddRedCoreThemeAvailable.isRedCoreAvailable()){
	        return null;
	    }
		Theme currentTheme = ThemePlugin.getDefault().getThemeManager().getCurrentTheme();
		return currentTheme.getBackground();
	}


}
