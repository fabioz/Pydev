package org.python.pydev.shared_ui.search;

/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import org.eclipse.osgi.util.NLS;

public final class SearchMessages extends NLS {

    private static final String BUNDLE_NAME = "org.python.pydev.shared_ui.search.SearchMessages";//$NON-NLS-1$

    private SearchMessages() {
        // Do not instantiate
    }

    public static String SearchPage_wholeWord;
    public static String FileSearchPage_open_file_dialog_title;
    public static String FileSearchPage_open_file_failed;
    public static String FileTextSearchScope_scope_empty;
    public static String FileTextSearchScope_scope_single;
    public static String FileTextSearchScope_scope_double;
    public static String FileTextSearchScope_scope_multiple;

    public static String FileTextSearchScope_ws_scope_empty;
    public static String FileTextSearchScope_ws_scope_single;
    public static String FileTextSearchScope_ws_scope_double;
    public static String FileTextSearchScope_ws_scope_multiple;
    public static String ReplaceAction_description_operation;
    public static String ReplaceAction_title_all;
    public static String ReplaceAction_title_selected;
    public static String ReplaceConfigurationPage_description_many_in_many;
    public static String ReplaceConfigurationPage_description_many_in_one;
    public static String ReplaceConfigurationPage_description_one_in_one;
    public static String ReplaceConfigurationPage_isRegex_label;
    public static String ReplaceConfigurationPage_replace_label;
    public static String ReplaceConfigurationPage_with_label;
    public static String ReplaceRefactoring_composite_change_name;
    public static String ReplaceRefactoring_error_access_file;
    public static String ReplaceRefactoring_error_accessing_file_buffer;
    public static String ReplaceRefactoring_error_illegal_search_string;
    public static String ReplaceRefactoring_error_match_content_changed;
    public static String ReplaceRefactoring_error_no_changes;
    public static String ReplaceRefactoring_error_no_matches;
    public static String ReplaceRefactoring_error_no_replace_string;
    public static String ReplaceRefactoring_error_replacement_expression;
    public static String ReplaceRefactoring_group_label_change_for_file;
    public static String ReplaceRefactoring_group_label_match_replace;
    public static String ReplaceRefactoring_refactoring_name;
    public static String ReplaceRefactoring_result_update_name;

    public static String PatternConstructor_error_escape_sequence;
    public static String PatternConstructor_error_hex_escape_sequence;
    public static String PatternConstructor_error_line_delim_position;
    public static String PatternConstructor_error_unicode_escape_sequence;
    public static String FileLabelProvider_line_number;
    public static String FileSearchPage_limited_format_files;
    public static String FileSearchPage_limited_format_matches;

    public static String SearchDialog_title;
    public static String SearchDialog_searchAction;
    public static String SearchDialog_replaceAction;
    public static String SearchDialog_customize;
    public static String SearchDialog_noSearchExtension;
    public static String SearchPageSelectionDialog_title;
    public static String SearchPageSelectionDialog_message;
    public static String SearchManager_resourceChangedWarning;
    public static String SearchManager_resourceChanged;
    public static String SearchManager_resourceDeleted;
    public static String SearchManager_updating;
    public static String SearchResultView_title;
    public static String SearchResultView_titleWithDescription;
    public static String SearchResultView_matches;
    public static String SearchResultView_removed_resource;
    public static String SearchResultView_removeAllResults_text;
    public static String SearchResultView_removeAllResults_tooltip;
    public static String SearchResultView_removeAllSearches_text;
    public static String SearchResultView_removeAllSearches_tooltip;
    public static String SearchResultView_searchAgain_text;
    public static String SearchResultView_searchAgain_tooltip;
    public static String SearchResultView_previousSearches_text;
    public static String SearchResultView_previousSearches_tooltip;
    public static String SearchResultView_removeEntry_text;
    public static String SearchResultView_removeEntry_tooltip;
    public static String SearchResultView_removeEntries_text;
    public static String SearchResultView_removeEntries_tooltip;
    public static String SearchResultView_removeMatch_text;
    public static String SearchResultView_removeMatch_tooltip;
    public static String SearchResultView_gotoMarker_text;
    public static String SearchResultView_gotoMarker_tooltip;
    public static String SearchResultView_showNext_text;
    public static String SearchResultView_showNext_tooltip;
    public static String SearchResultView_showPrev_text;
    public static String SearchResultView_showPrev_tooltip;
    public static String SearchDialogClosingDialog_title;
    public static String SearchDialogClosingDialog_message;
    public static String SearchDialog_error_pageCreationFailed;
    public static String SearchPlugin_internal_error;
    public static String Search_Error_search_title;
    public static String Search_Error_search_message;
    public static String Search_Error_setDescription_title;
    public static String Search_Error_setDescription_message;
    public static String Search_Error_openResultView_title;
    public static String Search_Error_openResultView_message;
    public static String Search_Error_deleteMarkers_title;
    public static String Search_Error_deleteMarkers_message;
    public static String Search_Error_createMarker_title;
    public static String Search_Error_createMarker_message;
    public static String Search_Error_markerAttributeAccess_title;
    public static String Search_Error_markerAttributeAccess_message;
    public static String Search_Error_switchSearch_title;
    public static String Search_Error_switchSearch_message;
    public static String Search_Error_createSearchPage_title;
    public static String Search_Error_createSearchPage_message;
    public static String Search_Error_createSorter_title;
    public static String Search_Error_createSorter_message;
    public static String SearchPage_containingText_text;
    public static String SearchPage_containingText_hint;
    public static String SearchPage_browse;
    public static String SearchPage_fileNamePatterns_text;
    public static String SearchPage_fileNamePatterns_hint;
    public static String SearchPage_caseSensitive;
    public static String SearchPage_regularExpression;
    public static String TextSearchEngine_statusMessage;
    public static String TextSearchPage_replace_querycreationproblem_message;
    public static String TextSearchPage_replace_runproblem_message;
    public static String TextSearchPage_searchDerived_label;
    public static String TextSearchVisitor_filesearch_task_label;
    public static String TextSearchVisitor_patterntoocomplex0;
    public static String TextSearchVisitor_progress_updating_job;
    public static String TextSearchVisitor_scanning;
    public static String TextSearchVisitor_error;
    public static String TextSearchVisitor_canceled;
    public static String TextSearchVisitor_textsearch_task_label;
    public static String TextSearchVisitor_unsupportedcharset;
    public static String TextSearchVisitor_illegalcharset;
    public static String SortDropDownAction_label;
    public static String SortDropDownAction_tooltip;
    public static String ShowOtherSearchesAction_label;
    public static String ShowOtherSearchesAction_tooltip;
    public static String OtherSearchesDialog_title;
    public static String OtherSearchesDialog_message;
    public static String PreviousSearchesDialog_title;
    public static String PreviousSearchesDialog_message;
    public static String TextSearchPage_replace_searchproblems_title;
    public static String TextSearchPage_replace_searchproblems_message;
    public static String FileSearchQuery_label;
    public static String FileSearchQuery_pluralPattern;
    public static String FileSearchQuery_singularLabel;
    public static String FileSearchQuery_singularLabel_fileNameSearch;
    public static String FileSearchQuery_pluralPattern_fileNameSearch;
    public static String OpenSearchDialogAction_label;
    public static String OpenSearchDialogAction_tooltip;
    public static String FileTypeEditor_typeDelimiter;
    public static String FileLabelProvider_dashSeparated;
    public static String FileLabelProvider_count_format;
    public static String FileLabelProvider_removed_resource_label;
    public static String FileSearchPage_sort_name_label;
    public static String FileSearchPage_sort_path_label;
    public static String FileSearchPage_error_marker;
    public static String FileSearchPage_sort_by_label;
    public static String WorkspaceScope;
    public static String ScopePart_group_text;
    public static String ScopePart_selectedResourcesScope_text;
    public static String ScopePart_enclosingProjectsScope_text;
    public static String ScopePart_workingSetChooseButton_text;
    public static String ScopePart_workingSetText_accessible_label;
    public static String ScopePart_workingSetScope_text;
    public static String ScopePart_workspaceScope_text;
    public static String ScopePart_workingSetConcatenation;
    public static String CopyToClipboardAction_label;
    public static String CopyToClipboardAction_tooltip;
    public static String CopyToClipboardAction_error_title;
    public static String CopyToClipboardAction_error_message;
    public static String ExceptionDialog_seeErrorLogMessage;
    public static String SearchPreferencePage_emphasizePotentialMatches;
    public static String SearchPreferencePage_potentialMatchFgColor;
    public static String SearchPreferencePage_reuseEditor;
    public static String SearchPreferencePage_bringToFront;
    public static String SearchPreferencePage_defaultPerspective;
    public static String SearchPreferencePage_defaultPerspective_none;
    public static String SearchPreferencePage_ignorePotentialMatches;
    public static String ReplaceAction_label_all;
    public static String ReplaceAction_label_selected;
    public static String ReplaceAction2_error_validate_title;
    public static String ReplaceAction2_error_validate_message;
    public static String ReplaceDialog_replace_label;
    public static String ReplaceDialog_with_label;
    public static String ReplaceDialog_replace;
    public static String ReplaceDialog_replaceAllInFile;
    public static String ReplaceDialog_replaceAll;
    public static String ReplaceDialog_skip;
    public static String ReplaceDialog2_regexError_format;
    public static String ReplaceDialog_skipFile;
    public static String ReplaceDialog_dialog_title;
    public static String ReplaceDialog_error_unable_to_open_text_editor;
    public static String ReplaceDialog_error_unable_to_replace;
    public static String SelectAllAction_label;
    public static String SelectAllAction_tooltip;
    public static String RemovePotentialMatchesAction_removePotentialMatch_text;
    public static String RemovePotentialMatchesAction_removePotentialMatch_tooltip;
    public static String RemovePotentialMatchesAction_removePotentialMatches_text;
    public static String RemovePotentialMatchesAction_removePotentialMatches_tooltip;
    public static String RemovePotentialMatchesAction_dialog_title;
    public static String RemovePotentialMatchesAction_dialog_message;
    public static String OpenWithMenu_label;
    public static String ReadOnlyDialog_skipFile;
    public static String ReadOnlyDialog_skipAll;
    public static String ReadOnlyDialog_message;
    public static String ReplaceDialog_task_replace;
    public static String ReplaceDialog_task_replaceInFile;
    public static String ReplaceDialog_task_replace_replaceAll;
    public static String ReplaceDialog2_error_disableAutobuild;
    public static String ReplaceDialog2_error_restoreAutobuild;
    public static String ReplaceAction_label;
    public static String ReplaceAction_research_error;
    public static String ReplaceAction2_statusMessage;
    public static String SearchAgainConfirmationDialog_outofsync_message;
    public static String SearchAgainConfirmationDialog_outofsync_label;
    public static String SearchAgainConfirmationDialog_stale_message;
    public static String SearchAgainConfirmationDialog_stale_label;
    public static String SearchAgainConfirmationDialog_title;
    public static String ReplaceDialog_isRegex_label;

    static {
        NLS.initializeMessages(BUNDLE_NAME, SearchMessages.class);
    }

    public static String ReplaceDialog2_nomatches_error;
    public static String SearchPreferencePage_textSearchEngine;
    public static String TextSearchEngineRegistry_defaulttextsearch_label;
    public static String FileSearchQuery_singularPatternWithFileExt;
    public static String FileSearchQuery_pluralPatternWithFileExt;

    public static String AbstractTextSearchViewPage_update_job_name;
    public static String MatchFilterSelectionAction_label;
    public static String MatchFilterSelectionDialog_description_label;
    public static String MatchFilterSelectionDialog_error_invalid_limit;
    public static String MatchFilterSelectionDialog_filter_description;
    public static String MatchFilterSelectionDialog_label;
    public static String MatchFilterSelectionDialog_limit_description;
    public static String OpenSearchPreferencesAction_label;
    public static String OpenSearchPreferencesAction_tooltip;
    public static String RemoveSelectedMatchesAction_label;
    public static String RemoveSelectedMatchesAction_tooltip;
    public static String SearchAgainAction_label;
    public static String SearchAgainAction_tooltip;
    public static String SearchAgainAction_Error_title;
    public static String SearchAgainAction_Error_message;
    public static String SearchDropDownAction_label;
    public static String SearchDropDownAction_tooltip;
    public static String SearchesDialog_remove_label;
    public static String SearchDropDownAction_running_message;
    public static String SearchHistorySelectionDialog_configure_link_label;
    public static String SearchHistorySelectionDialog_history_size_description;
    public static String SearchHistorySelectionDialog_history_size_error;
    public static String SearchHistorySelectionDialog_history_size_title;
    public static String SearchHistorySelectionDialog_open_in_new_button;
    public static String SearchHistorySelectionDialog_restore_default_button;
    public static String SearchView_empty_search_label;
    public static String ShowSearchesAction_label;
    public static String ShowSearchesAction_tooltip;
    public static String SearchView_showIn_menu;
    public static String SearchesDialog_title;
    public static String SearchesDialog_message;
    public static String RemoveAllSearchesAction_label;
    public static String RemoveAllSearchesAction_tooltip;
    public static String RemoveAllMatchesAction_label;
    public static String RemoveAllMatchesAction_tooltip;
    public static String ShowNextResultAction_label;
    public static String ShowNextResultAction_tooltip;
    public static String ShowPreviousResultAction_label;
    public static String ShowPreviousResultAction_tooltip;
    public static String RemoveMatchAction_label;
    public static String RemoveMatchAction_tooltip;
    public static String DefaultSearchViewPage_show_match;
    public static String DefaultSearchViewPage_error_no_editor;
    public static String AbstractTextSearchViewPage_flat_layout_label;
    public static String AbstractTextSearchViewPage_flat_layout_tooltip;
    public static String AbstractTextSearchViewPage_hierarchical_layout_label;
    public static String AbstractTextSearchViewPage_hierarchical_layout_tooltip;
    public static String CancelSearchAction_label;
    public static String CancelSearchAction_tooltip;
    public static String AbstractTextSearchViewPage_searching_label;
    public static String CollapseAllAction_0;
    public static String CollapseAllAction_1;
    public static String ExpandAllAction_label;
    public static String ExpandAllAction_tooltip;
    public static String SearchView_error_noResultPage;
    public static String InternalSearchUI_error_unexpected;
    public static String NewSearchUI_error_title;
    public static String NewSearchUI_error_label;
    public static String AnnotationHighlighter_error_noDocument;
    public static String EditorAccessHighlighter_error_badLocation;
    public static String PinSearchViewAction_label;
    public static String PinSearchViewAction_tooltip;
    public static String SearchPageRegistry_error_creating_extensionpoint;
    public static String TextSearchGroup_submenu_text;
    public static String FindInWorkspaceActionDelegate_text;
    public static String FindInProjectActionDelegate_text;
    public static String FindInWorkingSetActionDelegate_text;
    public static String FindInFileActionDelegate_text;
    public static String TextSearchQueryProviderRegistry_defaultProviderLabel;
    public static String RetrieverAction_dialog_title;
    public static String RetrieverAction_empty_selection;
    public static String RetrieverAction_error_title;
    public static String RetrieverAction_error_message;

}
