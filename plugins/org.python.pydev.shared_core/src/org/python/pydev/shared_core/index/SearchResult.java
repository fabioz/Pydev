package org.python.pydev.shared_core.index;

import org.apache.lucene.search.ScoreDoc;

public class SearchResult {

    private int numberOfDocumentMatches;

    public SearchResult() {
        //Empty result
    }

    public SearchResult(ScoreDoc[] scoreDocs) {
        this.numberOfDocumentMatches = scoreDocs.length;
    }

    public int getNumberOfDocumentMatches() {
        return numberOfDocumentMatches;
    }

}
