/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import java.io.File;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.core.REF;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Translate XML protocol responses into Py structures.
 * 
 * Things get more complex than I'd like when complex Py structures get built.
 */
public class XMLUtils {

	static SAXParserFactory parserFactory = SAXParserFactory.newInstance();

	static SAXParser getSAXParser() throws CoreException {
		SAXParser parser = null;
		try {
			synchronized(parserFactory) {
				parser = parserFactory.newSAXParser();
			}
		} catch (ParserConfigurationException e) {
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML SAX error", e));
		} catch (SAXException e) {
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML SAX error", e));
		}
		return parser;
	}
	/**
	 * SAX parser for thread info
	 * <xml><thread name="name" id="id"/><xml>
	 */
	static class XMLToThreadInfo extends DefaultHandler {
		
		public AbstractDebugTarget target;
		public ArrayList threads = new ArrayList();
		
		public XMLToThreadInfo(AbstractDebugTarget target) {
			this.target = target;
		}

		public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
				if (qName.equals("thread")) {
					String name = attributes.getValue("name");
					String id = attributes.getValue("id");
					try {
                        if (name != null){
                            name = URLDecoder.decode(name, "UTF-8");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
					threads.add(new PyThread(target, name, id));
				}
		}
	}

	/**
	 * Creates IThread[] from the XML response
	 */
	static public IThread[] ThreadsFromXML(AbstractDebugTarget target, String payload) throws CoreException {
		try {
			SAXParser parser = getSAXParser();
			XMLToThreadInfo info = new XMLToThreadInfo(target);
			parser.parse(new StringBufferInputStream(payload), info);
			return (IThread[]) info.threads.toArray(new IThread[0]);
			
		} catch (CoreException e) {
			throw e;
		} catch (SAXException e) {
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
		} catch (IOException e) {
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
		}
	}

	/**
	 * Creates a variable from XML attributes
	 * <var name="self" type="ObjectType" value="<DeepThread>"/>
	 */
	static PyVariable createVariable(AbstractDebugTarget target, IVariableLocator locator, Attributes attributes) {
		PyVariable var;
		String name = attributes.getValue("name");
		String type = attributes.getValue("type");
		String value = attributes.getValue("value");
		try {
            if (value != null){
                value = URLDecoder.decode(value, "UTF-8");
            }
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
		String isContainer = attributes.getValue("isContainer");
		if ("True".equals(isContainer)){
			var = new PyVariableCollection(target, name, type, value, locator);
		}else{ 
			var = new PyVariable(target,  name, type, value);
		}
		return var;
	}

	/**
	 * XMLToStack SAX traverse
	 */
	static class XMLToStackInfo extends DefaultHandler {
		public PyThread thread;
		public String stop_reason;
		public ArrayList stack = new ArrayList();
		public ArrayList locals;
		public AbstractDebugTarget target;
		PyStackFrame currentFrame;
		
		public XMLToStackInfo(AbstractDebugTarget target) {
			this.target = target;
		}

		private void startThread(Attributes attributes) throws SAXException {
			String target_id = attributes.getValue("id");
			thread = target.findThreadByID(target_id);
			if (thread == null){
				throw new SAXException("Thread not found");		// can happen when debugger has been destroyed
			}
			
			stop_reason = attributes.getValue("stop_reason");
		}
		
		private void startFrame(Attributes attributes) {
			String name = attributes.getValue("name");
			String id = attributes.getValue("id");
			String file = attributes.getValue("file");
			try {
                if (file != null){
                    file = URLDecoder.decode(file, "UTF-8");
                    file = REF.getFileAbsolutePath(new File(file));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
			String line = attributes.getValue("line");
			IPath filePath = new Path(file);
			// Try to recycle old stack objects
			currentFrame = thread.findStackFrameByID(id);
			if (currentFrame == null){
				currentFrame = new PyStackFrame(thread, id, name, filePath, Integer.parseInt(line));
			} else { 
				currentFrame.setName(name);
				currentFrame.setPath(filePath);
				currentFrame.setLine(Integer.parseInt(line));
			}
			stack.add(currentFrame);
		}
		
		private void initializeLocals() {
			locals = new ArrayList();
			PyVariableCollection global = new PyVariableCollection(target, "Globals", "frame.f_global", "Global variables", currentFrame.getGlobalLocator());
			locals.add(global);	// locals always include global as the top
		}
		
		// local variables belong to the stack frame
		// when 
		private void startVar(Attributes attributes) {
			if (locals == null)
				initializeLocals();
			// create a local variable, and add it to locals
			PyVariable newLocal = createVariable(target, currentFrame.getLocalsLocator(), attributes);
			locals.add(newLocal);
		}
		

		/**
		 * Assign stack frames to thread.
		 * Assign global variables to thread
		 * Assign local variables to stack frame
	     */
		public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
				/*       
				 <xml>
				   <thread id="id"/>
						<frame id="id" name="functionName " file="file" line="line">
							<var scope="local" name="self" type="ObjectType" value="<DeepThread>"/>
						</frame>*
				 */
				if (qName.equals("thread")){
					startThread(attributes);
					
				}else if (qName.equals("frame")){ 
					startFrame(attributes);
					
				}else if (qName.equals("var")){
					startVar(attributes);
				}
		}
		
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
			if (qName.equals("frame")) {
				// when frame ends, we need to assign all the local variables
				if (locals == null){
					initializeLocals();
				}
				
				IVariable[] locArry = new IVariable[locals.size()];
				
				for (int i=0; i < locArry.length; i++){
					locArry[i] = (IVariable)locals.get(i);
				}
				
				currentFrame.setVariables(locArry);
				locals = null;
			}
		}

	}
	/**
	 * @param payload
	 * @return an array of [thread_id, stop_reason, IStackFrame[]]
	 */
	public static Object[] XMLToStack(AbstractDebugTarget target, String payload) throws CoreException {
		IStackFrame[] stack;
		Object[] retVal = new Object[3];
		try {
			SAXParser parser = getSAXParser();
			XMLToStackInfo info = new XMLToStackInfo(target);
			parser.parse(new StringBufferInputStream(payload), info);
			stack = new IStackFrame[info.stack.size()];
			Iterator it = info.stack.iterator();
			int i = 0;
			while (it.hasNext())
				stack[i++] = (IStackFrame)it.next();
			retVal[0] = info.thread;
			retVal[1] = info.stop_reason;
			retVal[2] = stack;
		} catch (CoreException e) {
			throw e;
		} catch (SAXException e) {
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error reading:"+payload, e));
		} catch (IOException e) {
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected IO error reading xml:"+payload, e));
		}
		return retVal;
	}


	/**
	 * Processes CMD_GET_VARIABLE return
	 *
	 */
	static class XMLToVariableInfo extends DefaultHandler {
		private AbstractDebugTarget target;
		private IVariableLocator locator;
		public ArrayList vars;
		
		public XMLToVariableInfo(AbstractDebugTarget target, IVariableLocator locator) {
			this.target = target;
			this.locator = locator;
			vars = new ArrayList();
		}
		
		public void startElement(String uri, String localName, String qName,
								Attributes attributes) throws SAXException {
			// <var name="self" type="ObjectType" value="<DeepThread>"/>
			// create a local variable, and add it to locals
			if (qName.equals("var"))
				vars.add(createVariable(target, locator, attributes));
		}
	}

	public static PyVariable[] XMLToVariables(AbstractDebugTarget target, IVariableLocator locator, String payload) throws CoreException {
		try {
			SAXParser parser = getSAXParser();
			XMLToVariableInfo info = new XMLToVariableInfo(target, locator);
			parser.parse(new StringBufferInputStream(payload), info);
			PyVariable[] vars = new PyVariable[info.vars.size()];
			for (int i =0; i< info.vars.size(); i++)
				vars[i] = (PyVariable)info.vars.get(i);
			return vars;
		} catch (CoreException e) {
			throw e;
		} catch (SAXException e) {
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
		} catch (IOException e) {
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
		}
	}

}
