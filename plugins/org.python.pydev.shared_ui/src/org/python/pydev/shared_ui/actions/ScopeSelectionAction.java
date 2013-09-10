/******************************************************************************
* Copyright (C) 2011-2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial API and implementation
*     Andrew Ferrazzutti <aferrazz@redhat.com> - ongoing maintenance
******************************************************************************/
package org.python.pydev.shared_ui.actions;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.parsing.IScopesParser;
import org.python.pydev.shared_core.parsing.Scopes;
import org.python.pydev.shared_core.string.ICharacterPairMatcher2;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.editor.BaseEditor;

public class ScopeSelectionAction {

    public static final String SELECTION_SCOPE_CACHE = "_SELECTION_SCOPE_CACHE_";

    public void perform(IDocument doc, ITextSelection selection, BaseEditor editor) {
        FastStack<IRegion> cache = getCache(editor);
        Region initialRegion = new Region(selection.getOffset(), selection.getLength());
        if (cache.size() > 0) {
            IRegion peek = cache.peek();
            if (!peek.equals(initialRegion)) {
                cache.clear();
            }
        }
        if (cache.size() == 0) {
            cache.push(initialRegion);
        }
        ITextSelection newSelection = getNewSelection(doc, selection, editor);
        if (initialRegion.equals(new Region(newSelection.getOffset(), newSelection.getLength()))) {
            return;
        }

        editor.setSelection(newSelection.getOffset(), newSelection.getLength());
        cache.push(new Region(newSelection.getOffset(), newSelection.getLength()));
    }

    public ITextSelection getNewSelection(IDocument doc, ITextSelection selection, BaseEditor editor) {
        return this.getNewSelection(doc, selection, editor.getPairMatcher(), editor.createScopesParser());
    }

    public ITextSelection getNewSelection(IDocument doc, ITextSelection selection, ICharacterPairMatcher2 pairMatcher,
            IScopesParser scopesParser) {
        try {
            TextSelectionUtils ps = new TextSelectionUtils(doc, selection);
            String selectedText = ps.getSelectedText();
            if (selectedText.length() == 0) {
                //Select the current word
                Tuple<String, Integer> currToken = ps.getCurrToken();
                if (currToken.o1.length() > 0) {
                    return new TextSelection(currToken.o2, currToken.o1.length());
                } else {
                    char c = '\0';
                    try {
                        c = ps.getCharAtCurrentOffset();
                    } catch (BadLocationException e) {
                        //Ignore (end of document is selected).
                    }
                    if (StringUtils.isClosingPeer(c)) {
                        int openingOffset = pairMatcher.searchForOpeningPeer(ps.getAbsoluteCursorOffset(),
                                StringUtils.getPeer(c), c, doc);
                        if (openingOffset >= 0) {
                            return new TextSelection(openingOffset, ps.getAbsoluteCursorOffset() - openingOffset + 1);
                        }
                    }
                }
            } else {
                //There's already some text selected
                boolean tryMatchWithQualifier = true;
                boolean hasDotSelected = false; //value only valid if tryMatchWithQualifier == true!
                for (int i = 0; i < selectedText.length(); i++) {
                    char c = selectedText.charAt(i);
                    if (c == '.') {
                        hasDotSelected = true;
                        continue;
                    }
                    if (!Character.isJavaIdentifierPart(c)) {
                        tryMatchWithQualifier = false;
                        break;
                    }
                }
                if (tryMatchWithQualifier) {
                    Tuple<String, Integer> currToken = ps.getCurrToken();
                    if (!hasDotSelected && !currToken.o1.equals(selectedText)) {
                        return new TextSelection(currToken.o2, currToken.o1.length());
                    } else {
                        //The selected text is not equal to the current token, let's see if we have to select a full dotted word
                        Tuple<String, Integer> currDottedToken = ps.getCurrDottedStatement(pairMatcher);
                        if (!currDottedToken.o1.equals(selectedText)) {
                            return new TextSelection(currDottedToken.o2, currDottedToken.o1.length());
                        }
                    }
                }
            }
            Scopes scopes = scopesParser.createScopes(doc);
            //            System.out.println(scopes.debugString(doc));
            IRegion scope = scopes.getScopeForSelection(selection.getOffset(), selection.getLength());
            if (scope != null) {
                return new TextSelection(scope.getOffset(), scope.getLength());
            }

        } catch (BadLocationException e) {
            Log.log(e);
        }

        return selection;
    }

    private static String getCurrentSelectionCacheKey(BaseEditor pyEdit) {
        IDocument doc = pyEdit.getDocument();

        int length = doc.getLength();
        String key = Integer.toString(length);
        if (doc instanceof IDocumentExtension4) {
            IDocumentExtension4 document = (IDocumentExtension4) doc;
            long modificationStamp = document.getModificationStamp();
            key += " - " + modificationStamp;
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    public static FastStack<IRegion> getCache(BaseEditor pyEdit) {
        Map<String, Object> cache = pyEdit.getCache();
        String key = getCurrentSelectionCacheKey(pyEdit);
        try {
            Tuple<String, FastStack<IRegion>> object = (Tuple<String, FastStack<IRegion>>) cache
                    .get(SELECTION_SCOPE_CACHE);

            if (object != null) {
                if (key.equals(object.o1)) {
                    return object.o2;
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }

        FastStack<IRegion> stack = new FastStack<IRegion>(20);
        cache.put(SELECTION_SCOPE_CACHE, new Tuple<String, FastStack<IRegion>>(key, stack));
        return stack;
    }

    public void deselect(BaseEditor editor) {
        FastStack<IRegion> stack = ScopeSelectionAction.getCache(editor);

        ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
        Region region = new Region(selection.getOffset(), selection.getLength());
        Iterator<IRegion> it = stack.topDownIterator();
        while (it.hasNext()) {
            IRegion iRegion = it.next();
            stack.pop(); //After getting the latest, pop it.

            if (iRegion.equals(region)) {
                if (stack.size() > 0) {
                    IRegion peek = stack.peek();
                    editor.setSelection(peek.getOffset(), peek.getLength());
                }
                break;
            }
        }
    }
}
