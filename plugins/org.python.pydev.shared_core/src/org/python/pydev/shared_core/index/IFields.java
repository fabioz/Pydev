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
