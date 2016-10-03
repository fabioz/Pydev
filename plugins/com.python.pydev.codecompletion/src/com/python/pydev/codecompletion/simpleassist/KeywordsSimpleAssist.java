/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package com.python.pydev.codecompletion.simpleassist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.IPySyntaxHighlightingAndCodeCompletionEditor;
import org.python.pydev.editor.simpleassist.ISimpleAssistParticipant;
import org.python.pydev.editor.simpleassist.ISimpleAssistParticipant2;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

import com.python.pydev.codecompletion.ui.CodeCompletionPreferencesPage;

/**
 * Auto completion for keywords:
 * 
 * import keyword
 * >>> for k in keyword.kwlist: print k
and
assert
break
class
continue
def
del
elif
else
except
exec
finally
for
from
global
if
import
in
is
lambda
not
or
pass
print
raise
return
try
while
yield
 * @author Fabio
 */
public class KeywordsSimpleAssist implements ISimpleAssistParticipant, ISimpleAssistParticipant2 {

    public static String defaultKeywordsAsString() {
        String[] KEYWORDS = new String[] { "and", "assert", "break", "class", "continue", "def", "del",
                //                "elif", -- starting with 'e'
                //                "else:", -- starting with 'e'
                //                "except:",  -- ctrl+1 covers for try..except/ starting with 'e'
                //                "exec", -- starting with 'e'
                "finally:", "for", "from", "global",
                //                "if", --too small
                "import",
                //                "in", --too small
                //                "is", --too small
                "lambda", "not",
                //                "or", --too small
                "pass", "print", "raise", "return",
                //                "try:", -- ctrl+1 covers for try..except
                "while", "with", "yield",

                //the ones below were not in the initial list
                "self", "__init__",
                //                "as", --too small
                "False", "None", "object", "True" };
        return wordsAsString(KEYWORDS);
    }

    //very simple cache (this might be requested a lot).
    private static String cache;
    private static String[] cacheRet;

    public static String[] stringAsWords(String keywords) {
        if (cache != null && cache.equals(keywords)) {
            return cacheRet;
        }
        StringTokenizer tokenizer = new StringTokenizer(keywords);
        ArrayList<String> strs = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            strs.add(tokenizer.nextToken());
        }
        cache = keywords;
        cacheRet = strs.toArray(new String[0]);
        return cacheRet;
    }

    /**
     * @param keywords keywords to be gotten as string
     * @return a string with all the passed words separated by '\n'
     */
    public static String wordsAsString(String[] keywords) {
        StringBuffer buf = new StringBuffer();
        for (String string : keywords) {
            buf.append(string);
            buf.append("\n");
        }
        return buf.toString();
    }

    /**
     * @see ISimpleAssistParticipant
     */
    @Override
    public Collection<ICompletionProposal> computeCompletionProposals(String activationToken, String qualifier,
            PySelection ps, IPySyntaxHighlightingAndCodeCompletionEditor edit, int offset) {
        boolean isPy3Syntax = false;
        if (CodeCompletionPreferencesPage.forcePy3kPrintOnPy2()) {
            isPy3Syntax = true;

        } else {
            try {
                IPythonNature nature = edit.getPythonNature();
                if (nature != null) {
                    isPy3Syntax = nature.getGrammarVersion() >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0;
                }
            } catch (MisconfigurationException e) {
            }
        }
        return innerComputeProposals(activationToken, qualifier, offset, false, isPy3Syntax);
    }

    /**
     * @see ISimpleAssistParticipant2
     */
    @Override
    public Collection<ICompletionProposal> computeConsoleProposals(String activationToken, String qualifier,
            int offset) {
        return innerComputeProposals(activationToken, qualifier, offset, true, false);
    }

    /**
     * Collects simple completions (keywords)
     * 
     * @param activationToken activation token used
     * @param qualifier qualifier used
     * @param offset offset at which the completion was requested
     * @param buildForConsole whether the completions should be built for the console or not
     * @param isPy3Syntax if py 3 syntax we'll treat print differently.
     * @return a list with the completions available.
     */
    private Collection<ICompletionProposal> innerComputeProposals(String activationToken, String qualifier, int offset,
            boolean buildForConsole, boolean isPy3Syntax) {

        List<ICompletionProposal> results = new ArrayList<ICompletionProposal>();
        //check if we have to use it
        if (!CodeCompletionPreferencesPage.useKeywordsCodeCompletion()) {
            return results;
        }

        //get them
        int qlen = qualifier.length();
        if (activationToken.equals("") && qualifier.equals("") == false) {
            for (String keyw : CodeCompletionPreferencesPage.getKeywords()) {
                if (keyw.startsWith(qualifier) && !keyw.equals(qualifier)) {
                    if (buildForConsole) {
                        //In the console, only show the simple completions without any special treatment
                        results.add(new PyCompletionProposal(keyw, offset - qlen, qlen, keyw.length(),
                                PyCompletionProposal.PRIORITY_DEFAULT, null));

                    } else {
                        //in the editor, we'll create a special proposal with more features
                        if (isPy3Syntax) {
                            if ("print".equals(keyw)) {
                                keyw = "print()";//Handling print in py3k.
                            }
                        }
                        results.add(new SimpleAssistProposal(keyw, offset - qlen, qlen, keyw.length(),
                                PyCompletionProposal.PRIORITY_DEFAULT, null));
                    }
                }
            }
        }

        return results;
    }

}
