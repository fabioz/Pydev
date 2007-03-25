/*
 * License: Eclipse Public License v1.0
 * Created on Oct 21, 2006
 * 
 * @author Gergely Kis
 */
package org.python.pydev.plugin.nature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/**
 * This class stores PythonNature and PythonPathNature properties inside the project in a file instead of persistent properties. This allows PYTHONPATH and Python project version to be checked in into
 * version control systems.
 * 
 * @author Gergely Kis <gergely.kis@gmail.com>
 * 
 */

class PythonNatureStore implements IResourceChangeListener, IPythonNatureStore {

    /**
     * This is the class that does the store job.
     * 
     * @author Fabio
     */
    private final class PythonNatureStoreJob extends Job {
        private final ByteArrayInputStream is;

        private PythonNatureStoreJob(String name, ByteArrayInputStream is) {
            super(name);
            this.is = is;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            synchronized(xmlFile){
                try{
                    onIgnoreRefresh++;
                    if (!xmlFile.exists()) {
                        xmlFile.create(is, true, monitor);
                        modStamp = xmlFile.getModificationStamp();
                    } else {
                        xmlFile.setContents(is, true, false, monitor);
                        modStamp = xmlFile.getModificationStamp();
                    }
                    xmlFile.refreshLocal(IResource.DEPTH_ZERO, monitor);
                }catch(Exception e){
                    PydevPlugin.log(e);
                }finally{
                    onIgnoreRefresh--;
                }
            }
            return Status.OK_STATUS;
        }
    }

    private final static String STORE_FILE_NAME = ".pydevproject";

    private final static String PYDEV_PROJECT_DESCRIPTION = "pydev_project";

    private final static String PYDEV_NATURE_PROPERTY = "pydev_property";

    private final static String PYDEV_NATURE_PATH_PROPERTY = "pydev_pathproperty";

    private final static String PYDEV_PATH = "path";

    private final static String PYDEV_NATURE_PROPERTY_NAME = "name";

    private volatile IProject project = null;

    private volatile IFile xmlFile = null;

    private volatile long modStamp = IFile.NULL_STAMP;
    
    private volatile int onIgnoreRefresh = 0;

    /**
     * 0 means we're not in a store job
     */
    private volatile int onStoreJob = 0;

    /**
     * Whether the file has already been loaded
     */
    private volatile boolean loaded = false;
    
    /**
     * This is the dom document that is used to manipulate the xml info.
     */
    private volatile Document document = null;
    
    private static final boolean TRACE_PYTHON_NATURE_STORE = false;
    private StringBuffer indent = new StringBuffer();

    private volatile boolean inInit;  
    
    private void traceFunc(String func){
        if(TRACE_PYTHON_NATURE_STORE){
            if(!func.startsWith("END ")){
                System.out.println(indent+func);
                indent.append("  ");
                
            }else{
                indent.delete(0, 2);
                System.out.println(indent+func);
            }
        }
    }
    

    /* (non-Javadoc)
     * @see org.python.pydev.plugin.nature.IPythonNatureStore#setProject(org.eclipse.core.resources.IProject)
     */
    public synchronized void setProject(IProject project) {
        traceFunc("setProject - "+project.getName());
        synchronized (this) {
        	try{
    	        this.project = project;
    	        this.xmlFile = project.getFile(STORE_FILE_NAME);
    	        try {
    	            loadFromFile();
    	        } catch (CoreException e) {
    	            throw new RuntimeException("Error loading project: "+project, e);
    	        }
    	        if (!ProjectModulesManager.IN_TESTS) {
    	            project.getWorkspace().addResourceChangeListener(this);
    	        }
            }finally{
            	loaded = true;
            }
        }
        traceFunc("END setProject - "+project.getName());
    }

    /**
     * @param function the function that is checking for load
     */
    private synchronized void checkLoad(String function) {
        traceFunc("checkLoad");
        synchronized (this) {
        	if(!loaded){
                PydevPlugin.log(new RuntimeException(StringUtils.format("%s still not loaded and '%s' already called.", xmlFile, function)));
            }
        }
        traceFunc("END checkLoad");
    }
    
    /* (non-Javadoc)
     * @see org.python.pydev.plugin.nature.IPythonNatureStore#getPathProperty(org.eclipse.core.runtime.QualifiedName)
     */
    public synchronized String getPathProperty(QualifiedName key) throws CoreException {
        traceFunc("getPathProperty - "+key);
        synchronized (this) {
        	checkLoad("getPathProperty");
            String ret = getPathStringFromArray(getPathPropertyFromXml(key));
            traceFunc("END getPathProperty - "+ret);
            return ret;
        }
    }

	/* (non-Javadoc)
     * @see org.python.pydev.plugin.nature.IPythonNatureStore#setPathProperty(org.eclipse.core.runtime.QualifiedName, java.lang.String)
     */
    public synchronized void setPathProperty(QualifiedName key, String value) throws CoreException {
        traceFunc("setPathProperty");
        synchronized (this) {
        	checkLoad("setPathProperty");
            setPathPropertyToXml(key, getArrayFromPathString(value), true);
        }
        traceFunc("END setPathProperty");
    }

    /**
     * Loads the Xml representation of the PythonNature properties from the project resource. If the project resource does not exist then an empty representation is created, and its storage is
     * requested in the project folder.
     * 
     * @return true if some change has actually happened in the file and false otherwise
     * @throws CoreException
     */
    private synchronized boolean loadFromFile() throws CoreException {
        traceFunc("loadFromFile");
        boolean ret;
        synchronized (this) {
            try {
                DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                try{
                    onIgnoreRefresh++;
                    xmlFile.refreshLocal(IResource.DEPTH_ZERO, null);
                }finally{
                    onIgnoreRefresh--;
                }
                if (!xmlFile.exists()) {
                    if (document != null) {
                        // Someone removed the project descriptor, store it from the memory model
                        doStore();
                        ret = true;
                    } else {
                        // The document never existed
                        document = parser.newDocument();
                        ProcessingInstruction version = document.createProcessingInstruction("eclipse-pydev", "version=\"1.0\""); //$NON-NLS-1$ //$NON-NLS-2$
                        document.appendChild(version);
                        Element configRootElement = document.createElement(PYDEV_PROJECT_DESCRIPTION);
                        document.appendChild(configRootElement);
    
                        migrateProperty(PythonNature.getPythonProjectVersionQualifiedName());
                        migratePath(PythonPathNature.getProjectSourcePathQualifiedName());
                        migratePath(PythonPathNature.getProjectExternalSourcePathQualifiedName());
                        doStore();
                        ret = true;
                    }
                } else {
                	try{
                	    onIgnoreRefresh++;
                		xmlFile.refreshLocal(0, new NullProgressMonitor());
                	}catch(Exception e){
                		PydevPlugin.log(e);
                	}finally{
                        onIgnoreRefresh--;   
                    }
                    document = parser.parse(xmlFile.getContents());
                    modStamp = xmlFile.getModificationStamp();
                    ret = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
                throw new CoreException(status);
            }
        }
        
        traceFunc("END loadFromFile");
        return ret;
    }

    private synchronized void migrateProperty(QualifiedName key) throws CoreException {
        traceFunc("migrateProperty");
        synchronized (this) {
            // Nothing found, try to migrate from persistent property
            String propertyVal = project.getPersistentProperty(key);
            if (propertyVal != null) {
                setPropertyToXml(key, propertyVal, false);
                project.setPersistentProperty(key, (String) null);
            }
        }
        traceFunc("END migrateProperty");
    }

    private synchronized void migratePath(QualifiedName key) throws CoreException {
        traceFunc("migratePath");
        synchronized (this) {
            // Try to migrate from persistent property
            String[] propertyVal = getArrayFromPathString(project.getPersistentProperty(key));
            if (propertyVal != null) {
                // set in the xml
                setPathPropertyToXml(key, propertyVal, false);
                // and remove from the project
                project.setPersistentProperty(key, (String) null);
            }
        }
        traceFunc("END migratePath");
    }

    /**
     * Get the root node of the project description
     * 
     * @return the root Node object
     * @throws CoreException if root node is not present
     */
    private synchronized Node getRootNodeInXml() {
        traceFunc("getRootNodeInXml");
        synchronized (this) {
        	Assert.isNotNull(document);
            NodeList nodeList = document.getElementsByTagName(PYDEV_PROJECT_DESCRIPTION);
            Node ret = null;
            if (nodeList != null && nodeList.getLength() > 0) {
                ret = nodeList.item(0);
            }
            
            traceFunc("END getRootNodeInXml -- "+ret);
            if(ret != null){
                return ret;
            }
            throw new RuntimeException(StringUtils.format("Error. Unable to get the %s tag by its name. Project: %s", PYDEV_PROJECT_DESCRIPTION, project));
        }
    }

    /**
     * Assemble a string representation of a QualifiedName typed key
     * 
     * @param key
     * @return the assembled string key representation
     */
    private synchronized String getKeyString(QualifiedName key) {
        traceFunc("getKeyString");
        synchronized (this) {
            String keyString = key.getQualifier() != null ? key.getQualifier() : "";
            String ret = keyString + "." + key.getLocalName();
            traceFunc("END getKeyString");
            return ret;
        }
    }

    /**
     * Finds a property node as a direct child of the root node with the specified type and key.
     * 
     * @param type
     * @param key
     * @return The property node or null if a node with the supplied key and type cannot be found.
     * @throws CoreException
     */
    private synchronized Node findPropertyNodeInXml(String type, QualifiedName key) {
        traceFunc("findPropertyNodeInXml");
        synchronized (this) {
            Node root = getRootNodeInXml();
            NodeList childNodes = root.getChildNodes();
            if (childNodes != null && childNodes.getLength() > 0) {
                String keyString = getKeyString(key);
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if (child.getNodeName().equals(type)) {
                        NamedNodeMap attrs = child.getAttributes();
                        if (attrs != null && attrs.getLength() > 0) {
                            String name = attrs.getNamedItem(PYDEV_NATURE_PROPERTY_NAME).getNodeValue();
                            if (name != null && name.equals(keyString)) {
                                traceFunc("END findPropertyNodeInXml - "+child);
                                return child;
                            }
                        }
                    }
                }
            }
            traceFunc("END findPropertyNodeInXml (null)");
            return null;
        }
    }

    /**
     * Returns the text contents of a nodes' children. The children shall have the specified type.
     * 
     * @param node
     * @param type
     * @return the array of strings with the text contents or null if the node has no children.
     */
    private synchronized String[] getChildValuesWithType(Node node, String type) {
        traceFunc("getChildValuesWithType");
        synchronized (this) {
            NodeList childNodes = node.getChildNodes();
            if (childNodes != null && childNodes.getLength() > 0) {
                List<String> result = new ArrayList<String>();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if (child.getNodeName().equals(type)) {
                        result.add(getTextContent(child));
                    }
                }
                String[] retval = new String[result.size()];
                traceFunc("END getChildValuesWithType");
                return result.toArray(retval);
            }
            traceFunc("END getChildValuesWithType (null)");
            return null;
        }
    }

    /**
     * Add children to a node with specified type and text contents. For each values array element a new child is created.
     * 
     * @param node
     * @param type
     * @param values
     */
    private synchronized void addChildValuesWithType(Node node, String type, String[] values) {
        traceFunc("addChildValuesWithType");
        synchronized (this) {
            assert (node != null);
            assert (values != null);
            assert (type != null);
            for (int i = 0; i < values.length; i++) {
                Node child = document.createElement(type);
                setTextContent(values[i], child);
                node.appendChild(child);
            }
        }
        traceFunc("END addChildValuesWithType");
    }

    /**
     * Convert an array of path strings to a single string separated by | characters.
     * 
     * @param pathArray
     * @return the assembled string of paths or null if the input was null
     */
    private synchronized String getPathStringFromArray(String[] pathArray) {
        traceFunc("getPathStringFromArray");
        synchronized (this) {
            if (pathArray != null) {
                StringBuffer s = new StringBuffer("");
                for (int i = 0; i < pathArray.length; i++) {
                    if (i > 0) {
                        s.append('|');
                    }
                    s.append(pathArray[i]);
                }
                traceFunc("END getPathStringFromArray");
                return s.toString();
            }
            traceFunc("END getPathStringFromArray (null)");
            return null;
        }
    }

    /**
     * Convert a single string of paths separated by | characters to an array of strings.
     * 
     * @param pathString
     * @return the splitted array of strings or null if the input was null
     */
    private synchronized String[] getArrayFromPathString(String pathString) {
        traceFunc("getArrayFromPathString");
        synchronized (this) {
            if (pathString != null) {
                traceFunc("END getArrayFromPathString");
                return pathString.split("\\|");
            }
            traceFunc("END getArrayFromPathString (null)");
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.python.pydev.plugin.nature.IPythonNatureStore#getPropertyFromXml(org.eclipse.core.runtime.QualifiedName)
     */
    public synchronized String getPropertyFromXml(QualifiedName key){
        traceFunc("getPropertyFromXml - "+key);
        synchronized (this) {
        	checkLoad("getPropertyFromXml");
            try {
                Node propertyNode = findPropertyNodeInXml(PYDEV_NATURE_PROPERTY, key);
    
                if (propertyNode != null) {
                    String ret = getTextContent(propertyNode);
                    traceFunc("END getPropertyFromXml -- "+ret);
                    return ret;
                }
    
                traceFunc("END getPropertyFromXml (null)");
                return null;
            } catch (Exception e) {
                traceFunc("END getPropertyFromXml (EXCEPTION)");
                throw new RuntimeException("Error on document:"+document+" project:"+project, e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.python.pydev.plugin.nature.IPythonNatureStore#setPropertyToXml(org.eclipse.core.runtime.QualifiedName, java.lang.String, boolean)
     */
    public synchronized void setPropertyToXml(QualifiedName key, String value, boolean store) throws CoreException {
        traceFunc(StringUtils.format("setPropertyToXml key:%s value:%s store:%s", key, value, store));
        synchronized (this) {
        	if(store){
        		checkLoad("setPropertyToXml");
        	}
            try {
                Node child = findPropertyNodeInXml(PYDEV_NATURE_PROPERTY, key);
                if (child != null) {
                    if (value == null) {
                        // remove child from file
                        getRootNodeInXml().removeChild(child);
                    } else {
                        setTextContent(value, child);
                    }
                    doStore();
                } else if (value != null) {
                    // The property is not in the file and we need to set it
                    Node property = document.createElement(PYDEV_NATURE_PROPERTY);
                    Node propertyName = document.createAttribute(PYDEV_NATURE_PROPERTY_NAME);
                    propertyName.setNodeValue(getKeyString(key));
                    property.getAttributes().setNamedItem(propertyName);
                    setTextContent(value, property);
                    getRootNodeInXml().appendChild(property);
    
                    if (store) {
                        doStore();
                    }
                }
    
            } catch (Exception e) {
                traceFunc("END setPropertyToXml (EXCEPTION)");
                IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
                throw new CoreException(status);
            }
        }
        traceFunc("END setPropertyToXml");
    }

    /**
     * This function was gotten as a copy of the Node.setTextContent, because this function 
     * is not available in java 1.4
     */
    private synchronized void setTextContent(String textContent, Node self) throws DOMException {
        traceFunc("setTextContent");
        synchronized (this) {
            // get rid of any existing children
            Node child;
            while ((child = self.getFirstChild()) != null) {
                self.removeChild(child);
            }
            // create a Text node to hold the given content
            if (textContent != null && textContent.length() != 0) {
                self.appendChild(document.createTextNode(textContent));
            }
        }
        traceFunc("END setTextContent");
    }
    
    
    private synchronized String getTextContent(Node self) throws DOMException {
        traceFunc("getTextContent");
        synchronized (this) {
            StringBuffer fBufferStr = new StringBuffer();
            Node child = self.getFirstChild();
            if (child != null) {
                Node next = child.getNextSibling();
                if (next == null) {
                    if(hasTextContent(child)){
                        String nodeValue = child.getNodeValue();
                        if(nodeValue != null){
                            traceFunc("END getTextContent - "+nodeValue);
                            return nodeValue;
                        }
                    }
                    traceFunc("END getTextContent - EMPTY");
                    return "";
                }
                if (fBufferStr == null){
                    fBufferStr = new StringBuffer();
                }
                else {
                    fBufferStr.setLength(0);
                }
                getTextContent(fBufferStr, self);
                traceFunc("END getTextContent - "+fBufferStr);
                return fBufferStr.toString();
            }
            traceFunc("END getTextContent - EMPTY");
            return "";
        }
    }

    
    // internal method taking a StringBuffer in parameter
    private synchronized void getTextContent(StringBuffer buf, Node self) throws DOMException {
        traceFunc("getTextContent");
        synchronized (this) {
            Node child = self.getFirstChild();
            while (child != null) {
                if (hasTextContent(child)) {
                    getTextContent(buf, child);
                }
                child = child.getNextSibling();
            }
        }
        traceFunc("END getTextContent");
    }

    // internal method returning whether to take the given node's text content
    private synchronized boolean hasTextContent(Node child) {
        traceFunc("hasTextContent");
        synchronized (this) {
            boolean ret = child.getNodeType() != Node.COMMENT_NODE &&
                            child.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE;
            traceFunc("END hasTextContent "+ret);
            return ret;
        }
    }


    /**
     * Retrieve the value of a path property from the Xml representation. If the property is not found in the Xml document, the eclipse persistent property of the same key is read and migrated to the
     * xml representation.
     * 
     * @param key
     * @return the array of strings representing paths
     * @throws CoreException
     */
    private synchronized String[] getPathPropertyFromXml(QualifiedName key) throws CoreException {
        traceFunc("getPathPropertyFromXml");
        synchronized (this) {
            try {
                Node propertyNode = findPropertyNodeInXml(PYDEV_NATURE_PATH_PROPERTY, key);
    
                if (propertyNode != null) {
                    traceFunc("END getPathPropertyFromXml");
                    return getChildValuesWithType(propertyNode, PYDEV_PATH);
                }
    
                traceFunc("END getPathPropertyFromXml (null)");
                return null;
            } catch (Exception e) {
                traceFunc("END getPathPropertyFromXml (EXCEPTION)");
                IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
                throw new CoreException(status);
            }
        }
    }

    /**
     * Store a path property in the xml document and request the storage of changes. If the paths parameter is null the property is removed from the document.
     * 
     * @param key
     * @param paths
     * @throws CoreException
     */
    private synchronized void setPathPropertyToXml(QualifiedName key, String[] paths, boolean store) throws CoreException {
        traceFunc("setPathPropertyToXml");
        synchronized (this) {
            try {
                Node oldChild = findPropertyNodeInXml(PYDEV_NATURE_PATH_PROPERTY, key);
                if (oldChild != null && paths == null) {
                    getRootNodeInXml().removeChild(oldChild);
                    doStore();
                } else if (paths != null) {
                    // The property is not in the file and we need to set it
                    Node property = document.createElement(PYDEV_NATURE_PATH_PROPERTY);
                    Node propertyName = document.createAttribute(PYDEV_NATURE_PROPERTY_NAME);
                    propertyName.setNodeValue(getKeyString(key));
                    property.getAttributes().setNamedItem(propertyName);
                    addChildValuesWithType(property, PYDEV_PATH, paths);
                    if (oldChild == null) {
                        getRootNodeInXml().appendChild(property);
                    } else {
                        getRootNodeInXml().replaceChild(property, oldChild);
                    }
                    if (store) {
                        doStore();
                    }
                }
    
            } catch (Exception e) {
                traceFunc("END setPathPropertyToXml (EXCEPTION)");
                IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
                throw new CoreException(status);
            }
        }
        traceFunc("END setPathPropertyToXml");
    }

    /**
     * Serializes an Xml document to an array of bytes.
     * 
     * @param doc
     * @return the array of bytes representing the Xml document
     * @throws IOException
     * @throws TransformerException
     */
    private synchronized byte[] serializeDocument(Document doc) throws IOException, TransformerException {
        traceFunc("serializeDocument");
        synchronized (this) {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
    
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
    
            DOMSource source = new DOMSource(doc);
            StreamResult outputTarget = new StreamResult(s);
            transformer.transform(source, outputTarget);
    
            traceFunc("END serializeDocument");
            return s.toByteArray();
        }
    }

    /**
     * Stores the internal Xml representation as a project resource.
     * 
     */
    private synchronized void doStore() {
        traceFunc("doStore");
        synchronized (this) {
            try {
                doStore(new NullProgressMonitor());
            } catch (Exception e) {
                throw new RuntimeException("Error storing pydev project descriptor:"+project, e);
            }
        }
        traceFunc("END doStore");
    }

    public void resourceChanged(IResourceChangeEvent event) {
        traceFunc("resourceChanged -- "+project.getName());
        if(inInit){
            traceFunc("END resourceChanged (inInit)");
            return;
        }
        
        //not synched -- called when a file is changed
        boolean doRebuild = false;
        if (onIgnoreRefresh != 0) {
            traceFunc("END resourceChanged (on ignore refresh)");
            return; 
        }
        
        if (onStoreJob != 0) {
            traceFunc("END resourceChanged (on store)");
            return; // we're on a store job from the store itself, this means that we must
            // call the rebuildPath programatically (and that the resource change
            // is actually already reflected in our document and file)
        }
        
        if(!project.isOpen()){
            traceFunc("END resourceChanged (!open)");
            return; // project closed... no need to do anything if the project is closed
        }
        
        IResourceDelta eventDelta = event.getDelta();
        if(eventDelta == null){
            traceFunc("END resourceChanged (eventDelta == null)");
            return; //no delta here... move on
        }
        
        IResourceDelta delta = eventDelta.findMember(xmlFile.getFullPath());
        if (delta != null) {
            if (xmlFile.getModificationStamp() != modStamp) {
                try {
                    if (loadFromFile()) {
                        doRebuild = true;
                    }
                } catch (CoreException e) {
                    traceFunc("END resourceChanged (EXCEPTION)");
                    throw new RuntimeException(e);
                }
            }
        }
        if(doRebuild){
            PythonNature nature = PythonNature.getPythonNature(project);
            if (nature != null) {
                nature.rebuildPath();
            }
        }
        traceFunc("END resourceChanged -- rebuilt:"+doRebuild);
    }

    /**
     * This is the function that actually stores the contents of the xml into the file with the configurations.
     */
    private synchronized IStatus doStore(IProgressMonitor monitor) throws IOException, TransformerException, CoreException {
        traceFunc("doStore");
        if(inInit){
            traceFunc("END doStore (inInit)");
            return Status.OK_STATUS;
        }
        synchronized (this) {
            try {
                // that's because while we're in a store job, we don't want to listen to resource changes (we just
                // want resource changes from the user).
                if (onStoreJob != 0) {
                    traceFunc("END doStore (already in store)");
                    return Status.OK_STATUS;
                }
    
                onStoreJob++;
                if (document == null) {
                    traceFunc("END doStore (document == null)");
                    return new Status(Status.ERROR, "PythonNatureStore.doStore", -2, "document == null", new RuntimeException("document == null"));
                }
    
                final ByteArrayInputStream is = new ByteArrayInputStream(serializeDocument(document));
    
                PythonNatureStoreJob job = new PythonNatureStoreJob("Save .pydevproject", is);
                
                if (ProjectModulesManager.IN_TESTS){
                    job.run(new NullProgressMonitor());
                }else{
                    job.schedule();
                }
                traceFunc("END doStore");
                return Status.OK_STATUS;
            } finally {
                onStoreJob--;
            }
        }
    }

    public void startInit() {
        inInit = true;
    }

    public void endInit() {
        inInit = false;
        doStore();
    }





}
