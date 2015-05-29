package org.python.pydev.shared_core.index;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;

public class SearchResult {

    private int numberOfDocumentMatches;

    /* default */ void add(ScoreDoc[] scoreDocs, IndexReader reader) {
        numberOfDocumentMatches += scoreDocs.length;
        //        for (ScoreDoc scoreDoc : scoreDocs) {
        //            Document document;
        //            try {
        //                document = reader.document(scoreDoc.doc);
        //                //System.out.println(document.getField("filename").stringValue() + " - " + scoreDoc);
        //            } catch (IOException e) {
        //                Log.log(e);
        //            }
        //        }

    }

    public int getNumberOfDocumentMatches() {
        return numberOfDocumentMatches;
    }

}
