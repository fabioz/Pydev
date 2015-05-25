/******************************************************************************
* Copyright (C) 2007-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.utils;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;

public final class TestUtils {

    private static final Pattern CURSOR_PATTERN = Pattern.compile("##\\|");
    private static final String CURSOR = "##|";

    private TestUtils() {
    }

    public static final class Marker {
        public final int beginLine;
        public final String expr;
        public final String type;

        public Marker(int beginLine, String expr, String type) {
            this.beginLine = beginLine;
            this.expr = expr;
            this.type = type;
        }

    }

    public static List<Marker> getMarkers(String sourceCode) {
        List<Marker> markers = new LinkedListWarningOnSlowOperations<Marker>();

        String[] lines = sourceCode.split("\n");

        int beginLine = 0;

        for (String line : lines) {
            beginLine += 1;

            line = line.trim();
            int pos = line.indexOf("##");
            if (pos < 0) {
                continue;
            }

            StringTokenizer tok = new StringTokenizer(line.substring(pos + 2));
            String test = tok.nextToken();

            if ("exit".equals(test)) {
                break;
            } else if ("type".equals(test)) {
                String type = tok.nextToken();
                String expr = line.substring(0, line.indexOf("##")).trim();

                markers.add(new Marker(beginLine, expr, type));
            }

        }

        return markers;
    }

    public static final class Cursors {

        public final String text;
        public final List<Integer> positions;

        public Cursors(String text, List<Integer> positions) {
            this.text = text;
            this.positions = positions;
        }

    }

    public static Cursors findCursors(String input) {
        List<Integer> positions = new LinkedListWarningOnSlowOperations<Integer>();
        String text = input;

        while (true) {
            int index = text.indexOf(CURSOR);

            if (index < 0) {
                break;
            }

            positions.add(index);
            text = CURSOR_PATTERN.matcher(text).replaceFirst("");
        }

        return new Cursors(text, positions);
    }

}
