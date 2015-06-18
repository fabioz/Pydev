/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Jul 29, 2003
 */
package org.python.pydev.shared_ui;

/**
 * Miscellaneous UI strings
 */
public class UIConstants {
    public static final String CREATE_CLASS_ICON = "icons/new/create_class_obj.png";
    public static final String CREATE_METHOD_ICON = "icons/new/create_method_obj.png";
    public static final String CREATE_MODULE_ICON = "icons/new/create_python_module.png";

    // Outline view
    public static final String CLASS_ICON = "icons/new/class_obj.gif";
    public static final String METHOD_ICON = "icons/new/method_obj.gif";

    public static final String IMPORT_ICON = "icons/new/imp_obj.gif";

    public static final String PRIVATE_FIELD_ICON = "icons/field_private_obj.gif";
    public static final String PROTECTED_FIELD_ICON = "icons/field_protected_obj.gif";
    public static final String PUBLIC_ATTR_ICON = "icons/new/attrpub_obj.gif";
    public static final String XML_TAG_ICON = "icons/new/xml_tag.png";
    public static final String ACTION_ICON = "icons/new/action.png";

    public static final String MAIN_FUNCTION_ICON = "icons/mainfunction.gif";
    public static final String COMMENT = "icons/python_comment.png";

    //Actions in outline
    public static final String COMMENT_BLACK = "icons/python_comment_black.png";
    public static final String MAGIC_OBJECT_ICON = "icons/magic_co.gif";
    public static final String STATIC_MEMBER_HIDE_ICON = "icons/static_co.gif";
    public static final String FIELDS_HIDE_ICON = "icons/fields_co.gif";

    //Decorations
    public static final String PRIVATE_ICON = "icons/private_obj.gif"; //__XXX
    public static final String PROTECTED_ICON = "icons/protected_obj.gif"; // _XXX
    public static final String CTX_INSENSITIVE_DECORATION_ICON = "icons/new/imp_dec.gif";

    public static final String DECORATION_CLASS = "icons/decoration_class_obj.png";
    public static final String DECORATION_STATIC = "icons/decoration_static_obj.png";

    // Actions
    public static final String SYNC_WITH_EDITOR = "icons/sync_ed.gif";
    public static final String ALPHA_SORT = "icons/alphab_sort_co.gif";
    public static final String COLLAPSE_ALL = "icons/collapseall.gif";
    public static final String EXPAND_ALL = "icons/expand.gif";

    //file content
    public static final String PY_FILE_ICON = "icons/python_file.gif";
    public static final String CYTHON_FILE_ICON = "icons/cython_file.png";
    public static final String CYTHON_ICON = "icons/cython.png";

    public static final String PY_FILE_CUSTOM_ICON = "icons/custom_python_file.png";
    public static final String FILE_ICON = "icons/file.gif";
    public static final String FOLDER_ICON = "icons/folder.gif";
    public static final String FOLDER_PACKAGE_ICON = "icons/package_obj.gif";
    public static final String CUSTOM_INIT_ICON = "icons/custom_init.png";
    public static final String COPY_ICON = "icons/copy.gif";
    public static final String CLOSE_ICON = "icons/close.gif";
    public static final String SOURCE_FOLDER_ICON = "icons/packagefolder_obj.gif";
    public static final String PROJECT_SOURCE_FOLDER_ICON = "icons/project_source_folder.gif";
    public static final String PROJECT_ICON = "icons/project.png";

    //completion
    public static final String BUILTINS_ICON = "icons/builtin_obj.gif";

    public static final String COMPLETION_PACKAGE_ICON = "icons/package_obj.gif";
    public static final String COMPLETION_TEMPLATE = "icons/template.gif";
    public static final String COMPLETION_IMPORT_ICON = IMPORT_ICON;
    public static final String COMPLETION_RELATIVE_IMPORT_ICON = "icons/new/imp_rel_obj.gif";
    public static final String COMPLETION_CLASS_ICON = CLASS_ICON;
    public static final String COMPLETION_PARAMETERS_ICON = "icons/parameters_obj.gif";
    public static final String COMPLETION_EPYDOC = "icons/annotation_obj.gif";
    public static final String COMPLETION_IPYTHON_MAGIC = "icons/mainfunction.gif";

    //content assist
    public static final String ASSIST_ANNOTATION = "icons/annotation_obj.gif";
    public static final String ASSIST_TRY_EXCEPT = COMPLETION_TEMPLATE;
    public static final String ASSIST_TRY_FINNALLY = COMPLETION_TEMPLATE;

    public static final String ASSIST_NEW_METHOD = "icons/add_correction.gif";
    public static final String ASSIST_NEW_GENERIC = "icons/add_correction.gif";
    public static final String ASSIST_NEW_CLASS = CLASS_ICON;

    public static final String ASSIST_MOVE_IMPORT = "icons/correction_move.gif";

    public static final String ASSIST_ASSIGN_TO_LOCAL = PUBLIC_ATTR_ICON;
    public static final String ASSIST_ASSIGN_TO_CLASS = PUBLIC_ATTR_ICON;

    public static final String ASSIST_DOCSTRING = COMPLETION_TEMPLATE;

    //libraries
    public static final String LIB_SYSTEM_ROOT = "icons/library_obj.gif";
    public static final String LIB_SYSTEM = "icons/jar_l_obj.gif";
    public static final String REMOVE_LIB_SYSTEM = "icons/jar_remove_l_obj.gif";
    public static final String LIB_FORCED_BUILTIN = "icons/jar_obj.gif";

    //general
    public static final String HISTORY = "icons/history_list.gif";
    public static final String ERROR = "icons/showerr_tsk.gif";
    public static final String ERROR_SMALL = "icons/error_small.gif";
    public static final String PY_INTERPRETER_ICON = "icons/python_16x16.png";
    public static final String VARIABLE_ICON = "icons/build_var_obj.gif";
    public static final String ENVIRONMENT_ICON = "icons/environment_obj.gif";
    public static final String COPY_QUALIFIED_NAME = "icons/cpyqual_menu.gif";
    public static final String SEARCH = "icons/search.gif";
    public static final String SEARCH_DOCS = "icons/search_docs.gif";
    public static final String RELAUNCH = "icons/relaunch.png";
    public static final String RELAUNCH1 = "icons/relaunch1.png";
    public static final String RELAUNCH_ERRORS = "icons/relaunch_errors.png";
    public static final String TERMINATE_ALL = "icons/terminate_all.gif";
    public static final String TERMINATE = "icons/terminate.gif";
    public static final String SHOW_ONLY_ERRORS = "icons/failures.gif";
    public static final String WORKING_SET = "icons/workset.gif";

    public static final String REMOVE = "icons/remove.gif";
    public static final String REMOVE_ALL = "icons/remove_all.gif"; //note: only in SharedUI plugin.

    //browser
    public static final String STOP = "icons/showerr_tsk.gif";
    public static final String REFRESH = "icons/refresh_nav.gif";
    public static final String FORWARD = "icons/forward_nav.gif";
    public static final String BACK = "icons/backward_nav.gif";
    public static final String HOME = "icons/home_nav.gif";

    public static final String CONSOLE_ENABLED = "icons/console_enabled.png";
    public static final String CONSOLE_DISABLED = "icons/console_disabled.png";

    public static final String FORCE_TABS_ACTIVE = "icons/tabs_active.png";
    public static final String FORCE_TABS_INACTIVE = "icons/tabs_inactive.png";
    public static final String PY_LINT_ICON = "icons/pylint.png";
    public static final String WARNING = "icons/warning.png";
    public static final String ERROR_DECORATION = "icons/error_decoration.gif";
    public static final String WARNING_DECORATION = "icons/warning_decoration.gif";

    // search
    public static final String LINE_MATCH = "icons/line_match.gif";
}
