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

import org.eclipse.jface.text.rules.IToken;

public interface IFields {

    // Metadata

    public static String FILEPATH = "filepath";

    public static String FILENAME = "filename";

    public static String EXTENSION = "ext";

    public static String MODIFIED_TIME = "mod_time";

    // Content-related

    public static String PYTHON = "python";

    public static String COMMENT = "comment";

    public static String STRING = "string";

    public static String GENERAL_CONTENTS = "contents";

    String getTokenFieldName(IToken nextToken);

}
