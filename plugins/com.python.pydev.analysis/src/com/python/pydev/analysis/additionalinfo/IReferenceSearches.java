package com.python.pydev.analysis.additionalinfo;

import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.shared_core.index.IFields;
import org.python.pydev.shared_core.structure.OrderedMap;

public interface IReferenceSearches {

    void dispose();

    // These are the indexed fields we use.
    public static String FIELD_MODULES_KEY_IO = "modules_key";
    public static String FIELD_MODULE_NAME = "module_name";
    public static String FIELD_MODIFIED_TIME = IFields.MODIFIED_TIME;
    public static String FIELD_CONTENTS = IFields.GENERAL_CONTENTS;

    List<ModulesKey> search(IProject project, OrderedMap<String, Set<String>> fieldNameToValues,
            IProgressMonitor monitor) throws OperationCanceledException;

}
