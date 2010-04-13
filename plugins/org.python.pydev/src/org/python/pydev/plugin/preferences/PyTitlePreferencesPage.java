package org.python.pydev.plugin.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;
import org.python.pydev.utils.TableComboFieldEditor;

public class PyTitlePreferencesPage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public static final String TITLE_EDITOR_NAMES_UNIQUE = "TITLE_EDITOR_NAMES_UNIQUE";
	public static final boolean DEFAULT_TITLE_EDITOR_NAMES_UNIQUE = true;
	
	public static final String TITLE_EDITOR_SHOW_EXTENSION = "TITLE_EDITOR_SHOW_EXTENSION";
	public static final boolean DEFAULT_TITLE_EDITOR_SHOW_EXTENSION = false;
	
	public static final String TITLE_EDITOR_CUSTOM_INIT_ICON = "TITLE_EDITOR_CUSTOM_INIT_ICON";
	public static final boolean DEFAULT_TITLE_EDITOR_CUSTOM_INIT_ICON = true;
	
	public static final String TITLE_EDITOR_INIT_HANDLING = "TITLE_EDITOR_INIT_HANDLING";
	public static final String TITLE_EDITOR_INIT_HANDLING_IN_TITLE = "TITLE_EDITOR_INIT_HANDLING_IN_TITLE";
	public static final String TITLE_EDITOR_INIT_HANDLING_PARENT_IN_TITLE = "TITLE_EDITOR_INIT_HANDLING_PARENT_IN_TITLE";
	public static final String DEFAULT_TITLE_EDITOR_INIT_HANDLING = TITLE_EDITOR_INIT_HANDLING_PARENT_IN_TITLE;

	
	public static boolean isTitlePreferencesProperty(String property) {
		return PyTitlePreferencesPage.TITLE_EDITOR_INIT_HANDLING.equals(property)||
			PyTitlePreferencesPage.TITLE_EDITOR_NAMES_UNIQUE.equals(property)||
			PyTitlePreferencesPage.TITLE_EDITOR_SHOW_EXTENSION.equals(property)||
			PyTitlePreferencesPage.TITLE_EDITOR_CUSTOM_INIT_ICON.equals(property);

	}

	public static boolean isTitlePreferencesIconRelatedProperty(String property) {
		return PyTitlePreferencesPage.TITLE_EDITOR_CUSTOM_INIT_ICON.equals(property);
	}
	
	private BooleanFieldEditor editorNamesUnique;
	private BooleanFieldEditor changeInitIcon;
	private TableComboFieldEditor initHandlingFieldEditor;
	private BooleanFieldEditor titleShowExtension;
//	private List<Image> allocatedImages = new ArrayList<Image>();
	
    public PyTitlePreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }


	public void init(IWorkbench workbench) {
	}

	protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        
        
        //Unique names?
        editorNamesUnique = new BooleanFieldEditor(
        		TITLE_EDITOR_NAMES_UNIQUE, "Editor name (considering icon) must be unique?", BooleanFieldEditor.SEPARATE_LABEL, p);
        addField(editorNamesUnique);
        
        titleShowExtension = new BooleanFieldEditor(
        		TITLE_EDITOR_SHOW_EXTENSION, "Show file extension on tab?", BooleanFieldEditor.SEPARATE_LABEL, p);
        addField(titleShowExtension);
        
        
        //__init__ handling
//        ImageCache imageCache = PydevPlugin.getImageCache();
//        Display display = Display.getCurrent();
//        Image imageCustom = createCustomImage(imageCache, display, "I"); 
//        allocatedImages.add(imageCustom);
        
        
        //Should pydev change the init icon?
        changeInitIcon = new BooleanFieldEditor(
        		TITLE_EDITOR_CUSTOM_INIT_ICON, "Use custom init icon?", BooleanFieldEditor.SEPARATE_LABEL, p);
        addField(changeInitIcon);
        
    	Object[][] EDITOR__INIT__HANDLING_VALUES = {
    		{
    			"__init__.py should appear in title", 
    			TITLE_EDITOR_INIT_HANDLING_IN_TITLE, 
    			null
    		},
    		
    		{
    			"Show parent name in title", 
    			TITLE_EDITOR_INIT_HANDLING_PARENT_IN_TITLE, 
    			null
    		},
    		
    	};
    	
    	
        initHandlingFieldEditor = new TableComboFieldEditor(
        		TITLE_EDITOR_INIT_HANDLING, "__init__.py handling:", EDITOR__INIT__HANDLING_VALUES, p);
        addField(initHandlingFieldEditor);
	}

//	private static Image createCustomImage(ImageCache imageCache, Display display, String text) {
//		return imageCache.get(UIConstants.CUSTOM_INIT_ICON);
//		Image imageCustom = new Image(display, imageCache.get(UIConstants.CUSTOM_FOLDER_PACKAGE_ICON), SWT.IMAGE_COPY);
//        GC gc = new GC(imageCustom);
//        Color color = new Color(display, 41, 192, 88);
//        gc.setForeground(color); 
//        Font font = new Font(display, new FontData("Courier New", 6, SWT.BOLD));
//        gc.setFont(font);
//        gc.drawText(text, 6, 3, false);
//        color.dispose();
//        font.dispose();
//        gc.dispose();
//		return imageCustom;
//	}
	
	public void dispose() {
		super.dispose();
//		for(Image image:allocatedImages){
//			image.dispose();
//		}
//		allocatedImages.clear();
	}

	public static boolean getEditorNamesUnique() {
		return PydevPrefs.getPreferences().getBoolean(TITLE_EDITOR_NAMES_UNIQUE);
	}

	
	/**
	 * @return A constant defined in this class
	 * EDITOR_TITLE_INIT_HANDLING_XXX
	 * 
	 * Note that clients using this methods can compare it with == with a constant in this
	 * class (as it will return the actual constant and not what's set in the preferences).
	 */
	public static String getInitHandling() {
		String initHandling = PydevPrefs.getPreferences().getString(TITLE_EDITOR_INIT_HANDLING);
		if(TITLE_EDITOR_INIT_HANDLING_IN_TITLE.equals(initHandling)){
			return TITLE_EDITOR_INIT_HANDLING_IN_TITLE;
		}
		return TITLE_EDITOR_INIT_HANDLING_PARENT_IN_TITLE; //default
	}
	
	
	public static boolean useCustomInitIcon() {
		return PydevPrefs.getPreferences().getBoolean(TITLE_EDITOR_CUSTOM_INIT_ICON);
	}
	
	public static boolean getTitleShowExtension() {
		return PydevPrefs.getPreferences().getBoolean(TITLE_EDITOR_SHOW_EXTENSION);
	}
	
	public static Image getInitIcon() {
		ImageCache imageCache = PydevPlugin.getImageCache();
		if(useCustomInitIcon()){
			return imageCache.get(UIConstants.CUSTOM_INIT_ICON);
		}else{
			return imageCache.get(UIConstants.PY_FILE_ICON); //default icon
		}
	}

}
