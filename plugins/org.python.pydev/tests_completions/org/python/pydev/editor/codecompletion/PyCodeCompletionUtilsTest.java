/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import junit.framework.TestCase;

public class PyCodeCompletionUtilsTest extends TestCase {

    public void testIt() throws Exception {
        List<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
        props.add(new CompletionProposal("foo1(a, b)", 0, 0, 0));
        props.add(new CompletionProposal("foo1", 0, 0, 0));
        String qualifier = "foo";
        boolean onlyForCalltips = false;
        
        ICompletionProposal[] proposals = PyCodeCompletionUtils.onlyValidSorted(props, qualifier, onlyForCalltips);
        compare(new String[]{"foo1", "foo1(a, b)"}, proposals);
    }

    private void compare(String[] strings, ICompletionProposal[] proposals) {
//        for (int i = 0; i < proposals.length; i++) {
//            System.out.println(proposals[i].getDisplayString());
//        }
        for (int i = 0; i < proposals.length; i++) {
            assertEquals(strings[i], proposals[i].getDisplayString());
        }
        assertEquals(strings.length, proposals.length);
        
        
    }
}
