/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.adapter;

import org.eclipse.jface.text.Document;
import org.python.pydev.refactoring.ast.FQIdentifier;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

import com.thoughtworks.xstream.XStream;

public class ModuleAdapterTestCase extends AbstractIOTestCase {

    public ModuleAdapterTestCase(String name) {
        super(name);
    }

    @Override
    public void runTest() throws Throwable {
        StringBuffer buffer = new StringBuffer();

        ModuleAdapterTestConfig config = null;
        XStream xstream = new XStream();
        xstream.alias("config", ModuleAdapterTestConfig.class);

        ModuleAdapter module = VisitorFactory.createModuleAdapter(null, null, new Document(data.source),
                new PythonNatureStub(), createVersionProvider());
        if (data.config.length() > 0) {
            config = (ModuleAdapterTestConfig) xstream.fromXML(data.config);
        } else {
            fail("Could not unserialize configuration");
            return; /* explicit return, fail should already abort */
        }

        for (String identifier : config.resolveNames) {
            for (FQIdentifier id : module.resolveFullyQualified(identifier)) {
                buffer.append("# " + identifier + " -> " + id.getFQName());
                buffer.append("\n");
            }
        }
        buffer.append("# Imported regular modules (Alias, Realname)");
        for (String aliasModule : module.getRegularImportedModules().keySet()) {
            buffer.append("\n# " + aliasModule + " " + module.getRegularImportedModules().get(aliasModule));
        }

        buffer.append("\n");
        buffer.append("# AliasToIdentifier (Module, Realname, Alias)");
        for (FQIdentifier identifier : module.getAliasToIdentifier()) {
            buffer.append("\n# " + identifier.getModule() + " " + identifier.getRealName() + " "
                    + identifier.getAlias());
        }

        this.setTestGenerated(buffer.toString().trim());
        assertEquals(getExpected(), getGenerated());
    }
}
