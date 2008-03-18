/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;

/**
 * This document partitioner does not actually partition things, but receives ranges with the 
 * valid styles, so, each range maps to a style.
 * 
 * Aside from that, all is marked as the default content type.
 */
public class ScriptConsolePartitioner implements IConsoleDocumentPartitioner {

    private static final String[] LEGAL_CONTENT_TYPES = new String[]{IDocument.DEFAULT_CONTENT_TYPE};
    
    private List<StyleRange> ranges = new ArrayList<StyleRange>();

    public ScriptConsolePartitioner() {
    }

    /**
     * Adds a given style range.
     * 
     * When a range is added, the ranges that were added before must be removed/updated if the added range
     * has some intersection with a previous one.
     * 
     * The ranges must all be set sequentially, so, all the ranges that have some intersection with that 
     * range must be removed/updated.
     * 
     * @param r the range to be added.
     */
    public void addRange(StyleRange r) {
        if(r.length > 0){
            for(int i=ranges.size()-1; i>=0; i--){
                StyleRange last = ranges.get(i);
                int end = last.start+last.length;
                if(end > r.start){
                    if(r.start <= last.start){
                        ranges.remove(i);
                    }else{
                        last.length = r.start-last.start;
                    }
                }else{
                    //
                    break;
                }
            }
            ranges.add(r);
        }
    }

    /**
     * @return the ranges that intersect with the given offset/length.
     */
    public StyleRange[] getStyleRanges(int offset, int length) {
        int lastOffset = offset;
        
        List<StyleRange> result = new ArrayList<StyleRange>();
        for (StyleRange r:ranges) {
            if ((r.start >= offset && r.start <= offset + length) || (r.start < offset && r.start+r.length > offset)){
                result.add(r);
                lastOffset = r.start+r.length;
            }
        }

        if(lastOffset < offset+length){
            //if we haven't been able to cover the whole range, there's probably something wrong (so, let's 
            //leave it in gray so that we know about that).
            StyleRange lastPart = new StyleRange(lastOffset, length-lastOffset, Display.getDefault().getSystemColor(SWT.COLOR_GRAY),
                    Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            result.add(lastPart);
        }
        
        return (StyleRange[]) result.toArray(new StyleRange[result.size()]);
    }

    
    
    
    //-------------------- Just return default content type for any related request ------------------------------------
    
    
    
    public boolean isReadOnly(int offset) {
        return false;
    }

    public ITypedRegion[] computePartitioning(int offset, int length) {
        return new TypedRegion[]{new TypedRegion(offset, length, IDocument.DEFAULT_CONTENT_TYPE)};
    }

    public void connect(IDocument document) {
    }

    public void disconnect() {
    }

    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    public boolean documentChanged(DocumentEvent event) {
        return false;
    }

    public String getContentType(int offset) {
        return IDocument.DEFAULT_CONTENT_TYPE;
    }

    public String[] getLegalContentTypes() {
        return LEGAL_CONTENT_TYPES;
    }

    public ITypedRegion getPartition(int offset) {
        return new TypedRegion(offset, 1, IDocument.DEFAULT_CONTENT_TYPE);
    }
}
