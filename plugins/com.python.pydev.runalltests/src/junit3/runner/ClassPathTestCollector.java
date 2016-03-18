/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial API and implementation
*     Andrew Ferrazzutti <aferrazz@redhat.com> - ongoing maintenance
******************************************************************************/
package junit3.runner;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * An implementation of a TestCollector that consults the
 * class path. It considers all classes on the class path
 * excluding classes in JARs. It leaves it up to subclasses
 * to decide whether a class is a runnable Test.
 *
 * @see TestCollector
 */
public abstract class ClassPathTestCollector implements TestCollector {
	
	static final int SUFFIX_LENGTH= ".class".length();
	
	public ClassPathTestCollector() {
	}
	
	@Override
    public Enumeration<String> collectTests() {
		String classPath= System.getProperty("java.class.path");
		Hashtable<String, String> result = collectFilesInPath(classPath);
		return result.elements();
	}

	public Hashtable<String, String> collectFilesInPath(String classPath) {
		Hashtable<String, String> result= collectFilesInRoots(splitClassPath(classPath));
		return result;
	}
	
	Hashtable<String, String> collectFilesInRoots(Vector roots) {
		Hashtable<String, String> result= new Hashtable(100);
		Enumeration<String> e= roots.elements();
		while (e.hasMoreElements()) 
			gatherFiles(new File((String)e.nextElement()), "", result);
		return result;
	}

	void gatherFiles(File classRoot, String classFileName, Hashtable<String, String> result) {
		File thisRoot= new File(classRoot, classFileName);
		if (thisRoot.isFile()) {
			if (isTestClass(classFileName)) {
				String className= classNameFromFile(classFileName);
				result.put(className, className);
			}
			return;
		}		
		String[] contents= thisRoot.list();
		if (contents != null) { 
			for (int i= 0; i < contents.length; i++) 
				gatherFiles(classRoot, classFileName+File.separatorChar+contents[i], result);		
		}
	}
	
	Vector<String> splitClassPath(String classPath) {
		Vector<String> result= new Vector<String>();
		String separator= System.getProperty("path.separator");
		StringTokenizer tokenizer= new StringTokenizer(classPath, separator);
		while (tokenizer.hasMoreTokens()) 
			result.addElement(tokenizer.nextToken());
		return result;
	}
	
	protected boolean isTestClass(String classFileName) {
		return 
			classFileName.endsWith(".class") && 
			classFileName.indexOf('$') < 0 &&
			classFileName.indexOf("Test") > 0;
	}
	
	protected String classNameFromFile(String classFileName) {
		// convert /a/b.class to a.b
		String s= classFileName.substring(0, classFileName.length()-SUFFIX_LENGTH);
		String s2= s.replace(File.separatorChar, '.');
		if (s2.startsWith("."))
			return s2.substring(1);
		return s2;
	}	
}
