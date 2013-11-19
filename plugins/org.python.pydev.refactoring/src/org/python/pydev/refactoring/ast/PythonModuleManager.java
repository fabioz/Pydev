/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;

/**
 * @author Ueli Kistler
 * 
 */
public class PythonModuleManager {

    private transient IPythonNature nature;

    /**
     * If we're testing, we want to return docs only with \n, otherwise, we want it to be based
     * on the actual document from the user
     */
    private static boolean testingFlag = false;

    public PythonModuleManager(IPythonNature nature) {
        this.nature = nature;
    }

    public static IDocument getDocFromFile(File file) {
        boolean loadIfNotInWorkspace = !testingFlag;

        IDocument doc = null;
        try {
            doc = FileUtilsFileBuffer.getDocFromFile(file, loadIfNotInWorkspace);
        } catch (IOException e1) {
            //ignore (will remain null)
        }
        if (doc == null) {
            try {
                doc = new Document(getFileContent(new FileInputStream(file)));
            } catch (FileNotFoundException e) {
                return null;
            }
        }
        return doc;
    }

    private static String getFileContent(InputStream stream) {
        if (!testingFlag) {
            throw new RuntimeException("Should only call this method in tests.");
        }
        try {
            StringBuilder contentBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                contentBuilder.append(line).append('\n');
            }
            return contentBuilder.toString();
        } catch (IOException e) {
        }
        return "";
    }

    public IModulesManager getIModuleManager() {
        return nature.getAstManager().getModulesManager();
    }

    public static boolean isTesting() {
        return testingFlag;
    }

    public static void setTesting(boolean testingFlag) {
        PythonModuleManager.testingFlag = testingFlag;
    }
}
