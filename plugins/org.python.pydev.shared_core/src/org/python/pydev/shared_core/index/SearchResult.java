/******************************************************************************
* Copyright (C) 2015  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
******************************************************************************/
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
