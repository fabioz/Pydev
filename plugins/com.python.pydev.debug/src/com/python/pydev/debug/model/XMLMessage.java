/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 20, 2006
 */
package com.python.pydev.debug.model;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.shared_core.structure.Tuple;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLMessage extends DefaultHandler {

    //------------------------- static stuff
    private static SAXParserFactory parserFactory = SAXParserFactory.newInstance();

    private static SAXParser getSAXParser() throws CoreException {
        SAXParser parser = null;
        try {
            synchronized (parserFactory) {
                parser = parserFactory.newSAXParser();
            }
        } catch (ParserConfigurationException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML SAX error", e));
        } catch (SAXException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML SAX error", e));
        }
        return parser;
    }

    public static Tuple<String, Integer> getMessage(String payload) {
        XMLMessage m = new XMLMessage();
        try {
            SAXParser parser = getSAXParser();
            parser.parse(new ByteArrayInputStream(payload.getBytes()), m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return m.info;
    }

    //------------------------- end static stuff

    Tuple<String, Integer> info = new Tuple<String, Integer>("", 0);

    //message == <xml><io s="%s" ctx="%s"/></xml>
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            if (qName.equals("io")) {
                String s = URLDecoder.decode(attributes.getValue("s"), "UTF-8");
                String ctx = URLDecoder.decode(attributes.getValue("ctx"), "UTF-8");
                this.info.o1 = s;
                this.info.o2 = Integer.parseInt(ctx);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
