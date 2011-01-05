package org.python.pydev.red_core;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.plugin.preferences.IPydevPreferencesProvider;
import org.python.pydev.plugin.preferences.IPydevPreferencesProvider2;
import org.python.pydev.utils.LabelFieldEditor;
import org.python.pydev.utils.LinkFieldEditor;

import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.theme.IThemeManager;
import com.aptana.theme.Theme;
import com.aptana.theme.ThemePlugin;

/**
 * Adds the colors of the Aptana theming to the pydev syntax tokens.
 */
public class AddRedCorePreferences implements IPydevPreferencesProvider, IPydevPreferencesProvider2{

	
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
	
    public TextAttribute getParensTextAttribute() {
        return getFromTheme("source.parens.py");
    }

    public TextAttribute getOperatorsTextAttribute() {
        return getFromTheme("source.operators.py");
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
	
	public TextAttribute getHyperlinkTextAttribute() {
	    return getFromTheme("hyperlink.py");
	}

	public RGB getConsoleBackgroundRGB() {
	    if(!AddRedCoreThemeAvailable.isRedCoreAvailable()){
	        return null;
	    }
		Theme currentTheme = ThemePlugin.getDefault().getThemeManager().getCurrentTheme();
		return currentTheme.getBackground();
	}

    public boolean createColorOptions(Composite appearanceComposite, final PydevEditorPrefs prefs) {
        if(!AddRedCoreThemeAvailable.isRedCoreAvailable()){
            return false;
        }
        LinkFieldEditor colorsAndFontsLinkFieldEditor = new LinkFieldEditor(
                "UNUSED", "Colors handled through <a>Aptana Themes</a>\n"
                		, appearanceComposite, new SelectionListener() {
                    
            public void widgetSelected(SelectionEvent e) {
                String id = "com.aptana.theme.preferencePage";
                IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) prefs.getContainer());
                workbenchPreferenceContainer.openPage(id, null);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        colorsAndFontsLinkFieldEditor.getLinkControl(appearanceComposite);
        
        LabelFieldEditor labelFieldEditor = new LabelFieldEditor("UNUSED", "Scopes used in Aptana Themes:\n\n" +
                "Code:          source                   " +  "Backquotes: support.type\n" +
                "Keywords:      keyword                  " +  "{}, [], (): source.parens\n" +
                "Self:          keyword.other.self       " +  "Comments:   comment\n" +
                "Decorators:    storage.type.annotation  " +  "Strings:    string\n" +
                "Numbers:       constant.numeric         " +  "Stderr:     console.error\n" +
                "Class name:    entity.name.class        " +  "Stdout:     console.output\n" +
                "Function name: entity.name.function     " +  "Input:      console.input\n" +
                "Operators:     source.operators         " +  "Prompt:     console.prompt\n" +
                "", appearanceComposite);
        Label labelControl = labelFieldEditor.getLabelControl(appearanceComposite);
        try {
            FontData labelFontData = new FontData("Courier New", 8, SWT.NONE);
            labelControl.setFont(new Font(labelControl.getDisplay(), labelFontData));
        } catch (Throwable e) {
            //ignore
        }
        
        prefs.setUpdateLabelExampleOnPrefsChanges();
        return true;
        
    }

}
