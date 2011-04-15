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
import org.python.pydev.core.docutils.StringUtils;

public class FileUtils {
	private static String DELIMITER = File.pathSeparator;
	private static String SEPERATOR = File.separator;

	private FileUtils() {

	}

	public static void writeExceptionsToFile(String[] exceptionArray) {
		String pyExceptionsToBreak = StringUtils
				.join(DELIMITER, exceptionArray);
		IPath path = PydevDebugPlugin.getWorkspace().getRoot().getLocation();
		String filePath = path.toString() + SEPERATOR + Constants.FILE_PATH;
		String fileName = filePath + SEPERATOR + Constants.FILE_NAME;
		try {
			FileWriter fstream = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(fstream);
			bufferedWriter.write(pyExceptionsToBreak);
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String readExceptionsFromFile() {
		StringBuilder builder = new StringBuilder();
		IPath path = PydevDebugPlugin.getWorkspace().getRoot().getLocation();
		String filePath = path.toString() + SEPERATOR + Constants.FILE_PATH;
		String fileName = filePath + SEPERATOR + Constants.FILE_NAME;
		try {
			BufferedReader bReader = new BufferedReader(
					new FileReader(fileName));
			String exceptionString;
			while ((exceptionString = bReader.readLine()) != null) {
				builder.append(exceptionString);
			}
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return builder.toString();
	}

	public static List getConfiguredExceptions() {
		String[] selectedItems = {};
		selectedItems = readExceptionsFromFile().split("\\" + DELIMITER);
		return Arrays.asList(selectedItems);
	}
}
