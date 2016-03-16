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

package org.python.pydev.refactoring.ui.pages.extractmethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.python.pydev.refactoring.coderefactoring.extractmethod.ExtractMethodRefactoring;
import org.python.pydev.refactoring.coderefactoring.extractmethod.ExtractMethodRequestProcessor;
import org.python.pydev.refactoring.ui.pages.PyDevInputWizardPage;
import org.python.pydev.refactoring.ui.pages.core.SimpleTableItem;

public class ExtractMethodPage extends PyDevInputWizardPage {
    private static final String PAGE_NAME = "ExtractMethodPage";
    private ExtractMethodComposite extractComposite;
    private Composite parent;

    public ExtractMethodPage() {
        super(PAGE_NAME);
    }

    @Override
    public void createControl(Composite parent) {
        this.parent = parent;
        setupComposite();
    }

    public void setupComposite() {
        if (extractComposite != null) {
            extractComposite.dispose();
            extractComposite = null;
        }
        boolean hasArguments = getRequestProcessor().getDeducer().getParameters().size() > 0;

        extractComposite = new ExtractMethodComposite(this, parent, hasArguments, getRequestProcessor()
                .getScopeAdapter());

        extractComposite.registerListeners(this);
        updateArgumentTable();
        setControl(this.extractComposite);

        voodooResizeToPage();
        setPageComplete(false);
    }

    public void updateArgumentTable() {
        if (extractComposite != null && extractComposite.getArgumentsTable() != null) {
            extractComposite.initTable(getRequestProcessor().getDeducer().getParameters());
        }
    }

    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    @Override
    public void validate() {
        setErrorMessage(null);
        extractComposite.validate();
        setPageComplete(getErrorMessage() == null);
        if (isPageComplete()) {
            applySettings();
        }
    }

    private void applySettings() {
        this.getRequestProcessor().setMethodName(extractComposite.getFunctionName());
        this.getRequestProcessor().setOffsetStrategy(extractComposite.getOffsetStrategy());

        if (extractComposite.getArgumentsTable() != null) {
            List<String> parameterOrder = new ArrayList<String>();
            Map<String, String> parameterMap = new HashMap<String, String>();
            for (TableItem item : extractComposite.getArgumentsTable().getItems()) {
                if (item instanceof SimpleTableItem) {
                    SimpleTableItem tableItem = (SimpleTableItem) item;
                    parameterMap.put(tableItem.getOriginalName(), tableItem.getText());
                    parameterOrder.add(tableItem.getOriginalName());
                }
            }
            getRequestProcessor().setParameterMap(parameterMap);
            getRequestProcessor().setParameterOrder(parameterOrder);
        }
    }

    private ExtractMethodRequestProcessor getRequestProcessor() {
        return getExtractMethodRefactoring().getRequestProcessor();
    }

    private ExtractMethodRefactoring getExtractMethodRefactoring() {
        return (ExtractMethodRefactoring) getRefactoring();
    }

    @Override
    public void handleEvent(Event event) {
        validate();
    }
}
