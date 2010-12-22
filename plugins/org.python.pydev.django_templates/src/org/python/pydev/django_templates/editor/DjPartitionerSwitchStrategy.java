/**
 * Copyright (c) 2010 Aptana, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
