package org.python.pydev.ui.pythonpathconf.package_manager;

import java.io.File;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;

public abstract class AbstractPackageManager {

    protected IInterpreterInfo interpreterInfo;

    public AbstractPackageManager(IInterpreterInfo interpreterInfo) {
        this.interpreterInfo = interpreterInfo;
    }

    public static AbstractPackageManager createPackageManager(InterpreterInfo interpreterInfo) {
        File condaPrefix = interpreterInfo.getCondaPrefix();
        if (condaPrefix != null) {
            return new CondaPackageManager(interpreterInfo, condaPrefix);
        }

        return new PipPackageManager(interpreterInfo);
    }

    public abstract List<String[]> list();

    public List<String[]> errorToList(List<String[]> listed, UnableToFindExecutableException e) {
        String message = e.getMessage();
        String wrap = WrapAndCaseUtils.wrap(message, 80);
        for (String s : StringUtils.splitInLines(wrap)) {
            listed.add(new String[] { s, "", "" });
        }
        return listed;
    }

    public void updateTree(Tree tree, List<String[]> listed) {
        TreeColumn column = tree.getColumn(0);
        column.setText("Library (" + getPackageManagerName() + " | " + listed.size() + " found)");
        for (String[] s : listed) {
            TreeItem item = new TreeItem(tree, SWT.None);
            item.setText(s);
        }
    }

    protected abstract String getPackageManagerName();

    public abstract void manage();

}
