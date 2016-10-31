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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal.ICompareContext;

/**
 * @author Fabio Zadrozny
 */
public final class ProposalsComparator implements Comparator<ICompletionProposal>, ICompletionProposalSorter {

    public static class CompareContext implements ICompareContext {
        private IProject project;

        public CompareContext(IToken token) {
            this(token.getNature());
        }

        public CompareContext(IPythonNature nature) {
            this(nature != null ? nature.getProject() : null);
        }

        public CompareContext(IProject project) {
            this.project = project;
        }

        public CompareContext(PyEdit edit) {
            this(edit == null ? null : edit.getProject());
        }

        @Override
        public int getPriorityRelatedTo(ICompareContext compareContext) {
            if (compareContext instanceof CompareContext) {
                CompareContext compareContext2 = (CompareContext) compareContext;
                if (compareContext2.project != null && project != null) {
                    if (compareContext2.project.equals(project)) {
                        return ICompareContext.SAME_PROJECT_PRIORITY;
                    }
                }
            }
            if (this.project != null) {
                return ICompareContext.ANY_PROJECT_PRIORITY;
            }
            return ICompareContext.DEFAULT_PRIORITY;
        }

        @Override
        public String toString() {
            return "CompareContext[" + (project != null ? project.getName() : "null") + "]";
        }
    }

    private String qualifier;
    private String qualifierLower;
    private ICompareContext compareContext;
    private boolean qualifierHasUpper;

    public ProposalsComparator(String qualifier, ICompareContext compareContext) {
        this.compareContext = compareContext;
        this.setQualifier(qualifier);
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
        this.qualifierLower = qualifier.toLowerCase();
        this.qualifierHasUpper = this.qualifierLower.equals(this.qualifier);
    }

    public void setCompareContext(CompareContext compareContext) {
        this.compareContext = compareContext;
    }

    public int compare(String o1Str, String o2Str, int priority1, int priority2, ICompareContext ctx1,
            ICompareContext ctx2) {
        final String o1StrOriginal = o1Str;
        final String o2StrOriginal = o2Str;
        int o1Len = o1Str.length();
        int o2Len = o2Str.length();

        //START: Get the contents only to the first parens or space for the comparisons.
        {
            int iSplit1 = o1Str.indexOf('(', 0);
            int iSplit2 = o2Str.indexOf('(', 0);

            int iSpace1 = o1Str.indexOf(' ', 0);
            int iSpace2 = o2Str.indexOf(' ', 0);

            if (iSplit1 == -1 || (iSpace1 >= 0 && iSpace1 < iSplit1)) {
                iSplit1 = iSpace1;
            }

            if (iSplit2 == -1 || (iSpace2 >= 0 && iSpace2 < iSplit2)) {
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

        int v = onlyStringCompare(o1Str, o2Str);
        if (v != 0) {
            return v;
        }

        if (priority1 < priority2) {
            return -1;
        }
        if (priority1 > priority2) {
            return 1;
        }

        int ctx1Priority = ctx1 != null ? ctx1.getPriorityRelatedTo(compareContext)
                : ICompareContext.DEFAULT_PRIORITY;
        int ctx2Priority = ctx2 != null ? ctx2.getPriorityRelatedTo(compareContext)
                : ICompareContext.DEFAULT_PRIORITY;
        if (ctx1Priority != ctx2Priority) {
            if (ctx1Priority < ctx2Priority) {
                return -1;
            }
            return 1;
        }

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

    private int onlyStringCompare(String o1Str, String o2Str) {
        if (qualifierHasUpper) {
            if (o1Str.equals(qualifier) && !o2Str.equals(qualifier)) {
                return -1;
            } else {
                if (o2Str.equals(qualifier) && !o1Str.equals(qualifier)) {
                    return 1;
                }
            }
        }

        if (o1Str.equalsIgnoreCase(qualifier) && !o2Str.equalsIgnoreCase(qualifier)) {
            return -1;
        } else {
            if (o2Str.equalsIgnoreCase(qualifier) && !o1Str.equalsIgnoreCase(qualifier)) {
                return 1;
            }
        }

        if (qualifierHasUpper) {
            if (o1Str.startsWith(qualifier) && !o2Str.startsWith(qualifier)) {
                return -1;
            } else {
                if (o2Str.startsWith(qualifier) && !o1Str.startsWith(qualifier)) {
                    return 1;
                }
            }
        }

        if (o1Str.toLowerCase().startsWith(qualifierLower) && !o2Str.toLowerCase().startsWith(qualifierLower)) {
            return -1;
        } else {
            if (o2Str.toLowerCase().startsWith(qualifierLower)
                    && !o1Str.toLowerCase().startsWith(qualifierLower)) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int compare(ICompletionProposal o1, ICompletionProposal o2) {
        String o1Str = o1.getDisplayString();
        String o2Str = o2.getDisplayString();

        int p1;
        int p2;
        ICompareContext ctx1;
        ICompareContext ctx2;

        if (o1 instanceof IPyCompletionProposal) {
            IPyCompletionProposal iPyCompletionProposal = (IPyCompletionProposal) o1;
            p1 = iPyCompletionProposal.getPriority();
            ctx1 = iPyCompletionProposal.getCompareContext();
        } else {
            p1 = IPyCompletionProposal.PRIORITY_DEFAULT;
            ctx1 = null;
        }

        if (o2 instanceof IPyCompletionProposal) {
            IPyCompletionProposal iPyCompletionProposal = (IPyCompletionProposal) o2;
            p2 = iPyCompletionProposal.getPriority();
            ctx2 = iPyCompletionProposal.getCompareContext();
        } else {
            p2 = IPyCompletionProposal.PRIORITY_DEFAULT;
            ctx2 = null;
        }
        int ret = compare(
                o1Str,
                o2Str,
                p1,
                p2,
                ctx1,
                ctx2);
        // System.out.println(
        //         StringUtils.format("Compare (%s - %s): qual: %s %s and %s p1: %s p2: %s ctx1: %s ctx2: %s", ret,
        //                 this.hashCode(), qualifier,
        //                 o1Str, o2Str, p1,
        //                 p2, ctx1, ctx2));
        return ret;

    }

}
