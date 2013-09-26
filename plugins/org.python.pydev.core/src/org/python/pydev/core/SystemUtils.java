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
