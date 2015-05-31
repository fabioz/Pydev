package org.python.pydev.shared_core.index;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.python.pydev.shared_core.log.Log;

public class SearchResult {

    private int numberOfDocumentMatches;
    private ScoreDoc[] scoreDocs;
    private IndexReader reader;

    public SearchResult() {
        //Empty result
    }

    public SearchResult(ScoreDoc[] scoreDocs2, IndexReader reader2) {
        this.setResult(scoreDocs2, reader2);
    }

    private void setResult(ScoreDoc[] scoreDocs, IndexReader reader) {
        numberOfDocumentMatches = scoreDocs.length;
        this.scoreDocs = scoreDocs;
        this.reader = reader;

        for (ScoreDoc scoreDoc : scoreDocs) {
            try {
                Document document = reader.document(scoreDoc.doc);
                //System.out.println(document.getField(ITokenMapper.FILEPATH).stringValue() + " - " + scoreDoc);
            } catch (IOException e) {
                Log.log(e);
            }
        }
    }

    public int getNumberOfDocumentMatches() {
        return numberOfDocumentMatches;
    }

}
