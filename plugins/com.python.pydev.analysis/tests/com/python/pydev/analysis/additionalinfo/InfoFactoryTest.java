/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.additionalinfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.python.pydev.analysis.actions.AdditionalInfoAndIInfo;

import junit.framework.TestCase;

public class InfoFactoryTest extends TestCase {

    public void testInfoFactory() throws Exception {
        InfoFactory infoFactory = new InfoFactory(new AdditionalInfoAndIInfo(null, null));
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        Element root = document.createElement("root");
        IMemento memento = new XMLMemento(document, root);
        infoFactory.saveState(memento);

        assertNull(infoFactory.createElement(memento));

        ClassInfo info = new ClassInfo(null, null, null, null);
        infoFactory = new InfoFactory(new AdditionalInfoAndIInfo(null, info));
        infoFactory.saveState(memento);
        assertNull(infoFactory.createElement(memento));
    }
}
