/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.partitioner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;

public class PartitionMerger {

    private static final class PositionComparator implements Comparator<Position> {
        public int compare(Position o1, Position o2) {
            return o1.offset - o2.offset;
        }
    }

    /**
     * Important: don't change the initial Position[] received!
     */
    public static List<TypedPosition> sortAndMergePositions(Position[] positions, int docLen) {
        Arrays.sort(positions, new PositionComparator());

        //Fill in the spaces.
        ArrayList<TypedPosition> lst = new ArrayList<TypedPosition>(positions.length);
        int lastOffset = 0;
        TypedPosition last = null;
        for (int j = 0; j < positions.length; j++) {
            Position position = positions[j];
            if (position instanceof TypedPosition) {
                TypedPosition typedPosition = (TypedPosition) position;
                String type = typedPosition.getType();

                int currOffset = typedPosition.getOffset();
                int currLen = typedPosition.getLength();
                if (lastOffset < currOffset) {
                    if (last != null && last.getType().equals(IDocument.DEFAULT_CONTENT_TYPE)) {
                        //Fix the existing one
                        last.setLength(last.getLength() + currOffset - lastOffset);

                    } else {
                        TypedPosition newPos = new TypedPosition(lastOffset, currOffset - lastOffset,
                                IDocument.DEFAULT_CONTENT_TYPE);
                        lst.add(newPos);
                        last = newPos;
                    }
                }
                if (last != null && last.getType().equals(type)) {
                    //Fix the existing one
                    last.setLength(last.getLength() + currLen);
                } else {
                    TypedPosition newPos = new TypedPosition(currOffset, currLen, type);
                    lst.add(newPos);
                    last = newPos;
                }
                lastOffset = currOffset + currLen;
            }
        }
        if (lastOffset < docLen) {
            if (last != null && last.getType().equals(IDocument.DEFAULT_CONTENT_TYPE)) {
                //Fix the existing one
                last.setLength(last.getLength() + docLen - lastOffset);
            } else {
                lst.add(new TypedPosition(lastOffset, docLen - lastOffset, IDocument.DEFAULT_CONTENT_TYPE));
            }
        }
        return lst;
    }

}
