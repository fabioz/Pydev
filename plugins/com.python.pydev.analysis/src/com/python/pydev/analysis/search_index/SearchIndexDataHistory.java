package com.python.pydev.analysis.search_index;

import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.python.pydev.shared_core.structure.OrderedSet;

import com.python.pydev.analysis.AnalysisPlugin;

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

    public SearchIndexData getLast() {
        return last;
    }

    public void add(SearchIndexData data) {
        fPreviousSearchPatterns.remove(data); // remove from where it was
        fPreviousSearchPatterns.add(data); // add it to the end
        last = data;
    }

    /**
     * Returns the page settings for this Text search page.
     *
     * @return the page settings to be used
     */
    public IDialogSettings getDialogSettings() {
        return AnalysisPlugin.getDefault().getDialogSettingsSection(PAGE_NAME);
    }

    /**
     * Initializes itself from the stored page settings.
     */
    public void readConfiguration(IDialogSettings s) {

        try {
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
        IDialogSettings s = getDialogSettings();

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
