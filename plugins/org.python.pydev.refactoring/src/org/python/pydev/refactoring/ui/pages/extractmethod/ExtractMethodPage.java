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
import org.python.pydev.refactoring.coderefactoring.extractmethod.ExtractMethodRequestProcessor;
import org.python.pydev.refactoring.ui.model.table.SimpleTableItem;
import org.python.pydev.refactoring.ui.pages.PyDevInputWizardPage;

public class ExtractMethodPage extends PyDevInputWizardPage {
    private ExtractMethodRequestProcessor requestProcessor;
    private ExtractMethodComposite extractComposite;
    private Composite parent;

    public ExtractMethodPage(String name, ExtractMethodRequestProcessor requestProcessor) {
        super(name);
        this.setTitle(name);
        this.requestProcessor = requestProcessor;
    }

    public void createControl(Composite parent) {
        this.parent = parent;
        setupComposite();
    }

    public void setupComposite() {
        if(extractComposite != null){
            extractComposite.dispose();
            extractComposite = null;
        }
        boolean hasArguments = this.requestProcessor.getDeducer().getParameters().size() > 0;

        extractComposite = new ExtractMethodComposite(this, parent, hasArguments, requestProcessor.getScopeAdapter());

        extractComposite.registerListeners(this);
        updateArgumentTable();
        setControl(this.extractComposite);

        voodooResizeToPage();
        setPageComplete(false);
    }

    public void updateArgumentTable() {
        if(extractComposite != null && extractComposite.getArgumentsTable() != null){
            extractComposite.initTable(requestProcessor.getDeducer().getParameters());
        }
    }

    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    public void validate() {
        setErrorMessage(null);
        extractComposite.validate();
        setPageComplete(getErrorMessage() == null);
        if(isPageComplete()){
            applySettings();
        }
    }

    private void applySettings() {
        this.requestProcessor.setMethodName(extractComposite.getFunctionName());
        this.requestProcessor.setOffsetStrategy(extractComposite.getOffsetStrategy());

        if(extractComposite.getArgumentsTable() != null){
            List<String> parameterOrder = new ArrayList<String>();
            Map<String, String> parameterMap = new HashMap<String, String>();
            for(TableItem item:extractComposite.getArgumentsTable().getItems()){
                if(item instanceof SimpleTableItem){
                    SimpleTableItem tableItem = (SimpleTableItem) item;
                    parameterMap.put(tableItem.getOriginalName(), tableItem.getText());
                    parameterOrder.add(tableItem.getOriginalName());
                }
            }
            requestProcessor.setParameterMap(parameterMap);
            requestProcessor.setParameterOrder(parameterOrder);
        }
    }

    public void handleEvent(Event event) {
        validate();
    }
}
