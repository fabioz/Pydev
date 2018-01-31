package org.python.pydev.plugin.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

public class PyDevEditorPreferences {

    //   Preferences
    //To add a new preference it needs to be included in
    //createAppearancePage
    //createOverlayStore
    //initializeDefaultPreferences
    //declaration of fAppearanceColorListModel if it is a color
    //constants (here)
    public static final int TOOLTIP_WIDTH = 80;

    /*
     * If you just want to add some option, you will need to:
     * - create fields for it, as seen here
     * - add to overlay store in createOverlayStore()
     * - add what appears in the Preferences page at createAppearancePage()
     * - add the function to the org.python.pydev.editor.autoedit.IIndentPrefs interface
     * - probably add that function to org.python.pydev.editor.autoedit.DefaultIndentPrefs
     *
     */

    /**
     * Edition of translation paths.
     */
    public static final String SOURCE_LOCATION_PATHS = "SOURCE_LOCATION_PATHS";

    public static final String USE_VERTICAL_INDENT_GUIDE = "USE_VERTICAL_INDENT_GUIDE";
    public static final boolean DEFAULT_USE_VERTICAL_INDENT_GUIDE = true;

    public static final String USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND = "USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND";
    public static final boolean DEFAULT_USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND = true;

    public static final String VERTICAL_INDENT_COLOR = "VERTICAL_INDENT_COLOR";
    public static final RGB DEFAULT_VERTICAL_INDENT_COLOR = new RGB(125, 125, 125);

    public static final String VERTICAL_INDENT_TRANSPARENCY = "VERTICAL_INDENT_TRANSPARENCY";
    public static final int DEFAULT_VERTICAL_INDENT_TRANSPARENCY = 100;

    public static final boolean DEFAULT_EDITOR_USE_CUSTOM_CARETS = false;
    public static final boolean DEFAULT_EDITOR_WIDE_CARET = false;

    //matching
    public static final String USE_MATCHING_BRACKETS = "USE_MATCHING_BRACKETS";
    public static final boolean DEFAULT_USE_MATCHING_BRACKETS = true;

    public static final String MATCHING_BRACKETS_COLOR = "EDITOR_MATCHING_BRACKETS_COLOR";
    public static final RGB DEFAULT_MATCHING_BRACKETS_COLOR = new RGB(64, 128, 128);

    public static final String MATCHING_BRACKETS_STYLE = "EDITOR_MATCHING_BRACKETS_STYLE";
    public static final int DEFAULT_MATCHING_BRACKETS_STYLE = SWT.NORMAL;

    //colors
    public static final String DECORATOR_COLOR = "DECORATOR_COLOR";
    public static final RGB DEFAULT_DECORATOR_COLOR = new RGB(125, 125, 125);

    public static final String NUMBER_COLOR = "NUMBER_COLOR";
    public static final RGB DEFAULT_NUMBER_COLOR = new RGB(128, 0, 0);

    public static final String CODE_COLOR = "CODE_COLOR";
    public static final RGB DEFAULT_CODE_COLOR = new RGB(0, 0, 0);

    public static final String KEYWORD_COLOR = "KEYWORD_COLOR";
    public static final RGB DEFAULT_KEYWORD_COLOR = new RGB(0, 0, 255);

    public static final String SELF_COLOR = "SELF_COLOR";
    public static final RGB DEFAULT_SELF_COLOR = new RGB(0, 0, 0);

    public static final String STRING_COLOR = "STRING_COLOR";
    public static final RGB DEFAULT_STRING_COLOR = new RGB(201, 128, 43);

    public static final String UNICODE_COLOR = "UNICODE_COLOR";
    public static final RGB DEFAULT_UNICODE_COLOR = new RGB(0, 170, 0);

    public static final String COMMENT_COLOR = "COMMENT_COLOR";
    public static final RGB DEFAULT_COMMENT_COLOR = new RGB(192, 192, 192);

    public static final String BACKQUOTES_COLOR = "BACKQUOTES_COLOR";
    public static final RGB DEFAULT_BACKQUOTES_COLOR = new RGB(0, 0, 0);

    public static final String CLASS_NAME_COLOR = "CLASS_NAME_COLOR";
    public static final RGB DEFAULT_CLASS_NAME_COLOR = new RGB(0, 0, 0);

    public static final String FUNC_NAME_COLOR = "FUNC_NAME_COLOR";
    public static final RGB DEFAULT_FUNC_NAME_COLOR = new RGB(0, 0, 0);

    public static final String PARENS_COLOR = "PARENS_COLOR";
    public static final RGB DEFAULT_PARENS_COLOR = new RGB(0, 0, 0);

    public static final String OPERATORS_COLOR = "OPERATORS_COLOR";
    public static final RGB DEFAULT_OPERATORS_COLOR = new RGB(0, 0, 0);

    public static final String DOCSTRING_MARKUP_COLOR = "DOCSTRING_MARKUP_COLOR";
    public static final RGB DEFAULT_DOCSTRING_MARKUP_COLOR = new RGB(0, 170, 0);

    //see initializeDefaultColors for selection defaults
    public static final String CONNECT_TIMEOUT = "CONNECT_TIMEOUT";
    public static final int DEFAULT_CONNECT_TIMEOUT = 20000;

    public static final String RELOAD_MODULE_ON_CHANGE = "RELOAD_MODULE_ON_CHANGE";
    public static final boolean DEFAULT_RELOAD_MODULE_ON_CHANGE = true;

    public static final String DONT_TRACE_ENABLED = "DONT_TRACE_ENABLED";
    public static final boolean DEFAULT_DONT_TRACE_ENABLED = true;

    public static final String SHOW_RETURN_VALUES = "SHOW_RETURN_VALUES";
    public static final boolean DEFAULT_SHOW_RETURN_VALUES = true;

    public static final String TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS = "TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS";
    public static final boolean DEFAULT_TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS = false;

    public static final String DEBUG_MULTIPROCESSING_ENABLED = "DEBUG_MULTIPROCESSING_ENABLED";
    public static final boolean DEFAULT_DEBUG_MULTIPROCESSING_ENABLED = true;

    public static final String KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS = "KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS";
    public static final boolean DEFAULT_KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS = true;

    public static final String QT_THREADS_DEBUG_MODE = "QT_THREADS_DEBUG_MODE";
    public static final String DEFAULT_QT_THREADS_DEBUG_MODE = "none";

    public static final String[][] ENTRIES_VALUES_QT_THREADS_DEBUG_MODE = new String[][] {
            { "No QThread debugging", "none" },
            { "Auto-discover Qt version (may fail if multiple Qt versions are installed)", "auto" },
            { "PyQt5", "pyqt5" },
            { "PyQt4", "pyqt4" },
            { "PySide", "pyside" },
    };

    public static final String MAKE_LAUNCHES_WITH_M_FLAG = "MAKE_LAUNCHES_WITH_M_FLAG";
    public static final boolean DEFAULT_MAKE_LAUNCHES_WITH_M_FLAG = false;

    public static final String GEVENT_DEBUGGING = "GEVENT_DEBUGGING";
    public static final boolean DEFAULT_GEVENT_DEBUGGING = false;

    //font
    public static final String DECORATOR_STYLE = "DECORATOR_STYLE";
    public static final int DEFAULT_DECORATOR_STYLE = SWT.ITALIC;

    public static final String NUMBER_STYLE = "NUMBER_STYLE";
    public static final int DEFAULT_NUMBER_STYLE = SWT.NORMAL;

    public static final String CODE_STYLE = "CODE_STYLE";
    public static final int DEFAULT_CODE_STYLE = SWT.NORMAL;

    public static final String KEYWORD_STYLE = "KEYWORD_STYLE";
    public static final int DEFAULT_KEYWORD_STYLE = SWT.NORMAL;

    public static final String SELF_STYLE = "SELF_STYLE";
    public static final int DEFAULT_SELF_STYLE = SWT.ITALIC;

    public static final String STRING_STYLE = "STRING_STYLE";
    public static final int DEFAULT_STRING_STYLE = SWT.ITALIC;

    public static final String UNICODE_STYLE = "UNICODE_STYLE";
    public static final int DEFAULT_UNICODE_STYLE = SWT.ITALIC;

    public static final String COMMENT_STYLE = "COMMENT_STYLE";
    public static final int DEFAULT_COMMENT_STYLE = SWT.NORMAL;

    public static final String BACKQUOTES_STYLE = "BACKQUOTES_STYLE";
    public static final int DEFAULT_BACKQUOTES_STYLE = SWT.BOLD;

    public static final String CLASS_NAME_STYLE = "CLASS_NAME_STYLE";
    public static final int DEFAULT_CLASS_NAME_STYLE = SWT.BOLD;

    public static final String FUNC_NAME_STYLE = "FUNC_NAME_STYLE";
    public static final int DEFAULT_FUNC_NAME_STYLE = SWT.BOLD;

    public static final String PARENS_STYLE = "PARENS_STYLE";
    public static final int DEFAULT_PARENS_STYLE = SWT.NORMAL;

    public static final String OPERATORS_STYLE = "OPERATORS_STYLE";
    public static final int DEFAULT_OPERATORS_STYLE = SWT.NORMAL;

    public static final String DOCSTRING_MARKUP_STYLE = "DOCSTRING_MARKUP_STYLE";
    public static final int DEFAULT_DOCSTRING_MARKUP_STYLE = SWT.BOLD;

}
