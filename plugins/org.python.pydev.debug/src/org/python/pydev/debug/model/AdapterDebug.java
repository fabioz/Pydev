/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

public class AdapterDebug {

    public static void print(Object askedfor, Class adapter) {
        if (false) {
            System.out.println(askedfor.getClass().getName() + " requests " + adapter.toString());
        }
    }

    public static void printDontKnow(Object askedfor, Class adapter) {
        if (false) {
            System.out.println("DONT KNOW: " + askedfor.getClass().getName() + " requests " + adapter.toString());
        }
    }

}
