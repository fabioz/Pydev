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
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.context.LocalAttributeVisitor;
import org.python.pydev.refactoring.ast.visitors.context.PropertyVisitor;
import org.python.pydev.refactoring.ast.visitors.context.ScopeAssignedVisitor;

public class ClassDefAdapter extends AbstractScopeNode<ClassDef> implements IClassDefAdapter {

    private static final String OBJECT = "object";

    private List<SimpleAdapter> attributes;

    private List<PropertyAdapter> properties;

    public ClassDefAdapter(ModuleAdapter module, AbstractScopeNode<?> parent, ClassDef node, AdapterPrefs adapterPrefs) {
        super(module, parent, node, adapterPrefs);
        this.attributes = null;
        this.properties = null;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#getBaseClassNames()
     */
    public List<String> getBaseClassNames() {
        return nodeHelper.getBaseClassName(getASTNode());
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#getBaseClasses()
     */
    public List<IClassDefAdapter> getBaseClasses() throws MisconfigurationException {
        return getModule().getBaseClasses(this);
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#hasBaseClass()
     */
    public boolean hasBaseClass() {
        return getBaseClassNames().size() > 0;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#getAttributes()
     */
    public List<SimpleAdapter> getAttributes() {
        if (attributes == null) {
            LocalAttributeVisitor visitor = VisitorFactory.createContextVisitor(LocalAttributeVisitor.class,
                    getASTNode(), getModule(), this);
            attributes = visitor.getAll();
        }
        return attributes;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#getProperties()
     */
    public List<PropertyAdapter> getProperties() {
        if (properties == null) {
            PropertyVisitor visitor = VisitorFactory.createContextVisitor(PropertyVisitor.class, getASTNode(),
                    getModule(), this);
            properties = visitor.getAll();
        }
        return properties;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#getFunctionsInitFiltered()
     */
    public List<FunctionDefAdapter> getFunctionsInitFiltered() {
        List<FunctionDefAdapter> functionsFiltered = new ArrayList<FunctionDefAdapter>();
        for (FunctionDefAdapter adapter : getFunctions()) {
            if (!(adapter.isInit())) {
                functionsFiltered.add(adapter);
            }
        }

        return functionsFiltered;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#hasFunctions()
     */
    public boolean hasFunctions() {
        return getFunctions().size() > 0;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#hasFunctionsInitFiltered()
     */
    public boolean hasFunctionsInitFiltered() {
        return getFunctionsInitFiltered().size() > 0;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#isNested()
     */
    public boolean isNested() {
        return nodeHelper.isFunctionOrClassDef(getParent().getASTNode());
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#hasAttributes()
     */
    public boolean hasAttributes() {
        return getAttributes().size() > 0;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#getNodeBodyIndent()
     */
    @Override
    public String getNodeBodyIndent() {
        ClassDef classNode = getASTNode();
        if (classNode.body == null || classNode.body.length == 0) {
            PySelection pySelection = new PySelection(getModule().getDoc());
            String indentationFromLine = PySelection.getIndentationFromLine(pySelection
                    .getLine(classNode.beginLine - 1));
            return indentationFromLine
                    + DefaultIndentPrefs.get(this.getAdapterPrefs().projectAdaptable).getIndentationString();

        }
        return getModule().getIndentationFromAst(classNode.body[0]);
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#hasInit()
     */
    public boolean hasInit() {
        return (getFirstInit() != null);
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#getFirstInit()
     */
    public FunctionDefAdapter getFirstInit() {
        for (FunctionDefAdapter func : getFunctions()) {
            if (func.isInit()) {
                return func;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#getAssignedVariables()
     */
    @Override
    public List<SimpleAdapter> getAssignedVariables() {
        ScopeAssignedVisitor visitor = VisitorFactory.createContextVisitor(ScopeAssignedVisitor.class, getASTNode(),
                this.getModule(), this);
        return visitor.getAll();
    }

    /* (non-Javadoc)
     * @see org.python.pydev.refactoring.ast.adapters.IClassDefAdapter#isNewStyleClass()
     */
    public boolean isNewStyleClass() {
        for (String base : getBaseClassNames()) {
            if (base.compareTo(OBJECT) == 0) {
                return true;
            }
        }
        return false;
    }
}
