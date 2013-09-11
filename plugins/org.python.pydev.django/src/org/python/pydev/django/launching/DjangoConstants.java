/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.launching;

public class DjangoConstants {

    /**
     * Points to the path inside of the project of the manage.py
     * E.g.: in a project 'django_test', if it's located in a folder src/foo/manage.py
     * the variable would be src/foo/manage.py
     */
    public static final String DJANGO_MANAGE_VARIABLE = "DJANGO_MANAGE_LOCATION";

    /**
     * This is the name of the module to be imported for the settings. E.g.: my_project.settings
     */
    public static final String DJANGO_SETTINGS_MODULE = "DJANGO_SETTINGS_MODULE";

    public static final String DJANGO_LAUNCH_CONFIGURATION_TYPE = "org.python.pydev.django.launching.DjangoLaunchConfigurationType";

}
