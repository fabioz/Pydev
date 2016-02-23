/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 7, 2004
 */
package org.python.pydev.shared_core.structure;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Struct class, holds line/column information. 
 * 
 * Has static utility functions for Location->offset/line conversions
 */
public class Location {

    /**
     * Location: line and col start at 0
     */
    public int line;
    public int column;

    static Location MIN_LOCATION = new Location(0, 0);
    static Location MAX_LOCATION = new Location(Integer.MAX_VALUE, Integer.MAX_VALUE);

    public Location() {
        line = column = 0;
    }

    public Location(int line, int column) {
        this.line = line;
        this.column = column;
    }

    /**
     * Conversion to document coordinates.
     */
    public int toOffset(IDocument document) throws BadLocationException {
        return document.getLineOffset(line) + column;
    }

    /**
     * @return true if location is completely enclosed between start & end.
     */
    public boolean contained(Location start, Location end) {
        boolean startOk = (line > start.line || line == start.line && column >= start.column);
        boolean endOk = startOk ? (line < end.line || line == end.line && column <= end.column) : false;
        return startOk && endOk;
    }

    @Override
    public String toString() {
        return "L:" + Integer.toString(line) + " C:" + Integer.toString(column);
    }

    /**
     * standard compare
     * @return 1 means I win, -1 means argument wins, 0 means equal
     */
    public int compareTo(Location l) {
        if (line > l.line)
            return 1;
        if (line < l.line)
            return -1;
        if (column > l.column)
            return 1;
        if (column < l.column)
            return -1;
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Location)) {
            return false;
        }
        Location l = (Location) obj;
        return l.line == line && l.column == column;
    }

    @Override
    public int hashCode() {
        return (line * 99) + (column * 5);
    }

    /**
     * Utility: Converts document's offset to Location
     * @return Location
     */
    static public Location offsetToLocation(IDocument document, int offset) {
        try {
            int line = document.getLineOfOffset(offset);
            int line_start = document.getLineOffset(line);
            return new Location(line, offset - line_start);
        } catch (BadLocationException e) {
            return new Location();
        }
    }
}
