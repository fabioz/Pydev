/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.debug.core.model.IThread;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Various utilities
 */
public class ModelUtils {


	/**
	 * SAX parser for thread info
	 * <xml><thread name="name" id="id"/><xml>
	 */
	static class XMLToThreadInfo extends DefaultHandler {
		
		public PyDebugTarget target;
		public ArrayList threads = new ArrayList();
		
		public XMLToThreadInfo(PyDebugTarget target) {
			this.target = target;
		}

		public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
				if (qName.equals("thread")) {
					String name = attributes.getValue("name");
					String id = attributes.getValue("id");
					threads.add(new PyThread(target, name, id));
				}
		}
	}

	/**
	 * Creates IThread[] from the XML response
	 */
	static public IThread[] ThreadsFromXML(PyDebugTarget target, String payload)  {
		IThread[] threads = null;
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			XMLToThreadInfo info = new XMLToThreadInfo(target);
			parser.parse(new StringBufferInputStream(payload), info);
			threads = new IThread[info.threads.size()];
			Iterator it = info.threads.iterator();
			int i = 0;
			while (it.hasNext())
				threads[i++] = (IThread)it.next();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return threads;
	}

}
