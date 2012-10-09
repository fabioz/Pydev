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

    private static final String[][] DJANGO_TEMPLATES_PAIRS = new String[][] { { "{%", "%}" }, { "{{", "}}" } };

    /**
     * 
     */
    private DjPartitionerSwitchStrategy() {
        super(DJANGO_TEMPLATES_PAIRS);
    }

    public static DjPartitionerSwitchStrategy getDefault() {
        if (instance == null) {
            instance = new DjPartitionerSwitchStrategy();
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
