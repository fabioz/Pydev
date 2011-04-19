package org.python.pydev.debug.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.python.pydev.core.docutils.StringUtils;

/**
 * 
 * @author hussain.bohra
 * 
 *         This files contains utility method for File read / write / append
 */
public class FileUtils {
	private static String DELIMITER = File.pathSeparator;

	private FileUtils() {

	}

	/**
	 * Creates a new file if isAppend is false, else appends data in the
	 * existing file.
	 * 
	 * @param fileName
	 * @param pyExceptionsStr
	 * @param isAppend
	 */
	public static void writeExceptionsToFile(String fileName,
			String pyExceptionsStr, boolean isAppend) {
		IPath path = getFilePathFromWorkSpace(fileName);
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
	 * 
	 * @param fileName
	 * @return
	 */
	public static String readExceptionsFromFile(String fileName) {
		StringBuilder builder = new StringBuilder();
		try {
			if (isFileExists(getFilePathFromWorkSpace(fileName).toString())) {
				BufferedReader bReader = new BufferedReader(new FileReader(
						getFilePathFromWorkSpace(fileName).toFile()));
				String exceptionString;
				while ((exceptionString = bReader.readLine()) != null) {
					builder.append(exceptionString);
				}
				bReader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return builder.toString();
	}

	/**
	 * Split the string receives from the file read by filePathSeperator and
	 * returns the list
	 * 
	 * @param fileName
	 * @return
	 */
	public static List getConfiguredExceptions(String fileName) {
		String[] currentElements = {};
		String pyExceptionStr = readExceptionsFromFile(fileName);
		if (pyExceptionStr.length() > 0) {
			currentElements = pyExceptionStr.split(
					"\\" + DELIMITER);
		}
		return Arrays.asList(currentElements);
	}

	/**
	 * Join the received array by the filePathSeperator and calls write to file
	 * with isAppend false
	 * 
	 * @param exceptionArray
	 */
	public static void saveConfiguredExceptions(String[] exceptionArray) {
		String pyExceptionsStr = StringUtils.join(DELIMITER, exceptionArray);
		writeExceptionsToFile(Constants.EXCEPTION_FILE_NAME, pyExceptionsStr,
				false);
	}

	/**
	 * * Check whether the file exists at given path
	 * 
	 * @param filePath
	 * @return
	 */
	public static boolean isFileExists(String filePath) {
		File file = new File(filePath);
		return file.exists();
	}

	/**
	 * Construct the file path existing in the workspace under
	 * <workspace>/.metadata/plugins/org.python.pydev
	 * 
	 * @param fileName
	 * @return
	 */
	public static IPath getFilePathFromWorkSpace(String fileName) {
		Bundle bundle = Platform.getBundle("org.python.pydev");
		IPath path = Platform.getStateLocation(bundle);
		path = path.addTrailingSeparator().append(fileName);
		return path;
	}
}
