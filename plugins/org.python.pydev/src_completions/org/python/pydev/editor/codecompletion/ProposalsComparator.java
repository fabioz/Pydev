/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.util.Comparator;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;

/**
 * @author Fabio Zadrozny
 */
public final class ProposalsComparator implements Comparator<ICompletionProposal> {

    public int compare(ICompletionProposal o1, ICompletionProposal o2) {

        if (o1 instanceof IPyCompletionProposal && o2 instanceof IPyCompletionProposal) {
            IPyCompletionProposal p1 = (IPyCompletionProposal) o1;
            IPyCompletionProposal p2 = (IPyCompletionProposal) o2;

            int priority1 = p1.getPriority();
            int priority2 = p2.getPriority();
            if (priority1 < priority2) {
                return -1;
            }
            if (priority1 > priority2) {
                return 1;
            }
        }
        //if it is not an IPyCompletionProposal, it has default priority.
        else if (o1 instanceof IPyCompletionProposal) {
            IPyCompletionProposal p1 = (IPyCompletionProposal) o1;

            int priority1 = p1.getPriority();
            if (priority1 < IPyCompletionProposal.PRIORITY_DEFAULT) {
                return -1;
            }
            if (priority1 > IPyCompletionProposal.PRIORITY_DEFAULT) {
                return 1;
            }
        }

        else if (o2 instanceof IPyCompletionProposal) {
            IPyCompletionProposal p2 = (IPyCompletionProposal) o2;

            int priority2 = p2.getPriority();
            if (IPyCompletionProposal.PRIORITY_DEFAULT < priority2) {
                return -1;
            }
            if (IPyCompletionProposal.PRIORITY_DEFAULT > priority2) {
                return 1;
            }
        }

        String o1Str = o1.getDisplayString();
        String o2Str = o2.getDisplayString();
        String o1StrOriginal = o1Str;
        String o2StrOriginal = o2Str;

        boolean o1StartsWithUnder = false;
        boolean o2StartsWithUnder = false;

        try {
            o1StartsWithUnder = o1Str.charAt(0) == '_';
        } catch (Exception e1) {
            //Shouldn't happen (empty completion?), but if it does, just ignore...
        }
        try {
            o2StartsWithUnder = o2Str.charAt(0) == '_';
        } catch (Exception e) {
            //Shouldn't happen (empty completion?), but if it does, just ignore...
        }

        if (o1StartsWithUnder != o2StartsWithUnder) {
            if (o1StartsWithUnder) {
                return 1;
            }
            return -1;

        } else if (o1StartsWithUnder) {//both start with '_' at this point, let's check for '__'
            int o1Len = o1Str.length();
            int o2Len = o2Str.length();

            if (o1Len > 1) {
                o1StartsWithUnder = o1Str.charAt(1) == '_';
            } else {
                o1StartsWithUnder = false;
            }
            if (o2Len > 1) {
                o2StartsWithUnder = o2Str.charAt(1) == '_';
            } else {
                o2StartsWithUnder = false;
            }

            if (o1StartsWithUnder != o2StartsWithUnder) {
                if (o1StartsWithUnder) {
                    return 1;
                }
                return -1;
            }

            //Ok, at this point, both start with '__', so, the final thing is checking for '__' in the end.

            //START: Get the contents only to the first parens or space for the comparisons.
            {
                int iSplit1 = o1Str.indexOf('(', 0);
                int iSplit2 = o2Str.indexOf('(', 0);

                int iSpace1 = o1Str.indexOf(' ', 0);
                int iSpace2 = o2Str.indexOf(' ', 0);

                if (iSpace1 >= 0 && iSpace1 < iSplit1) {
                    iSplit1 = iSpace1;
                }

                if (iSpace2 >= 0 && iSpace2 < iSplit2) {
                    iSplit2 = iSpace2;
                }

                if (iSplit1 >= 0) {
                    o1Str = o1Str.substring(0, iSplit1);
                    o1Len = o1Str.length();
                }
                if (iSplit2 >= 0) {
                    o2Str = o2Str.substring(0, iSplit2);
                    o2Len = o2Str.length();
                }
            }
            //END: Get the contents only to the first parens or space for the comparisons.

            boolean o1EndsWithUnder = false;
            boolean o2EndsWithUnder = false;

            if (o1Len > 2) {
                o1EndsWithUnder = o1Str.charAt(o1Len - 1) == '_';
            }
            if (o2Len > 2) {
                o2EndsWithUnder = o2Str.charAt(o2Len - 1) == '_';
            }

            if (o1EndsWithUnder != o2EndsWithUnder) {
                if (o1EndsWithUnder) {
                    return 1;
                }
                return -1;
            }

        }

        return o1StrOriginal.compareToIgnoreCase(o2StrOriginal);
    }

}
