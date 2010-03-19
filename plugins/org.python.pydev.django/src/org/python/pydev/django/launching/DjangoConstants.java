package org.python.pydev.django.launching;

public class DjangoConstants {

	/**
	 * Points to the path inside of the project of the manage.py
	 * E.g.: in a project 'django_test', if it's located in a folder src/foo/manage.py
	 * the variable would be src/foo/manage.py
	 */
	public static final String DJANGO_MANAGE_VARIABLE = "DJANGO_MANAGE_LOCATION";
	
	public static final String DJANGO_LAUNCH_CONFIGURATION_TYPE = 
		"org.python.pydev.django.launching.DjangoLaunchConfigurationType";

}
