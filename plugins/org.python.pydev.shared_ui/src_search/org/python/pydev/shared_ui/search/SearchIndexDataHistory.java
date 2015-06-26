/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.search;

import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.python.pydev.shared_core.structure.OrderedSet;

public class SearchIndexDataHistory {

    private static final String PAGE_NAME = "SearchIndexPage";

    private static final String STORE_HISTORY = "HISTORY";

    private static final String STORE_HISTORY_SIZE = "HISTORY_SIZE";

    private static final int HISTORY_SIZE = 15;

    private OrderedSet<SearchIndexData> fPreviousSearchPatterns = new OrderedSet<>();

    /**
     * This is always the last one added (the one to make current in a reload).
     */
    private SearchIndexData last = null;

    private IDialogSettings settings;

    public SearchIndexDataHistory(AbstractUIPlugin plugin) {
        IDialogSettings dialogSettings = plugin.getDialogSettings();
        IDialogSettings section = dialogSettings.getSection(PAGE_NAME);
        if (section == null) {
            section = dialogSettings.addNewSection(PAGE_NAME);
        }
        this.settings = section;
    }

    public SearchIndexData getLast() {
        return last;
    }

    public void add(SearchIndexData data) {
        fPreviousSearchPatterns.remove(data); // remove from where it was
        fPreviousSearchPatterns.add(data); // add it to the end
        if (fPreviousSearchPatterns.size() > HISTORY_SIZE) {
            final Iterator<SearchIndexData> it = fPreviousSearchPatterns.iterator();
            it.next();
            it.remove();
        }
        last = data;
    }

    /**
     * Initializes itself from the stored page settings.
     */
    public void readConfiguration() {
        try {
            IDialogSettings s = settings;
            int historySize = s.getInt(STORE_HISTORY_SIZE);
            for (int i = 0; i < historySize; i++) {
                IDialogSettings histSettings = s.getSection(STORE_HISTORY + i);
                if (histSettings != null) {
                    SearchIndexData data = SearchIndexData.create(histSettings);
                    if (data != null) {
                        last = data;
                        fPreviousSearchPatterns.add(data);
                    }
                }
            }
        } catch (NumberFormatException e) {
            // ignore
        }
    }

    /**
     * Stores it current configuration in the dialog store.
     */
    public void writeConfiguration() {
        IDialogSettings s = settings;

        int historySize = Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
        s.put(STORE_HISTORY_SIZE, historySize);
        Iterator<SearchIndexData> it = fPreviousSearchPatterns.iterator();
        for (int i = 0; i < historySize; i++) {
            IDialogSettings histSettings = s.addNewSection(STORE_HISTORY + i);
            SearchIndexData data = (it.next());
            data.store(histSettings);
        }
    }

}
