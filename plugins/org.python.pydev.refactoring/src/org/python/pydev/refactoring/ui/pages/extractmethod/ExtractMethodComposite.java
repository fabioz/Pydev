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

import java.util.List;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.core.model.OffsetStrategyModel;
import org.python.pydev.refactoring.core.model.OffsetStrategyProvider;
import org.python.pydev.refactoring.core.validator.NameValidator;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.refactoring.ui.core.LabeledEdit;
import org.python.pydev.refactoring.ui.pages.core.SimpleTableItem;
import org.python.pydev.refactoring.ui.pages.listener.ButtonActivationListener;
import org.python.pydev.refactoring.ui.pages.listener.FunctionSignatureListener;
import org.python.pydev.refactoring.ui.pages.listener.IValidationPage;
import org.python.pydev.refactoring.ui.pages.listener.TableCellEditorListener;

public class ExtractMethodComposite extends Composite {

    private Label functionPreviewLabel;

    private Label functionSignatureLabel;

    private Label argumentsLabel;

    private Button editArgumentsButton;

    private TableColumn nameColumn;

    private Table argumentsTable;

    private Button downArgumentsButton;

    private Button upArgumentsButton;

    private LabeledEdit functionNameEdit;

    private TableCellEditorListener cellEditorListener;

    private AbstractScopeNode<?> scopeAdapter;

    private IValidationPage page;

    private FunctionSignatureListener signatureListener;

    private OffsetStrategyProvider strategyProvider;

    private ComboViewer methodInsertionComb;

    private CLabel methodInsertionLbl;

    private Composite argumentsComposite;

    public ExtractMethodComposite(IValidationPage page, Composite parent, boolean hasArguments,
            AbstractScopeNode<?> scope) {
        super(parent, SWT.NONE);
        this.page = page;
        this.scopeAdapter = scope;

        this.strategyProvider = new OffsetStrategyProvider(scopeAdapter, IOffsetStrategy.BEFORECURRENT
                | IOffsetStrategy.AFTERINIT | IOffsetStrategy.BEGIN | IOffsetStrategy.END);

        createComposite(hasArguments);
    }

    public void createComposite(boolean hasArguments) {
        setLayout(new GridLayout());

        createFunctionName(this);

        if (hasArguments) {
            createArguments(this);
        }

        createOffsetStrategy(this);

        createFunctionSignature(this);

        pack();
    }

    private void createFunctionSignature(Composite control) {
        Composite functionSignatureComposite = new Composite(control, SWT.NONE);
        GridLayout compositeLayout = new GridLayout();
        compositeLayout.makeColumnsEqualWidth = true;
        GridData compositeLData = new GridData();
        compositeLData.horizontalAlignment = GridData.FILL;
        compositeLData.grabExcessHorizontalSpace = true;
        functionSignatureComposite.setLayoutData(compositeLData);
        functionSignatureComposite.setLayout(compositeLayout);

        functionSignatureLabel = new Label(functionSignatureComposite, SWT.NONE);
        GridData labelLData = new GridData();
        labelLData.horizontalAlignment = GridData.FILL;
        labelLData.grabExcessHorizontalSpace = true;
        functionSignatureLabel.setLayoutData(labelLData);
        functionSignatureLabel.setText(Messages.extractMethodFunctionPreview);

        functionPreviewLabel = new Label(functionSignatureComposite, SWT.NONE);
        GridData functionSignaturePreviewData = new GridData();
        functionSignaturePreviewData.horizontalAlignment = GridData.FILL;
        functionSignaturePreviewData.grabExcessHorizontalSpace = true;
        functionSignaturePreviewData.verticalAlignment = GridData.FILL;
        functionSignaturePreviewData.grabExcessVerticalSpace = true;
        functionPreviewLabel.setLayoutData(functionSignaturePreviewData);
        functionPreviewLabel.setText("");
    }

    private Button createButton(Composite parent, String name) {
        Button button = new Button(parent, SWT.PUSH | SWT.CENTER);
        GridData buttonLData = new GridData();
        buttonLData.horizontalAlignment = GridData.FILL;
        buttonLData.grabExcessHorizontalSpace = true;
        button.setLayoutData(buttonLData);
        button.setText(name);
        return button;
    }

    private void createArguments(Composite control) {

        argumentsComposite = new Composite(control, SWT.NONE);
        GridLayout compositeLayout = new GridLayout();
        compositeLayout.makeColumnsEqualWidth = true;
        GridData compositeLData = new GridData();
        compositeLData.grabExcessHorizontalSpace = true;
        compositeLData.horizontalAlignment = GridData.FILL;
        compositeLData.grabExcessVerticalSpace = true;
        compositeLData.verticalAlignment = GridData.FILL;
        argumentsComposite.setLayoutData(compositeLData);
        argumentsComposite.setLayout(compositeLayout);

        createArgumentsLabel(argumentsComposite);
        createArgumentsTable(argumentsComposite);
    }

    private void createArgumentsButton(Composite argumentsTableComposite) {
        Composite argumentsButtonComposite = new Composite(argumentsTableComposite, SWT.NONE);
        GridLayout compositeLayout = new GridLayout();
        compositeLayout.makeColumnsEqualWidth = true;
        FormData compositeLData = new FormData(0, 0);
        compositeLData.width = 80;
        compositeLData.bottom = new FormAttachment(1000, 1000, 0);
        compositeLData.right = new FormAttachment(1000, 1000, 0);
        compositeLData.top = new FormAttachment(0, 1000, 0);
        argumentsButtonComposite.setLayoutData(compositeLData);
        argumentsButtonComposite.setLayout(compositeLayout);

        editArgumentsButton = createButton(argumentsButtonComposite, Messages.extractMethodEditButton);
        editArgumentsButton.setEnabled(false);

        upArgumentsButton = createButton(argumentsButtonComposite, Messages.extractMethodUpButton);
        upArgumentsButton.setEnabled(false);

        downArgumentsButton = createButton(argumentsButtonComposite, Messages.extractMethodDownButton);
        downArgumentsButton.setEnabled(false);

    }

    private void registerDownButtonListener() {
        downArgumentsButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = argumentsTable.getSelectionIndex();
                if (argumentsTable.getSelectionCount() == 1 && selectionIndex != argumentsTable.getItemCount()) {

                    TableItem item = argumentsTable.getSelection()[0];
                    if (item instanceof SimpleTableItem) {
                        SimpleTableItem tableItem = (SimpleTableItem) item;

                        new SimpleTableItem(argumentsTable, tableItem.getOriginalName(), tableItem.getText(),
                                selectionIndex + 2);
                        argumentsTable.remove(selectionIndex);
                        argumentsTable.setSelection(selectionIndex + 1);
                        argumentsTable.notifyListeners(SWT.Selection, new Event());
                        page.validate();
                    }

                }
            }

        });
    }

    private void registerUpButtonListener() {
        upArgumentsButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = argumentsTable.getSelectionIndex();
                if (argumentsTable.getSelectionCount() == 1 && selectionIndex > 0) {

                    TableItem item = argumentsTable.getSelection()[0];
                    if (item instanceof SimpleTableItem) {
                        SimpleTableItem tableItem = (SimpleTableItem) item;

                        new SimpleTableItem(argumentsTable, tableItem.getOriginalName(), tableItem.getText(),
                                selectionIndex - 1);
                        argumentsTable.remove(selectionIndex + 1);
                        argumentsTable.setSelection(selectionIndex - 1);
                        argumentsTable.notifyListeners(SWT.Selection, new Event());
                        page.validate();
                    }

                }
            }

        });
    }

    private Composite createArgumentsTable(Composite parent) {
        final Composite argumentsComposite = new Composite(parent, SWT.NONE);
        FormLayout compositeLayout = new FormLayout();
        GridData compositeLData = new GridData(GridData.FILL_BOTH);

        argumentsComposite.setLayoutData(compositeLData);
        argumentsComposite.setLayout(compositeLayout);

        argumentsTable = new Table(argumentsComposite, SWT.BORDER | SWT.FULL_SELECTION);

        FormData tableLData = new FormData();
        tableLData.bottom = new FormAttachment(1000, 1000, 0);
        tableLData.left = new FormAttachment(0, 1000, 0);
        tableLData.right = new FormAttachment(1000, 1000, -80);
        tableLData.top = new FormAttachment(0, 1000, 4);
        argumentsTable.setLayoutData(tableLData);

        argumentsTable.setHeaderVisible(true);
        argumentsTable.setLinesVisible(true);

        nameColumn = new TableColumn(argumentsTable, SWT.NONE);
        nameColumn.setText(Messages.extractMethodArgumentName);

        createArgumentsButton(argumentsComposite);
        argumentsComposite.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle area = argumentsTable.getClientArea();
                Point preferredSize = argumentsTable.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                int width = area.width - 2 * argumentsTable.getBorderWidth();
                if (preferredSize.y > area.height + argumentsTable.getHeaderHeight()) {
                    Point vBarSize = argumentsTable.getVerticalBar().getSize();
                    width -= vBarSize.x;
                }
                Point oldSize = argumentsTable.getSize();
                if (oldSize.x > area.width) {
                    nameColumn.setWidth(width);
                    argumentsTable.setSize(area.width, area.height);
                } else {
                    argumentsTable.setSize(area.width, area.height);
                    nameColumn.setWidth(width);
                }
            }
        });
        argumentsComposite.notifyListeners(SWT.CONTROL, new Event());

        return argumentsComposite;
    }

    private void createArgumentsLabel(Composite argumentsComposite) {
        argumentsLabel = new Label(argumentsComposite, SWT.NONE);
        GridData labelLData = new GridData();
        labelLData.grabExcessHorizontalSpace = true;
        labelLData.horizontalAlignment = GridData.FILL;
        argumentsLabel.setLayoutData(labelLData);
        argumentsLabel.setText(Messages.extractMethodArgumentsTitle);
    }

    private void createFunctionName(Composite control) {
        Composite methodNameComposite = new Composite(control, SWT.NONE);
        FillLayout compositeLayout = new FillLayout(SWT.HORIZONTAL);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        methodNameComposite.setLayoutData(gridData);
        methodNameComposite.setLayout(compositeLayout);
        functionNameEdit = new LabeledEdit(methodNameComposite, Messages.extractMethodFunctionTitle);
    }

    private void createOffsetStrategy(Composite mainComp) {
        FillLayout fillLayout = new FillLayout();
        fillLayout.type = org.eclipse.swt.SWT.VERTICAL;
        GridData gridData7 = new GridData();
        gridData7.horizontalSpan = 2;
        gridData7.verticalAlignment = GridData.CENTER;
        gridData7.grabExcessHorizontalSpace = true;
        gridData7.horizontalAlignment = GridData.FILL;
        Composite comboComp = new Composite(mainComp, SWT.NONE);
        comboComp.setLayoutData(gridData7);
        comboComp.setLayout(fillLayout);
        methodInsertionLbl = new CLabel(comboComp, SWT.NONE);
        methodInsertionLbl.setText(Messages.offsetStrategyInsertionPointMethod);
        methodInsertionComb = createComboViewer(comboComp);

        methodInsertionComb.getCombo().select(0);

    }

    private ComboViewer createComboViewer(Composite comboComp) {
        ComboViewer v = new ComboViewer(comboComp);
        v.setContentProvider(this.strategyProvider);
        v.setLabelProvider(new LabelProvider());
        v.setInput("");
        return v;
    }

    public LabeledEdit getFunctionNameEdit() {
        return this.functionNameEdit;
    }

    public Label getSignaturePreview() {
        return this.functionPreviewLabel;

    }

    public Table getArgumentsTable() {
        return this.argumentsTable;
    }

    public Button getUpButton() {
        return this.upArgumentsButton;
    }

    public Button getDownButton() {
        return this.downArgumentsButton;
    }

    public Button getEditButton() {
        return this.editArgumentsButton;
    }

    public void registerListeners(final IValidationPage page) {

        signatureListener = new FunctionSignatureListener(page, getSignaturePreview(), getFunctionNameEdit(),
                getArgumentsTable());

        functionNameEdit.getEdit().addListener(SWT.Modify, page);
        functionNameEdit.getEdit().addListener(SWT.Modify, signatureListener);

        ButtonActivationListener buttonActivationListener = new ButtonActivationListener(getArgumentsTable(),
                getUpButton(), getDownButton(), getEditButton());

        if (argumentsTable != null) {
            cellEditorListener = new TableCellEditorListener(page, argumentsTable);

            argumentsTable.addListener(SWT.MouseDoubleClick, cellEditorListener);
            argumentsTable.addListener(SWT.DefaultSelection, cellEditorListener);

            argumentsTable.addListener(SWT.Selection, buttonActivationListener);
            argumentsTable.addListener(SWT.Selection, signatureListener);

            editArgumentsButton.addListener(SWT.Selection, cellEditorListener);
            registerUpButtonListener();
            registerDownButtonListener();
        }

        methodInsertionComb.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                if (!sel.isEmpty()) {
                    page.validate();
                }
            }
        });

    }

    public void initTable(List<String> arguments) {
        for (String argument : arguments) {
            new SimpleTableItem(getArgumentsTable(), argument);
        }
    }

    public Composite getArgumentsComposite() {
        return argumentsComposite;
    }

    public String getFunctionName() {
        return getFunctionNameEdit().getEdit().getText();
    }

    public int getOffsetStrategy() {
        IStructuredSelection sel = (IStructuredSelection) methodInsertionComb.getSelection();

        if (!sel.isEmpty()) {
            OffsetStrategyModel elem = (OffsetStrategyModel) sel.getFirstElement();
            return elem.getStrategy();
        }
        return strategyProvider.get(0).getStrategy();
    }

    public boolean validate() {
        if (argumentsTable != null) {
            VariableCellValidator cellValidator = new VariableCellValidator(this.page, getArgumentsTable(),
                    scopeAdapter);
            cellValidator.validate();
        }

        RefactoringStatus status = new RefactoringStatus();
        NameValidator nameValidator = new NameValidator(status, scopeAdapter);
        nameValidator.validateMethodName(getFunctionName());
        nameValidator.validateUniqueFunction(getFunctionName());

        if (status.hasError()) {
            page.setErrorMessage(status.getMessageMatchingSeverity(RefactoringStatus.WARNING));
        }

        return !status.hasError();
    }
}
