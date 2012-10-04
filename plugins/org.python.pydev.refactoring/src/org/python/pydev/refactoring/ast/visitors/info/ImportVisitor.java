/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.visitors.info;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.refactoring.ast.FQIdentifier;

/**
 * This visitor will resolve module aliases to real module names and also map alias identifiers to real identifier. For the last mapping
 * list, key consists of "modulename.identifier"
 * 
 */
public class ImportVisitor extends VisitorBase {

    private SortedMap<String, String> importedModules;

    private List<FQIdentifier> aliasToFQIdentifier;

    public ImportVisitor() {
        importedModules = new TreeMap<String, String>();
        aliasToFQIdentifier = new ArrayList<FQIdentifier>();
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        // ignore
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        visitImportModules(node.names);
        return null;
    }

    private void visitImportModules(aliasType[] names) {
        for (aliasType alias : names) {
            NameTok name = (NameTok) alias.name;
            NameTok asName = (NameTok) alias.asname;

            String realName = name.id;
            String aliasName = name.id;
            if (asName != null) {
                aliasName = asName.id;
            }

            addModuleImport(aliasName, realName);
        }

    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        NameTok moduleName = (NameTok) node.module;

        visitAlias(moduleName.id, node.names);
        return null;
    }

    private void visitAlias(String prefix, aliasType[] names) {
        if (names != null && names.length > 0) {
            for (aliasType alias : names) {
                NameTok name = (NameTok) alias.name;
                NameTok asName = (NameTok) alias.asname;

                String realName = name.id;
                String aliasName = name.id;
                if (asName != null) {
                    aliasName = asName.id;
                }

                aliasToFQIdentifier.add(new FQIdentifier(prefix, realName, aliasName));

            }
        } else {
            // from <smthing> import *
            importedModules.put(prefix, prefix);
        }
    }

    private void addModuleImport(String moduleAlias, String realName) {
        if (!(importedModules.containsKey(moduleAlias))) {
            importedModules.put(moduleAlias, realName);
        }
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        node.traverse(this);
        return null;
    }

    public List<FQIdentifier> getAliasToFQIdentifier() {
        return aliasToFQIdentifier;
    }

    public SortedMap<String, String> getImportedModules() {
        return importedModules;
    }

}
