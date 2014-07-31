/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 22, 2004
 */
package org.python.pydev.debug.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IStackFrame;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.EvaluateDebugConsoleExpression;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
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

    private static String decode(String value) {
        if (value != null) {
            try {
                return URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * SAX parser for thread info
     * <xml><thread name="name" id="id"/><xml>
     */
    static class XMLToThreadInfo extends DefaultHandler {

        public AbstractDebugTarget target;
        public List<PyThread> threads = new ArrayList<PyThread>();

        public XMLToThreadInfo(AbstractDebugTarget target) {
            this.target = target;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("thread")) {
                String name = attributes.getValue("name");
                String id = attributes.getValue("id");
                name = decode(name);
                threads.add(new PyThread(target, name, id));
            }
        }
    }

    /**
     * Creates IThread[] from the XML response
     */
    static public PyThread[] ThreadsFromXML(AbstractDebugTarget target, String payload) throws CoreException {
        try {
            SAXParser parser = getSAXParser();
            XMLToThreadInfo info = new XMLToThreadInfo(target);
            parser.parse(new ByteArrayInputStream(payload.getBytes()), info);
            return info.threads.toArray(new PyThread[0]);

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
            if (value != null) {
                value = URLDecoder.decode(value, "UTF-8");
            }
        } catch (Exception e) {
            Log.log(e);
        }
        try {
            if (name != null) {
                name = URLDecoder.decode(name, "UTF-8");
            }
        } catch (Exception e) {
            Log.log(e);
        }
        String isContainer = attributes.getValue("isContainer");
        if ("True".equals(isContainer)) {
            var = new PyVariableCollection(target, name, type, value, locator);
        } else {
            var = new PyVariable(target, name, type, value, locator);
        }
        return var;
    }

    /**
     * XMLToStack SAX traverse
     */
    static class XMLToStackInfo extends DefaultHandler {
        public PyThread thread;
        public String stopReason;
        public List<IStackFrame> stack = new ArrayList<IStackFrame>();
        public List<PyVariable> locals;
        public AbstractDebugTarget target;
        PyStackFrame currentFrame;

        public XMLToStackInfo(AbstractDebugTarget target) {
            this.target = target;
        }

        private void startThread(Attributes attributes) throws SAXException {
            String target_id = attributes.getValue("id");
            thread = target.findThreadByID(target_id);
            if (thread == null) {
                throw new SAXException("Thread not found (" + target_id + ")"); // can happen when debugger has been destroyed
            }

            stopReason = attributes.getValue("stop_reason");
        }

        private void startFrame(Attributes attributes) {
            String name = attributes.getValue("name");
            String id = attributes.getValue("id");
            String file = attributes.getValue("file");
            try {
                if (file != null) {
                    file = URLDecoder.decode(file, "UTF-8");
                    File tempFile = new File(file);
                    if (tempFile.exists()) {
                        file = FileUtils.getFileAbsolutePath(tempFile);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String line = attributes.getValue("line");
            IPath filePath = new Path(file);
            // Try to recycle old stack objects
            currentFrame = thread.findStackFrameByID(id);
            if (currentFrame == null) {
                currentFrame = new PyStackFrame(thread, id, name, filePath, Integer.parseInt(line), target);
            } else {
                currentFrame.setName(name);
                currentFrame.setPath(filePath);
                currentFrame.setLine(Integer.parseInt(line));
            }
            stack.add(currentFrame);
        }

        /**
         * Assign stack frames to thread.
         * Assign global variables to thread
         * Assign local variables to stack frame
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            /*
             <xml>
               <thread id="id"/>
                    <frame id="id" name="functionName " file="file" line="line">

                        @deprecated: variables are no longer returned in this request (they are
                        gotten later in asynchronously to speed up the debugger).
                        <var scope="local" name="self" type="ObjectType" value="<DeepThread>"/>

                    </frame>*
             */
            if (qName.equals("thread")) {
                startThread(attributes);

            } else if (qName.equals("frame")) {
                startFrame(attributes);

            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

    }

    public static class StoppedStack {

        public final PyThread thread;
        public final String stopReason;
        public final IStackFrame[] stack;

        public StoppedStack(PyThread thread, String stopReason, IStackFrame[] stack) {
            this.thread = thread;
            this.stopReason = stopReason;
            this.stack = stack;
        }

    }

    /**
     * @param payload
     * @return an array of [thread_id, stopReason, IStackFrame[]]
     */
    public static StoppedStack XMLToStack(AbstractDebugTarget target, String payload) throws CoreException {
        IStackFrame[] stack;
        StoppedStack retVal;
        try {
            SAXParser parser = getSAXParser();
            XMLToStackInfo info = null;
            try {
                info = new XMLToStackInfo(target);
                parser.parse(new ByteArrayInputStream(payload.getBytes()), info);
            } catch (SAXParseException e) {
                info = new XMLToStackInfo(target);
                FastStringBuffer buf2 = fixXml(payload);
                parser.parse(new ByteArrayInputStream(buf2.getBytes()), info);
                Log.log("Received wrong xml which was fixed but indicates problem in the debugger in the server-side (please report error):\n"
                        + payload, e);
            }

            stack = info.stack.toArray(new IStackFrame[0]);

            retVal = new StoppedStack(info.thread, info.stopReason, stack);
        } catch (CoreException e) {
            throw e;
        } catch (SAXException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error reading:"
                    + payload, e));
        } catch (IOException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected IO error reading xml:"
                    + payload, e));
        }
        return retVal;
    }

    /**
     * Try to fix a xml (which actually shouldn't happen): replace <,  > and " on wrong places with &lt; &gt; and &quot;
     */
    public static FastStringBuffer fixXml(String payload) {
        int length = payload.length();
        FastStringBuffer buf2 = new FastStringBuffer(length + 10);

        boolean inQuotes = false;
        boolean inTag = false;

        for (int i = 0; i < length; i++) {
            char c = payload.charAt(i);
            if (c == '"') {
                if (inTag) {
                    inQuotes = !inQuotes;
                    buf2.append(c);
                } else {
                    buf2.append("&quot;");
                }

            } else if (c == '<') {
                if (inQuotes) {
                    buf2.append("&lt;");
                } else {
                    inTag = true;
                    buf2.append(c);
                }

            } else if (c == '>') {
                if (inQuotes) {
                    buf2.append("&gt;");
                } else {
                    inTag = false;
                    buf2.append(c);
                }

            } else {
                buf2.append(c);
            }
        }
        return buf2;
    }

    /**
     * Processes CMD_GET_VARIABLE return
     *
     */
    static class XMLToVariableInfo extends DefaultHandler {
        private AbstractDebugTarget target;
        private IVariableLocator locator;
        public List<PyVariable> vars;

        public XMLToVariableInfo(AbstractDebugTarget target, IVariableLocator locator) {
            this.target = target;
            this.locator = locator;
            vars = new ArrayList<PyVariable>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            // <var name="self" type="ObjectType" value="<DeepThread>"/>
            // create a local variable, and add it to locals
            if (qName.equals("var")) {
                vars.add(createVariable(target, locator, attributes));
            }
        }
    }

    public static PyVariable[] XMLToVariables(AbstractDebugTarget target, IVariableLocator locator, String payload)
            throws CoreException {
        try {
            SAXParser parser = getSAXParser();
            XMLToVariableInfo info = new XMLToVariableInfo(target, locator);
            parser.parse(new ByteArrayInputStream(payload.getBytes()), info);
            PyVariable[] vars = new PyVariable[info.vars.size()];
            for (int i = 0; i < info.vars.size(); i++) {
                vars[i] = info.vars.get(i);
            }
            return vars;
        } catch (CoreException e) {
            throw e;
        } catch (SAXException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
        } catch (IOException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
        }
    }

    // Processing referrers --------------------------------------------------------------------------------------------

    /**
     * Processes Custom command to get referrers.
     */
    static class XMLToReferrersInfoHandler extends DefaultHandler {
        private AbstractDebugTarget target;
        public List<PyVariable> vars;
        public PyVariable forVar;
        private IVariableLocator locator;
        private boolean inFor;

        /**
         * @param locationInDb How to access the variable searched in the debugger.
         */
        public XMLToReferrersInfoHandler(AbstractDebugTarget target, final IVariableLocator locator) {
            this.target = target;
            this.locator = locator;
            vars = new ArrayList<PyVariable>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            // <var name="self" type="ObjectType" value="<DeepThread>"/>
            // create a local variable, and add it to locals
            if (qName.equals("for")) {
                inFor = true;

            } else if (qName.equals("var")) {
                PyVariable var = createVariable(target, locator, attributes);

                //When we find a var for the referrers, usually we have the id and sometimes we can know how that
                //variable is referenced in the container.
                String id = attributes.getValue("id");

                String foundAs = attributes.getValue("found_as");
                try {
                    if (foundAs != null) {
                        foundAs = URLDecoder.decode(foundAs, "UTF-8");
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
                var.setRefererrerFoundInfo(id, foundAs);

                if (inFor) {
                    forVar = var;
                } else {
                    vars.add(var);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("for")) {
                inFor = false;
            }
        }
    }

    public static class XMLToReferrersInfo {

        public final PyVariable forVar;
        public final PyVariable[] vars;
        public final AbstractDebugTarget target;

        public XMLToReferrersInfo(AbstractDebugTarget target, PyVariable forVar, PyVariable[] vars) {
            this.target = target;
            this.forVar = forVar;
            this.vars = vars;
        }

    }

    /**
     * May return null if there's some error in the processing.
     */
    public static XMLToReferrersInfo XMLToReferrers(final AbstractDebugTarget target,
            final IVariableLocator locationInDb,
            String payload) {
        try {
            SAXParser parser = getSAXParser();
            XMLToReferrersInfoHandler info = new XMLToReferrersInfoHandler(target, locationInDb);
            parser.parse(new ByteArrayInputStream(payload.getBytes()), info);

            PyVariable[] vars = info.vars.toArray(new PyVariable[info.vars.size()]);

            return new XMLToReferrersInfo(target, info.forVar, vars);

        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

    // Processing completions ------------------------------------------------------------------------------------------

    /**
     * Processes CMD_GET_COMPLETIONS return
     *
     */
    static class XMLToCompletionsInfo extends DefaultHandler {
        public List<Object[]> completions;

        public XMLToCompletionsInfo() {
            completions = new ArrayList<Object[]>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            // <comp p0="%s" p1="%s" p2="%s" p3="%s"/>
            if (qName.equals("comp")) {

                Object[] comp = new Object[] { decode(attributes.getValue("p0")), decode(attributes.getValue("p1")),
                        decode(attributes.getValue("p2")), decode(attributes.getValue("p3")), };

                completions.add(comp);
            }
        }

    }

    public static List<Object[]> convertXMLcompletionsFromConsole(String payload) throws CoreException {
        try {
            SAXParser parser = getSAXParser();
            XMLToCompletionsInfo info = new XMLToCompletionsInfo();
            parser.parse(new ByteArrayInputStream(payload.getBytes()), info);
            return info.completions;
        } catch (CoreException e) {
            throw e;
        } catch (SAXException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
        } catch (IOException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
        }

    }

    /**
     * Creates an object of
     * EvaluateDebugConsoleExpression.PydevDebugConsoleMessage. Parse the XML in
     * the below mentioned format
     * 		<xml>
     * 			<output message = console_output_message></output>
     * 			<error message = console_error_message></error>
     * 			<more>true/false</more>
     * 		</xml>
     *
     * @author hussain.bohra
     */
    static class DebugConsoleMessageInfo extends DefaultHandler {
        private EvaluateDebugConsoleExpression.PydevDebugConsoleMessage debugConsoleMessage;
        private String attrValue;

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            attrValue = new String(ch, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            boolean isError = true;
            if (qName.equalsIgnoreCase("MORE")) {
                return;
            }
            if (qName.equalsIgnoreCase("OUTPUT")) {
                isError = false;
            }
            for (int i = 0; i < attributes.getLength(); i++) {
                if (attributes.getQName(i).equalsIgnoreCase("MESSAGE")) {
                    String outputMessage = attributes.getValue(i);
                    debugConsoleMessage.appendMessage(outputMessage, isError);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equalsIgnoreCase("MORE")) {
                if (attrValue.equals("True")) {
                    debugConsoleMessage.setMore(true);
                } else {
                    debugConsoleMessage.setMore(false);
                }
            }
        }

        public DebugConsoleMessageInfo() {
            debugConsoleMessage = new EvaluateDebugConsoleExpression.PydevDebugConsoleMessage();
        }
    }

    /**
     * Get an instance of a SAXParser and create a new DebugConsoleMessageInfo object.
     *
     * Call the parser passing it a DebugConsoleMessageInfo Object
     *
     * @param payload
     * @return
     * @throws CoreException
     */
    public static EvaluateDebugConsoleExpression.PydevDebugConsoleMessage getConsoleMessage(String payload)
            throws CoreException {
        EvaluateDebugConsoleExpression.PydevDebugConsoleMessage debugConsoleMessage = new EvaluateDebugConsoleExpression.PydevDebugConsoleMessage();
        try {
            SAXParser parser = getSAXParser();

            DebugConsoleMessageInfo info = new DebugConsoleMessageInfo();
            parser.parse(new ByteArrayInputStream(payload.getBytes()), info);
            debugConsoleMessage = info.debugConsoleMessage;

        } catch (SAXException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error. Payload: "
                    + payload, e));
        } catch (IOException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
        }
        return debugConsoleMessage;
    }

    /**
     * Create a List<ExceptionStackTrace> from the received XML in the below mentioned
     * format:
     *     <xml>
     *        <frame
     *            thread_id = "pid1388_seq2"
     *            file="test\test.py"
     *            line="1332"
     *            name="func"
     *            obj="old_f(*args, **kwargs)" />
     *     </xml>
     *
     * @author hussain.bohra
      */
    static class ExceptionStackTraceXMLInfo extends DefaultHandler {
        private List<PyConditionalBreakPointManager.ExceptionStackTrace> exceptionStackTraceList;
        private PyConditionalBreakPointManager.ExceptionStackTrace exceptionStackTrace;
        //private String attrValue;
        private AbstractDebugTarget target;

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            //attrValue = new String(ch, start, length);
        }

        /**
         * Create a new ExceptionStackTrace Object on encountering <frame> tag.
         * Adds an object to main list
         *
         * Identify thread_id, filename, line, name and methodObj from xml and
         * creates a new ExceptionStackTrace Object
         *
         */
        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            if (qName.equalsIgnoreCase("FRAME")) {
                String filename = "";
                String name = "";
                String methodObj = "";
                int line = 0;

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equalsIgnoreCase("THREAD_ID")) {
                        //Ignore for now.

                    } else if (attributes.getQName(i).equalsIgnoreCase("FILE")) {
                        filename = attributes.getValue(i);

                    } else if ((attributes.getQName(i).equalsIgnoreCase("LINE"))) {
                        line = Integer.parseInt(attributes.getValue(i));

                    } else if ((attributes.getQName(i).equalsIgnoreCase("NAME"))) {
                        name = attributes.getValue(i);

                    } else if ((attributes.getQName(i).equalsIgnoreCase("OBJ"))) {
                        methodObj = attributes.getValue(i);

                    }
                }
                //PyThread pyThread = target.findThreadByID(threadId);
                //if (pyThread == null) {
                //    // can happen when debugger has been destroyed
                //    throw new SAXException("Thread not found (" + threadId + ")");
                //}
                exceptionStackTrace = new PyConditionalBreakPointManager.ExceptionStackTrace(
                        target, filename, line, name, methodObj);
                exceptionStackTraceList.add(exceptionStackTrace);
            }
        }

        public ExceptionStackTraceXMLInfo(AbstractDebugTarget target) {
            this.exceptionStackTraceList = new ArrayList<PyConditionalBreakPointManager.ExceptionStackTrace>();
            this.target = target;
        }
    }

    /**
     * Get an instance of a SAXParser and create a new ExceptionStackTraceXMLInfo object.
     *
     * Call the parser passing it an ExceptionStackTraceXMLInfo Object
     *
     * @param payload
     * @param AbstractDebugTarget target
     * @return
     * @throws CoreException
     */
    public static List<PyConditionalBreakPointManager.ExceptionStackTrace> getExceptionStackTrace(
            AbstractDebugTarget target, String payload) throws CoreException {
        List<PyConditionalBreakPointManager.ExceptionStackTrace> exceptionStackTraceList = new ArrayList<PyConditionalBreakPointManager.ExceptionStackTrace>();
        try {
            SAXParser parser = getSAXParser();
            ExceptionStackTraceXMLInfo info = new ExceptionStackTraceXMLInfo(target);
            parser.parse(new ByteArrayInputStream(payload.getBytes("utf-8")), info);
            exceptionStackTraceList = info.exceptionStackTraceList;
        } catch (SAXException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
        } catch (IOException e) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected XML error", e));
        }
        return exceptionStackTraceList;
    }
}
