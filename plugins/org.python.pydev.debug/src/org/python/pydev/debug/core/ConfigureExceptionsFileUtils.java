/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;

/**
 * This file contains utility methods for File read / write / append
 * 
 * @author hussain.bohra
 * @author Fabio Zadrozny
 */
public class ConfigureExceptionsFileUtils {
    
	public static String DELIMITER = ";";

	/**
	 * Only static methods (no need to instance it).
	 */
	private ConfigureExceptionsFileUtils() {

	}

	/**
	 * Creates a new file if isAppend is false, else appends data in the
	 * existing file.
	 */
	public static void writeToFile(String fileName, String pyExceptionsStr, boolean isAppend) {
		IPath path = getFilePathFromMetadata(fileName);
		try {
			FileWriter fstream = new FileWriter(path.toFile(), isAppend);
			BufferedWriter bufferedWriter = new BufferedWriter(fstream);
			bufferedWriter.write(pyExceptionsStr);
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read the data from the file and returns in the string format
	 */
	public static String readFromMetadataFile(String fileName) {
		IPath filePathFromWorkSpace = getFilePathFromMetadata(fileName);
		File file = filePathFromWorkSpace.toFile();
		
        if (file.exists()) {
            return REF.getFileContents(file);
		}

		return "";
	}

	/**
	 * Split the string received from the file read by the delimiter and
	 * returns the list
	 */
	public static List<String> getConfiguredExceptions(String fileName) {
		String pyExceptionStr = readFromMetadataFile(fileName);
		if (pyExceptionStr.length() > 0) {
			return StringUtils.split(pyExceptionStr, DELIMITER);
		}
		return new ArrayList<String>();
	}


	/**
	 * Construct the file path existing in the workspace under
	 * <workspace>/.metadata/plugins/org.python.pydev
	 */
	public static IPath getFilePathFromMetadata(String fileName) {
		Bundle bundle = Platform.getBundle("org.python.pydev");
		IPath path = Platform.getStateLocation(bundle);
		path = path.addTrailingSeparator().append(fileName);
		return path;
	}
}
