package org.python.pydev.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.dialogs.MapOfStringsInputDialog;
import org.python.pydev.ui.editors.TreeWithAddRemove;

/**
 * This class creates a tab to show the variables in the passed tab folder.
 */
public class TabVariables {

    private TreeWithAddRemove treeVariables;
	private TabFolder tabFolder;
	
	public TabVariables(TabFolder tabFolder, Map<String, String> initialVariables) {
		this.tabFolder = tabFolder;
		createTabVariables(initialVariables);
	}

	private void createTabVariables(Map<String, String> initialVariables){
        if(initialVariables == null){
            initialVariables = new HashMap<String, String>();
        }
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("String Substitution Variables");
        tabItem.setImage(PydevPlugin.getImageCache().get(UIConstants.VARIABLE_ICON));
        Composite topComp = new Composite(tabFolder, SWT.None);
        topComp.setLayout(new GridLayout(1, false));
        
        
        GridData gd;
        GridData data;
        Label l2;
        l2 = new Label(topComp, SWT.None);
        l2.setText("String substitution variables are used to resolve:\n" +
        		"  - source folders\n" +
        		"  - external libraries\n" +
        		"  - main module in launch configuration"
        );
        
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        l2.setLayoutData(gd);
        
        final Map<String, String> vars = initialVariables; 
        
        treeVariables = new TreeWithAddRemove(topComp, 0, vars) {

            @Override
            protected String getImageConstant() {
                return UIConstants.VARIABLE_ICON;
            }
            
            @Override
            protected void handleAddButtonSelected(int nButton){
                if(nButton == 0){
                    addItemWithDialog(new MapOfStringsInputDialog(getShell(), "Variable", "Enter the variable name/value.", vars));
                    
                }else{
                    throw new AssertionError("Unexpected (only 0 should be available)");
                }
            }

            
            @Override
            protected String getButtonLabel(int i) {
                if(i != 0){
                    throw new RuntimeException("Expected only i==0. Received: "+i);
                }
                return "Add variable";
            }
            
            
            @Override
            protected int getNumberOfAddButtons(){
                return 1;
            }
        };
        
        data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        treeVariables.setLayoutData(data);
        
        tabItem.setControl(topComp);
    }

	public Map<String, String> getTreeItemsAsMap() {
		return this.treeVariables.getTreeItemsAsMap();
	}
    
}
