/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.python.pydev.parser.jython.Token;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * A helper iterator class that can look ahead x tokens breaking when some new 
 * compound context starts (if wanted)
 */
final class TokensIterator implements Iterator<Token> {

    private Token currentToken;
    private int tokensToIterate;
    private int tokensIterated;
    private boolean breakOnIndentsDedentsAndNewCompounds;
    private ITokenManager tokenManager;
    private Tuple<Token, Token> prevAndReturned;
    private final HashSet<Integer> contextsToBreak = new HashSet<Integer>();
    private boolean calculatedNext;
    private boolean isFirst;

    //    private int parensLevel=0;

    /**
     * @param firstIterationToken this will be the 1st token returned in the iteration, and the next token can be the
     * next token already set in the given token or a new token loaded from the manager.
     * 
     * @param tokensToIterate if 1, will only yield 1 token, if 2, can get up to 2 tokens and so on.
     * 
     * @param breakOnIndentsDedentsAndNewCompounds if true, it'll break whenever we find a new compound context, 
     * or an indent or dedent.
     * 
     * @return an iterator that'll iterate through the next tokens.
     */
    public TokensIterator(ITokenManager tokenManager, Token firstIterationToken, int tokensToIterate,
            boolean breakOnIndentsDedentsAndNewCompounds) {
        contextsToBreak.add(tokenManager.getIndentId());
        contextsToBreak.add(tokenManager.getDedentId());

        contextsToBreak.add(tokenManager.getIfId());
        contextsToBreak.add(tokenManager.getWhileId());
        contextsToBreak.add(tokenManager.getForId());
        contextsToBreak.add(tokenManager.getTryId());

        contextsToBreak.add(tokenManager.getDefId());
        contextsToBreak.add(tokenManager.getClassId());
        contextsToBreak.add(tokenManager.getAtId());

        reset(tokenManager, firstIterationToken, tokensToIterate, breakOnIndentsDedentsAndNewCompounds);
    }

    public void reset(ITokenManager tokenManager, Token firstIterationToken, int tokensToIterate,
            boolean breakOnIndentsDedentsAndNewCompounds) {
        this.currentToken = firstIterationToken;
        this.tokenManager = tokenManager;
        this.tokensToIterate = tokensToIterate;
        this.breakOnIndentsDedentsAndNewCompounds = breakOnIndentsDedentsAndNewCompounds;
        this.tokensIterated = 0;
        this.tokensToIterate = 0;
        this.prevAndReturned = new Tuple<Token, Token>(null, null);
        this.calculatedNext = false;
        this.isFirst = true;
        //        this.parensLevel = 0;
    }

    @Override
    public boolean hasNext() {
        if (isFirst) {
            return currentToken != null;
        }
        if (!calculatedNext) {
            calculateNext();
            calculatedNext = true;
        }
        return currentToken != null && currentToken.next != null;
    }

    @Override
    public Token next() {
        if (isFirst) {
            isFirst = false;
            return currentToken;
        }
        if (!calculatedNext) {
            calculateNext();
        }

        calculatedNext = false;
        tokensIterated += 1;
        if (currentToken == null || currentToken.next == null) {
            throw new NoSuchElementException();
        }
        prevAndReturned.o1 = prevAndReturned.o2;
        prevAndReturned.o2 = currentToken.next;

        if (tokensIterated == tokensToIterate) {
            currentToken = null;
        } else {
            if (currentToken != null && currentToken.kind == tokenManager.getEofId()) {
                //always break on EOF
                currentToken = null;

            } else if (breakOnIndentsDedentsAndNewCompounds) {
                if (currentToken != null && contextsToBreak.contains(currentToken.kind)) {
                    currentToken = null; // we must break it now (indent or dedent found)
                }
            }
        }
        if (currentToken != null) {
            currentToken = currentToken.next;
        }
        return prevAndReturned.o2;
    }

    private void calculateNext() {
        if (currentToken == null) {
            return;
        }

        if (currentToken.kind == tokenManager.getEofId()) {
            //found end of file!
            currentToken = null;
            return;
        }

        if (currentToken.next == null) {
            currentToken.next = AbstractGrammarWalkHelpers.nextTokenConsideringNewLine(tokenManager);
            //            if(currentToken.next != null){
            //                int id = currentToken.next.kind;
            //                if(id == tokenManager.getIndentId()){
            //                    
            //                } else if(id == tokenManager.getRparenId() || id == tokenManager.getRbracketId() || id == tokenManager.getRbraceId()){
            //                    parensLevel--;
            //                    
            //                }else if(id == tokenManager.getLparenId() || id == tokenManager.getLbracketId() || id == tokenManager.getLbraceId()){
            //                    parensLevel++;
            //                }
            //            }
        }

    }

    @Override
    public void remove() {
        throw new RuntimeException("Not implemented");
    }

    public Token getBeforeLastReturned() {
        return this.prevAndReturned.o1;
    }

}