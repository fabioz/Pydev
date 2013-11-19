/******************************************************************************
* Copyright (C) 2013  André Berg and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     André Berg <andre.bergmedia@googlemail.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>           - ongoing maintenance
******************************************************************************/
package org.python.pydev.core;

import java.net.URL;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.python.pydev.core.log.Log;

/**
 * Utils operating system functionality.
 * 
 * @author André Berg
 * @version 0.1
 */
public class SystemUtils {

    /**
     * Open a webpage in Eclipse's default browser.
     * 
     * @param url URL address of the webpage
     * @param id String id for the newly created browser view
     */
    public static void openWebpageInEclipse(URL url, String id) {
        IWebBrowser browser;
        try {
            browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(id);
            browser.openURL(url);
        } catch (Exception e) {
            Log.log(e);
        }
    }
}
