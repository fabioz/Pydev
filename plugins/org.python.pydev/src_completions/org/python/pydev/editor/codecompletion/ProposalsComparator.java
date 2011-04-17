/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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

/**
 * @author Fabio Zadrozny
 */
public final class ProposalsComparator implements Comparator<ICompletionProposal> {

    public int compare(ICompletionProposal o1, ICompletionProposal o2) {
        
        if(o1 instanceof IPyCompletionProposal && o2 instanceof IPyCompletionProposal){
            IPyCompletionProposal p1 = (IPyCompletionProposal) o1;
            IPyCompletionProposal p2 = (IPyCompletionProposal) o2;
            
            if(p1.getPriority() < p2.getPriority()){
                return -1;
            }
            if(p1.getPriority() > p2.getPriority()){
                return 1;
            }
        }
        //if it is not an IPyCompletionProposal, it has default priority.
        else if(o1 instanceof IPyCompletionProposal){
            IPyCompletionProposal p1 = (IPyCompletionProposal) o1;
            
            if(p1.getPriority() < IPyCompletionProposal.PRIORITY_DEFAULT){
                return -1;
            }
            if(p1.getPriority() > IPyCompletionProposal.PRIORITY_DEFAULT){
                return 1;
            }
        }
        
        else if(o2 instanceof IPyCompletionProposal){
            IPyCompletionProposal p2 = (IPyCompletionProposal) o2;
            
            if(IPyCompletionProposal.PRIORITY_DEFAULT < p2.getPriority()){
                return -1;
            }
            if(IPyCompletionProposal.PRIORITY_DEFAULT > p2.getPriority()){
                return 1;
            }
        }
        
        
        String o1Str = o1.getDisplayString();
        String o2Str = o2.getDisplayString();
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
        
        if(o1StartsWithUnder != o2StartsWithUnder){
            if(o1StartsWithUnder){
                return 1;
            }
            return -1;
        }
        
        return o1Str.compareToIgnoreCase(o2Str);
    }

}
