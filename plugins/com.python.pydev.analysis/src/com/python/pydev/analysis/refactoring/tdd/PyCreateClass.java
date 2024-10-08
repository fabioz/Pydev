/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.refactoring.tdd;

import java.util.List;

import org.python.pydev.ast.adapters.ModuleAdapter;
import org.python.pydev.ast.refactoring.RefactoringInfo;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This class should be used to generate code for creating a new class.
 */
public class PyCreateClass extends AbstractPyCreateClassOrMethodOrField {

    private final static String baseClassStr = "" +
            "class %s:\n" +
            "    ${pass}${cursor}\n" +
            "\n" +
            "\n" +
            "";

    private final static String baseClassWithInitStr = "" +
            "class %s:\n" +
            "    \n"
            +
            "    def __init__(self, %s):\n" +
            "        ${pass}${cursor}\n" +
            "\n" +
            "\n" +
            "";

    @Override
    public String getCreationStr() {
        return "class";
    }

    @Override
    public String getDefaultActTok() {
        return "MyClass";
    }

    /**
     * Returns a proposal that can be used to generate the code.
     */
    @Override
    public TemplateInfo createProposal(RefactoringInfo refactoringInfo, String actTok,
            int locationStrategy,
            List<String> parametersAfterCall) {
        PySelection pySelection = refactoringInfo.getPySelection();
        ModuleAdapter moduleAdapter = refactoringInfo.getModuleAdapter();

        String source;
        if (parametersAfterCall == null || parametersAfterCall.size() == 0) {
            source = StringUtils.format(baseClassStr, actTok);
        } else {
            FastStringBuffer params = createParametersList(parametersAfterCall);
            source = StringUtils.format(baseClassWithInitStr, actTok, params);

        }

        Tuple<Integer, String> offsetAndIndent = getLocationOffset(locationStrategy, pySelection, moduleAdapter);

        return createProposal(pySelection, source, offsetAndIndent);
    }

}
