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
package org.python.pydev.shared_core.string;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;

public class TextSelectionUtilsTest extends TestCase {

    public void testSearch() throws Exception {
        Document document = new Document("aaa bbb aaa");
        TextSelectionUtils ts = new TextSelectionUtils(document, 0);
        List<IRegion> occurrences = ts.searchOccurrences("aaa");
        assertEquals(2, occurrences.size());
    }

    public void testSearch2() throws Exception {
        Document document = new Document("aaa bbb aaa");
        TextSelectionUtils ts = new TextSelectionUtils(document, 0);
        List<IRegion> occurrences = ts.searchOccurrences("a");
        assertEquals(0, occurrences.size());
    }

    public void testGetCurrToken() throws Exception {
        Document document = new Document("aaa bbb aaa");
        TextSelectionUtils ts = new TextSelectionUtils(document, 0);
        Set<Character> separatorChars = new HashSet<Character>();
        assertEquals("aaa", ts.getCurrToken(separatorChars).o1);
    }

    public void testGetCurrToken2() throws Exception {
        Document document = new Document("aaa-bbb aaa");
        TextSelectionUtils ts = new TextSelectionUtils(document, 0);
        Set<Character> separatorChars = new HashSet<Character>();
        assertEquals("aaa-bbb", ts.getCurrToken(separatorChars).o1);
    }

    public void testGetCurrToken3() throws Exception {
        Document document = new Document("aaa-bbb aaa");
        TextSelectionUtils ts = new TextSelectionUtils(document, 0);
        Set<Character> separatorChars = new HashSet<Character>();
        separatorChars.add('-');
        assertEquals("aaa", ts.getCurrToken(separatorChars).o1);
    }
}
