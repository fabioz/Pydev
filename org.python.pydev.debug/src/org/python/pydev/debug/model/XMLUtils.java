/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Various utilities
 */
public class XMLUtils {

	static SAXParserFactory parserFactory = SAXParserFactory.newInstance();
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
					if (name != null)
						name = URLDecoder.decode(name);
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
			SAXParser parser;
			synchronized(parserFactory) {
				parser = parserFactory.newSAXParser();
			}
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

	static class XMLToStackInfo extends DefaultHandler {
		public String thread_id;
		public String stop_reason;
		public ArrayList stack = new ArrayList();
		
/*       
 """ <xml>
        	<thread id="id"/>
        	<frame id="id" name="functionName " file="file" line="line">
 */
		public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
				if (qName.equals("thread")) {
					thread_id = attributes.getValue("id");
					stop_reason = attributes.getValue("stop_reason");
				}
				else if (qName.equals("frame")) {
					String name = attributes.getValue("name");
					String id = attributes.getValue("id");
					String file = attributes.getValue("file");
					String line = attributes.getValue("line");
					IPath filePath = new Path(file);
					stack.add(new PyStackFrame(id, name, filePath, Integer.parseInt(line)));
				}
		}
	}
	/**
	 * @param payload
	 * @return an array of [thread_id, stop_reason, IStackFrame[]]
	 */
	public static Object[] XMLToStack(String payload) {
		String thread_id;
		IStackFrame[] stack;
		Object[] retVal = new Object[3];
		try {
			SAXParser parser;
			synchronized(parserFactory) {
				parser = parserFactory.newSAXParser();
			}
			XMLToStackInfo info = new XMLToStackInfo();
			parser.parse(new StringBufferInputStream(payload), info);
			stack = new IStackFrame[info.stack.size()];
			Iterator it = info.stack.iterator();
			int i = 0;
			while (it.hasNext())
				stack[i++] = (IStackFrame)it.next();
			retVal[0] = info.thread_id;
			retVal[1] = info.stop_reason;
			retVal[2] = stack;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return retVal;
	}

}
