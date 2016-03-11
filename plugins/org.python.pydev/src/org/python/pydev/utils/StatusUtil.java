/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.python.pydev.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;

/**
 * A utility class to work with IStatus.
 */
public class StatusUtil {

    /**
     * Compares two instances of <code>IStatus</code>. The more severe is returned:
     * An error is more severe than a warning, and a warning is more severe
     * than ok. If the two stati have the same severity, the second is returned.
     * @param s1 first status
     * @param s2 second status
     * @return the more severe status
     */
    public static IStatus getMoreSevere(IStatus s1, IStatus s2) {
        if (s1.getSeverity() > s2.getSeverity()) {
            return s1;
        } else {
            return s2;
        }
    }

    /**
     * Finds the most severe status from a array of stati.
     * An error is more severe than a warning, and a warning is more severe
     * than ok.
     * @param status an array of stati
     * @return the most severe status
     */
    public static IStatus getMostSevere(IStatus[] status) {
        IStatus max = null;
        for (int i = 0; i < status.length; i++) {
            IStatus curr = status[i];
            if (curr.matches(IStatus.ERROR)) {
                return curr;
            }
            if (max == null || curr.getSeverity() > max.getSeverity()) {
                max = curr;
            }
        }
        return max;
    }

    /**
     * Applies the status to the status line of a dialog page.
     * @param page the dialog page
     * @param status the status to apply
     */
    public static void applyToStatusLine(DialogPage page, IStatus status) {
        String message = status.getMessage();
        if (message != null && message.length() == 0) {
            message = null;
        }
        switch (status.getSeverity()) {
            case IStatus.OK:
                page.setMessage(message, IMessageProvider.NONE);
                page.setErrorMessage(null);
                break;
            case IStatus.WARNING:
                page.setMessage(message, IMessageProvider.WARNING);
                page.setErrorMessage(null);
                break;
            case IStatus.INFO:
                page.setMessage(message, IMessageProvider.INFORMATION);
                page.setErrorMessage(null);
                break;
            default:
                page.setMessage(null);
                page.setErrorMessage(message);
                break;
        }
    }
}