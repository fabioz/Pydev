/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

import java.util.List;

import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.ast.ClassDef;

public interface IClassDefAdapter extends INodeAdapter, IASTNodeAdapter<ClassDef> {
    List<String> getBaseClassNames();

    List<IClassDefAdapter> getBaseClasses() throws MisconfigurationException;

    List<SimpleAdapter> getAttributes();

    List<PropertyAdapter> getProperties();

    FunctionDefAdapter getFirstInit();

    List<FunctionDefAdapter> getFunctionsInitFiltered();

    List<FunctionDefAdapter> getFunctions();

    List<SimpleAdapter> getAssignedVariables();

    String getNodeBodyIndent();

    boolean hasBaseClass();

    boolean hasFunctions();

    boolean hasFunctionsInitFiltered();

    boolean isNested();

    boolean hasAttributes();

    boolean hasInit();

    boolean isNewStyleClass();
}
