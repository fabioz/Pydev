/*******************************************************************************
 * Copyright (C) 2011, 2013  Jonah Graham and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
 *     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance 
 *******************************************************************************/
package org.python.pydev.dltk.console.codegen;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.StructuredSelection;
import org.python.pydev.shared_interactive_console.console.codegen.IScriptConsoleCodeGenerator;
import org.python.pydev.shared_interactive_console.console.codegen.PythonSnippetUtils;
import org.python.pydev.shared_interactive_console.console.codegen.StructuredSelectionScriptConsoleCodeGenerator;

@SuppressWarnings("rawtypes")
public class GetGeneratorTestWorkbench extends TestCase {

    private static final class TestAdapter implements IScriptConsoleCodeGenerator {
        private final Object adaptable;

        public TestAdapter(Object adaptable) {
            this.adaptable = adaptable;
        }

        public String getPyCode() {
            return null;
        }

        public boolean hasPyCode() {
            return false;
        }

        public Object getAdaptable() {
            return adaptable;
        }
    }

    private static final class SelfGenerator implements IScriptConsoleCodeGenerator {
        public boolean hasPyCode() {
            return false;
        }

        public String getPyCode() {
            return null;
        }
    }

    private static final class SelfAdaptable implements IAdaptable {

        public Object getAdapter(Class adapter) {
            if (adapter == IScriptConsoleCodeGenerator.class) {
                return new TestAdapter(this);
            }
            return null;
        }
    }

    private static final class FactoryAdaptable {

    }

    private static final class TestAdapterFactory implements IAdapterFactory {
        public Object getAdapter(Object adaptableObject, Class adapterType) {
            if (adapterType == IScriptConsoleCodeGenerator.class) {
                if (adaptableObject instanceof FactoryAdaptable) {
                    FactoryAdaptable adaptable = (FactoryAdaptable) adaptableObject;
                    return new TestAdapter(adaptable);
                }
            }
            return null;
        }

        public Class[] getAdapterList() {
            return new Class[] { IScriptConsoleCodeGenerator.class };
        }
    }

    public void testGetScriptConsoleCodeGeneratorAdapter_NullObject() {
        assertEquals(null, PythonSnippetUtils.getScriptConsoleCodeGeneratorAdapter(null));

    }

    public void testGetScriptConsoleCodeGeneratorAdapter_SelfGenerator() {
        IScriptConsoleCodeGenerator selfGenerator = new SelfGenerator();
        assertEquals(selfGenerator, PythonSnippetUtils.getScriptConsoleCodeGeneratorAdapter(selfGenerator));

    }

    public void testGetScriptConsoleCodeGeneratorAdapter_SelfAdaptable() {
        SelfAdaptable adaptable = new SelfAdaptable();
        // test fails if adapter is not type TestAdapter
        TestAdapter adapter = (TestAdapter) PythonSnippetUtils.getScriptConsoleCodeGeneratorAdapter(adaptable);
        assertEquals(adaptable, adapter.getAdaptable());
    }

    public void testGetScriptConsoleCodeGeneratorAdapter_FactoryAdaptable() {
        TestAdapterFactory factory = new TestAdapterFactory();
        Platform.getAdapterManager().registerAdapters(factory, FactoryAdaptable.class);
        try {
            FactoryAdaptable adaptable = new FactoryAdaptable();
            // test fails if adapter is not type TestAdapter
            TestAdapter adapter = (TestAdapter) PythonSnippetUtils.getScriptConsoleCodeGeneratorAdapter(adaptable);
            assertEquals(adaptable, adapter.getAdaptable());
        } finally {
            Platform.getAdapterManager().unregisterAdapters(factory);
        }
    }

    // This tests factory registered in plugin.xml
    public void testGetAdapterForStructuredSelection() {
        IScriptConsoleCodeGenerator generator = PythonSnippetUtils
                .getScriptConsoleCodeGeneratorAdapter(StructuredSelection.EMPTY);
        assertTrue(generator != null);
        assertEquals(StructuredSelectionScriptConsoleCodeGenerator.class, generator.getClass());
    }

}
