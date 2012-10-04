/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.adapter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

public class ClassDefAdapterTestCase extends AbstractIOTestCase {

    public ClassDefAdapterTestCase(String name) {
        super(name);
    }

    @Override
    public void runTest() throws Throwable {
        StringBuffer buffer = new StringBuffer();
        ModuleAdapter module = createModuleAdapterFromDataSource();

        List<IClassDefAdapter> classes = module.getClasses();
        assertTrue(classes.size() > 0);

        for (IClassDefAdapter adapter : module.getClasses()) {
            printBaseClass(buffer, adapter);
            List<IClassDefAdapter> baseClasses = adapter.getBaseClasses();
            Collections.sort(baseClasses, new Comparator<IClassDefAdapter>() {

                public int compare(IClassDefAdapter o1, IClassDefAdapter o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            for (IClassDefAdapter base : baseClasses) {
                buffer.append("## " + adapter.getName());
                printBaseDefClass(buffer, base);
            }

        }

        String generated = (buffer.toString().trim());
        setTestGenerated(generated);
        assertEquals(getExpected(), getGenerated());
    }

    private void printBaseDefClass(StringBuffer buffer, IClassDefAdapter base) {
        buffer.append(" Base: " + base.getName());
        buffer.append("\n");

    }

    private void printBaseClass(StringBuffer buffer, IClassDefAdapter adapter) {
        buffer.append("# " + adapter.getName());
        for (String name : adapter.getBaseClassNames()) {
            buffer.append(" " + name);
        }
        buffer.append("\n");
    }
}
