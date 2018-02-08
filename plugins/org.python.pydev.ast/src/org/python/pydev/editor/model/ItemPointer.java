/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 14, 2004
 */
package org.python.pydev.editor.model;

import java.io.File;
import java.net.URI;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.PropertiesHelper;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.FileTypesPreferences;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.modules.IAbstractJavaClassModule;
import org.python.pydev.editor.codecompletion.revisited.modules.IJavaDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.locator.BaseItemPointer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Location;

/**
 * Pointer points to a python resource inside a file system.
 *
 * You can create one of these, and use PyOpenAction to open the
 * right editor.
 */
public class ItemPointer extends BaseItemPointer {

    /**
     * The definition that originated this ItemPointer (good chance of being null).
     */
    public final Definition definition;

    /**
     * The path within the zip file for this pointer (null if we're not dealing with a zip file)
     */
    public final String zipFilePath;

    public ItemPointer(Object file) {
        this(file, new Location(), new Location());
    }

    public ItemPointer(Object file, SimpleNode n) {
        super(file, new Location(n.beginLine - 1, n.beginColumn - 1), new Location(n.beginLine - 1, n.beginColumn - 1));
        this.definition = null;
        this.zipFilePath = null;
    }

    public ItemPointer(Object file, Location start, Location end) {
        this(file, start, end, null, null);
    }

    public ItemPointer(Object file, Location start, Location end, Definition definition, String zipFilePath) {
        super(file, start, end);
        this.definition = definition;
        this.zipFilePath = zipFilePath;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        ItemPointer i = (ItemPointer) obj;
        if (!i.file.equals(file)) {
            return false;
        }
        if (!i.start.equals(start)) {
            return false;
        }
        if (!i.end.equals(end)) {
            return false;
        }
        //Overridden to check the zipFilePath
        if (i.zipFilePath != null && zipFilePath == null) {
            return false;
        }
        if (zipFilePath != null) {
            if (!zipFilePath.equals(i.zipFilePath)) {
                return false;
            }
        }

        return true;
    }

    public URI getFileAsURI() {
        // Also see org.python.pydev.editor.actions.PyOpenAction
        Object file = this.file;
        if (file instanceof File) {
            File f = (File) file;
            String filename = f.getName();
            if (PythonPathHelper.isValidSourceFile(filename) || filename.indexOf('.') == -1 || //treating files without any extension!
                    (zipFilePath != null && PythonPathHelper.isValidSourceFile(zipFilePath))) {

                //Keep on going as we were going...

            } else if (definition instanceof IJavaDefinition) {
                Log.log(new RuntimeException("Not currently able to convert JavaDefinition to URI."));
                return null;

            } else {
                boolean giveError = true;

                if (definition != null && definition.module instanceof IAbstractJavaClassModule) {
                    Log.log(new RuntimeException("Not currently able to convert AbstractJavaClassModule to URI."));
                    return null;

                } else {
                    if (FileTypesPreferences.isValidDll(filename)) {
                        if (f.exists()) {
                            //It's a pyd or dll, let's check if it was a cython module to open it...
                            File parentFile = f.getParentFile();
                            File newFile = new File(parentFile, StringUtils.stripExtension(f.getName()) + "." + "pyx");

                            if (!newFile.exists()) {
                                newFile = new File(parentFile, StringUtils.stripExtension(f.getName()) + "." + "pxd");
                            }
                            if (!newFile.exists()) {
                                newFile = new File(parentFile, StringUtils.stripExtension(f.getName()) + "." + "pxi");
                            }

                            if (newFile.exists()) {
                                giveError = false;
                                file = newFile;
                            }
                        }
                    }
                }

                if (giveError) {
                    return null;
                }
            }
        }

        if (zipFilePath != null) {
            Log.log(StringUtils.format(
                    "Not currently able to convert file with zip path to URI.\nFile: %s\nStart: %s\nEnd: %s\nZip path: %s\n",
                    file, start, end, zipFilePath));
            return null;

        } else if (file instanceof IFile) {
            IFile f = (IFile) file;
            return f.getRawLocationURI();

        } else if (file instanceof IPath) {
            IPath path = (IPath) file;
            return path.toFile().toURI();

        } else if (file instanceof File) {
            return ((File) file).toURI();
        }

        return null;
    }

    /**
     * @return a string to be used to recreate the item pointer. Note that it doesn't provide
     * the definition even if it was present in this pointer.
     */
    public String asPortableString() {
        IPath path;
        if (file instanceof IFile) {
            IFile f = (IFile) file;
            path = f.getRawLocation();

        } else if (file instanceof IPath) {
            path = (IPath) file;

        } else if (file instanceof File) {
            String absPath = FileUtils.getFileAbsolutePath((File) file);
            path = Path.fromOSString(absPath);

        } else if (file instanceof String) {
            path = Path.fromOSString((String) file);

        } else if (file == null) {
            path = null;

        } else {
            throw new RuntimeException("Don't know how to handle: " + file.getClass());
        }
        if (path == null) {
            return null; //the path is really needed.
        }

        Properties properties = new Properties();
        properties.put("FILE_PATH", path.toPortableString());
        if (start != null) {
            properties.put("START_LINE", String.valueOf(start.line));
            properties.put("START_COL", String.valueOf(start.column));
        }
        if (end != null) {
            properties.put("END_LINE", String.valueOf(end.line));
            properties.put("END_COL", String.valueOf(end.column));
        }
        if (zipFilePath != null) {
            properties.put("ZIP", zipFilePath);
        }

        return PropertiesHelper.createStringFromProperties(properties);
    }

    public static ItemPointer fromPortableString(String asPortableString) {
        Properties properties = PropertiesHelper.createPropertiesFromString(asPortableString);
        String filePath = (String) properties.get("FILE_PATH");
        if (filePath == null) {
            return null;
        }
        String startLine = (String) properties.get("START_LINE");
        String startCol = (String) properties.get("START_COL");
        Location start;
        if (startLine != null && startCol != null) {
            start = new Location(Integer.parseInt(startLine), Integer.parseInt(startCol));
        } else {
            start = new Location();
        }
        String endLine = (String) properties.get("END_LINE");
        String endCol = (String) properties.get("END_COL");
        Location end;
        if (endLine != null && endCol != null) {
            end = new Location(Integer.parseInt(endLine), Integer.parseInt(endCol));
        } else {
            end = new Location();
        }
        String zip = (String) properties.get("ZIP");
        return new ItemPointer(Path.fromPortableString(filePath), start, end, null, zip);
    }

}
