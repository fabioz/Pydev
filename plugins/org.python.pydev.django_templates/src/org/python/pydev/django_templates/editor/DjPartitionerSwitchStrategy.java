/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.django_templates.editor;

import com.aptana.editor.common.PartitionerSwitchStrategy;

/**
 * @author Fabio Zadrozny
 */
public class DjPartitionerSwitchStrategy extends PartitionerSwitchStrategy {

    private static DjPartitionerSwitchStrategy instance;

    private static final String[][] DJANGO_TEMPLATES_PAIRS = new String[][] { 
        { "{%", "%}" }, 
        { "{{", "}}" }
    };
    
    private static final String[][] DJANGO_ESCAPE_PAIRS = new String[][] { 
        { "{%", "%}" }, 
        { "{{", "}}" }
    };

    // /**
    //  * 
    //  */
    // private DjPartitionerSwitchStrategy() {
    //     super(DJANGO_TEMPLATES_PAIRS);
    // }
    
    // FIXME: 2011-01-11: the above seems to throw an error while building 
    //
    // I suspect this is an API discrepancy between the last released Aptana Studio 3 Beta (which I am building against) 
    // and the upcoming one (which I don't have, yet). Temporary solution: replace with auto-generated stub.
    protected DjPartitionerSwitchStrategy(String[][] switchSequencePairs, String[][] escapeSequencePairs) {
        super(switchSequencePairs, escapeSequencePairs);
    }

    public static DjPartitionerSwitchStrategy getDefault() {
        if (instance == null) {
            instance = new DjPartitionerSwitchStrategy(DJANGO_TEMPLATES_PAIRS, DJANGO_ESCAPE_PAIRS);
        }
        return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aptana.editor.common.IPartitionerSwitchStrategy#getSwitchTagPairs()
     */
    public String[][] getSwitchTagPairs() {
        return DJANGO_TEMPLATES_PAIRS;
    }

}
