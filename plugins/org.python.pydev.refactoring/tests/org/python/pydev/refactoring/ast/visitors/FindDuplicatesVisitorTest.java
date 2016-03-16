/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.refactoring.ast.visitors;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author fabioz
 *
 */
public class FindDuplicatesVisitorTest extends PyParserTestBase {

    public void testFindDuplicates() throws Exception {
        String s = "" + "a.get()\n" + // 0 - 8
                "a.get().bar\n" + //8 - 20 
                "a.get().a.get()\n" + //20 - 36
                "a.get(  ).foo\n" + //36 - 50 
                "a.get( #comma.get()ent\n ).foo\n" + //50 - 80
                "a.\\\nget().foo\n" + //80
                "";
        Module mod = (Module) parseLegalDocStr(s);
        stmtType b0 = mod.body[0];

        Document doc = new Document(s);
        FindDuplicatesVisitor visitor = new FindDuplicatesVisitor(new TextSelection(doc, 0, 7), ((Expr) b0).value, doc);
        mod.accept(visitor);
        List<Tuple<ITextSelection, SimpleNode>> duplicates = visitor.getDuplicates();

        Comparator<? super Tuple<Integer, Integer>> comparator = new Comparator<Tuple<Integer, Integer>>() {

            @Override
            public int compare(Tuple<Integer, Integer> o1, Tuple<Integer, Integer> o2) {
                int comp = o1.o1.compareTo(o2.o1);
                if (comp != 0) {
                    return comp;
                }
                return o1.o2.compareTo(o2.o2);
            }
        };
        Set<Tuple<Integer, Integer>> expected = new TreeSet<Tuple<Integer, Integer>>(comparator);
        expected.add(new Tuple<Integer, Integer>(8, 7));
        expected.add(new Tuple<Integer, Integer>(20, 7));
        expected.add(new Tuple<Integer, Integer>(36, 9));
        expected.add(new Tuple<Integer, Integer>(50, 25));
        expected.add(new Tuple<Integer, Integer>(80, 9));
        assertEquals(expected.size(), duplicates.size());

        Set<Tuple<Integer, Integer>> found = new TreeSet<Tuple<Integer, Integer>>(comparator);
        for (Tuple<ITextSelection, SimpleNode> tuple : duplicates) {
            found.add(new Tuple<Integer, Integer>(tuple.o1.getOffset(), tuple.o1.getLength()));
        }

        assertEquals(expected, found);
    }
}
