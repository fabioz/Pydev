/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.shared_core.string.StringUtils;

import junit.framework.TestCase;

public class PyCodeCompletionUtilsTest extends TestCase {

    public void testSimpleCompare() throws Exception {
        List<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
        props.add(new CompletionProposal("foo1(a, b)", 0, 0, 0));
        props.add(new CompletionProposal("foo1", 0, 0, 0));
        String qualifier = "foo";
        boolean onlyForCalltips = false;

        ICompletionProposal[] proposals = PyCodeCompletionUtils.onlyValid(props, qualifier, onlyForCalltips,
                false, null);
        PyCodeCompletionUtils.sort(proposals, qualifier, null);
        compare(new String[] { "foo1", "foo1(a, b)" }, proposals);
    }

    public void testCompareWithUnder() throws Exception {
        List<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
        props.add(new CompletionProposal("_foo1(a, b)", 0, 0, 0));
        props.add(new CompletionProposal("__foo1__", 0, 0, 0));
        props.add(new CompletionProposal("__foo1__()", 0, 0, 0));
        props.add(new CompletionProposal("__foo1()", 0, 0, 0));
        props.add(new CompletionProposal("__foo1 - __something__", 0, 0, 0));
        String qualifier = "_";
        boolean onlyForCalltips = false;

        ICompletionProposal[] proposals = PyCodeCompletionUtils.onlyValid(props, qualifier, onlyForCalltips,
                false, null);
        PyCodeCompletionUtils.sort(proposals, qualifier, null);
        compare(new String[] { "_foo1(a, b)", "__foo1 - __something__", "__foo1()", "__foo1__", "__foo1__()", },
                proposals);
    }

    public void testExactMatches() throws Exception {
        List<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
        props.add(new CompletionProposal("SystemError", 0, 0, 0));
        props.add(new CompletionProposal("system", 0, 0, 0));
        String qualifier = "sys";
        boolean onlyForCalltips = false;

        ICompletionProposal[] proposals = PyCodeCompletionUtils.onlyValid(props, qualifier, onlyForCalltips,
                false, null);
        PyCodeCompletionUtils.sort(proposals, qualifier, null);
        compare(new String[] { "system", "SystemError", }, proposals);
    }

    private void compare(String[] strings, ICompletionProposal[] proposals) {
        //        for (int i = 0; i < proposals.length; i++) {
        //            System.out.println(proposals[i].getDisplayString());
        //        }
        assertEquals(strings.length, proposals.length);
        List<String> lst = new ArrayList<>();
        for (int i = 0; i < proposals.length; i++) {
            lst.add(proposals[i].getDisplayString());
        }
        assertEquals(StringUtils.join("\n", strings),
                StringUtils.join("\n", lst));

    }
}
