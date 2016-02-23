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

package org.python.pydev.refactoring.ui.pages;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.codegenerator.constructorfield.ConstructorFieldRefactoring;
import org.python.pydev.refactoring.codegenerator.constructorfield.ConstructorFieldRequestProcessor;
import org.python.pydev.refactoring.core.model.OffsetStrategyModel;
import org.python.pydev.refactoring.core.model.OffsetStrategyProvider;
import org.python.pydev.refactoring.core.model.constructorfield.ClassFieldTreeProvider;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.refactoring.ui.core.TreeLabelProvider;

public class ConstructorFieldPage extends UserInputWizardPage {
    public static final String PAGE_NAME = "ConstructorFieldPage"; //$NON-NLS-1$

    private final OffsetStrategyProvider strategyProvider;
    private Composite mainComp = null;
    private Composite buttonComp = null;
    private Button selectAll = null;
    private Button deselectAll = null;
    private CLabel cLabel = null;
    private Composite treeComp = null;
    private Composite comboComp = null;
    private ComboViewer methodInsertionComb = null;
    private CLabel methodInsertionLbl = null;
    private ContainerCheckedTreeViewer treeViewer = null;
    private ClassFieldTreeProvider classProvider;
    private ILabelProvider labelProvider;

    public ConstructorFieldPage(ClassFieldTreeProvider provider) {
        super(PAGE_NAME);
        this.classProvider = provider;
        this.labelProvider = new TreeLabelProvider();

        this.strategyProvider = new OffsetStrategyProvider(IOffsetStrategy.AFTERINIT | IOffsetStrategy.BEGIN
                | IOffsetStrategy.END);
    }

    private Composite createMainComp(Composite parent) {
        GridData gridData12 = new GridData();
        gridData12.horizontalSpan = 2;
        GridData gridData11 = new GridData();
        gridData11.horizontalSpan = 2;
        GridLayout gridLayout2 = new GridLayout();
        gridLayout2.numColumns = 2;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.verticalAlignment = GridData.FILL;
        mainComp = new Composite(parent, SWT.NONE);
        mainComp.setLayoutData(gridData);
        cLabel = new CLabel(mainComp, SWT.NONE);
        cLabel.setText(Messages.constructorFieldSelect);
        cLabel.setLayoutData(gridData11);
        createTreeComp();
        createButtonComp();
        mainComp.setLayout(gridLayout2);

        createComboComp();

        return mainComp;
    }

    private void createButtonComp() {
        GridData gridData3 = new GridData();
        gridData3.horizontalAlignment = GridData.CENTER;
        gridData3.verticalAlignment = GridData.BEGINNING;
        GridData gridData2 = new GridData();
        gridData2.widthHint = 80;
        GridData gridData1 = new GridData();
        gridData1.widthHint = 80;
        buttonComp = new Composite(mainComp, SWT.NONE);
        buttonComp.setLayout(new GridLayout());
        buttonComp.setLayoutData(gridData3);
        selectAll = new Button(buttonComp, SWT.NONE);
        selectAll.setText(Messages.wizardSelectAll);
        selectAll.setLayoutData(gridData2);
        selectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                treeViewer.setAllChecked(true);
                getRequestProcessor().setCheckedElements(treeViewer.getCheckedElements());
                ConstructorFieldPage.this.getWizard().getContainer().updateButtons();
            }
        });
        deselectAll = new Button(buttonComp, SWT.NONE);
        deselectAll.setText(Messages.wizardDeselectAll);
        deselectAll.setLayoutData(gridData1);
        deselectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                treeViewer.setAllChecked(false);
                getRequestProcessor().setCheckedElements(treeViewer.getCheckedElements());
                ConstructorFieldPage.this.getWizard().getContainer().updateButtons();
            }
        });
    }

    private void createTreeComp() {
        GridData gridData5 = new GridData();
        gridData5.horizontalAlignment = GridData.FILL;
        gridData5.grabExcessHorizontalSpace = true;
        gridData5.grabExcessVerticalSpace = true;
        gridData5.verticalAlignment = GridData.FILL;
        treeComp = new Composite(mainComp, SWT.NONE);
        treeComp.setLayout(new FillLayout());
        treeComp.setLayoutData(gridData5);
        createTreeViewer(treeComp);
    }

    private void createComboComp() {
        FillLayout fillLayout = new FillLayout();
        fillLayout.type = org.eclipse.swt.SWT.VERTICAL;
        GridData gridData7 = new GridData();
        gridData7.horizontalSpan = 2;
        gridData7.verticalAlignment = GridData.CENTER;
        gridData7.grabExcessHorizontalSpace = true;
        gridData7.horizontalAlignment = GridData.FILL;
        comboComp = new Composite(mainComp, SWT.NONE);
        comboComp.setLayoutData(gridData7);
        comboComp.setLayout(fillLayout);
        methodInsertionLbl = new CLabel(comboComp, SWT.NONE);
        methodInsertionLbl.setText(Messages.offsetStrategyInsertionPointMethod);
        methodInsertionComb = createComboViewer(comboComp);
        methodInsertionComb.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                if (!sel.isEmpty()) {
                    OffsetStrategyModel elem = (OffsetStrategyModel) sel.getFirstElement();
                    getRequestProcessor().setMethodDestination(elem.getStrategy());
                }
            }
        });

        getRequestProcessor().setMethodDestination(strategyProvider.get(0).getStrategy());
        methodInsertionComb.getCombo().select(0);
    }

    private void createTreeViewer(Composite treeComp) {
        treeViewer = new ContainerCheckedTreeViewer(treeComp);
        treeViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                getRequestProcessor().setCheckedElements(treeViewer.getCheckedElements());
                ConstructorFieldPage.this.getWizard().getContainer().updateButtons();
            }
        });

        treeViewer.setContentProvider(classProvider);
        treeViewer.setLabelProvider(labelProvider);
        treeViewer.setAutoExpandLevel(2);
        treeViewer.setInput("");
        treeViewer.setSelection(new StructuredSelection(treeViewer.getExpandedElements()[0]));
    }

    @Override
    public void createControl(Composite composite) {
        Composite main = createMainComp(composite);
        main.pack();
        this.setControl(main);
    }

    private ComboViewer createComboViewer(Composite comboComp) {
        ComboViewer v = new ComboViewer(comboComp);
        v.setContentProvider(this.strategyProvider);
        v.setLabelProvider(new LabelProvider());
        v.setInput("");
        return v;
    }

    private ConstructorFieldRequestProcessor getRequestProcessor() {
        return getConstructorFieldRefactoring().getRequestProcesor();
    }

    public ConstructorFieldRefactoring getConstructorFieldRefactoring() {
        return (ConstructorFieldRefactoring) getRefactoring();
    }

    @Override
    public boolean canFlipToNextPage() {
        return (treeViewer.getCheckedElements().length > 0);
    }
}
