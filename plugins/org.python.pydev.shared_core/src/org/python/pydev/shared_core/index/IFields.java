/******************************************************************************
* Copyright (C) 2015  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.index;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.rules.IToken;

class StaticInit {

    static Set<String> createFieldsNegated() {
        Set<String> set = new HashSet<String>();
        set.add(IFields.FILEPATH);
        set.add(IFields.MODULE_PATH);
        set.add(IFields.FILENAME);
        set.add(IFields.EXTENSION);
        set.add(IFields.MODIFIED_TIME);
        return set;
    }

};

public interface IFields {

    // Metadata

    public static String FILEPATH = "filepath";

    public static String MODULE_PATH = "module_path";

    public static String FILENAME = "filename";

    public static String EXTENSION = "ext";

    public static String MODIFIED_TIME = "mod_time";

    // Content-related

    public static String PYTHON = "python";

    public static String COMMENT = "comment";

    public static String STRING = "string";

    public static String GENERAL_CONTENTS = "contents";

    String getTokenFieldName(IToken nextToken);

    public static Set<String> FIELDS_NEGATED_WITH_EXCLAMATION = StaticInit.createFieldsNegated();

}
