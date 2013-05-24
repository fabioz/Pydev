package org.python.pydev.core;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.ui.PartInitException;
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
     * Open webpage given by URI with the default system browser.
     */
    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Open webpage given by URL with the default system browser.
     */
    public static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    
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
        } catch (PartInitException e) {
            Log.log(e);
        }
    }
}

