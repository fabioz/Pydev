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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
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

class PythonNatureStore implements IResourceChangeListener {

    final static String STORE_FILE_NAME = ".pydevproject";

    final static String PYDEV_PROJECT_DESCRIPTION = "pydev_project";

    final static String PYDEV_NATURE_PROPERTY = "pydev_property";

    final static String PYDEV_NATURE_PATH_PROPERTY = "pydev_pathproperty";

    final static String PYDEV_PATH = "path";

    final static String PYDEV_NATURE_PROPERTY_NAME = "name";

    private IProject project = null;

    private IFile xmlFile = null;

    long modStamp = IFile.NULL_STAMP;

    /**
     * 0 means we're not in a store job
     */
    private int onStoreJob = 0;

    /**
     * This is the dom document that is used to manipulate the xml info.
     */
    private Document document = null;

    /**
     * Sets the projaect to be used.
     * 
     * @param project the project related to this store.
     * 
     * Note: the side-effect of this method is that the contents of the xml file are either created (if the file still does not exist) or just loaded.
     */
    public void setProject(IProject project) {
        this.project = project;
        this.xmlFile = project.getFile(STORE_FILE_NAME);
        try {
            loadFromFile();
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        if (!ProjectModulesManager.IN_TESTS) {
            project.getWorkspace().addResourceChangeListener(this);
        }
    }

    /**
     * Retrieve a path property as a combined string with | character as path separator
     * 
     * @param key
     * @return the combined path string or null if the property is not set.
     * @throws CoreException
     */
    public String getPathProperty(QualifiedName key) throws CoreException {
        synchronized (xmlFile) {
            return getPathStringFromArray(getPathPropertyFromXml(key));
        }
    }

    /**
     * Set a path property. If the value is null the property is removed.
     * 
     * @param key the name of the property
     * @param value the combined string of paths with | character as separator
     * @throws CoreException
     */
    public void setPathProperty(QualifiedName key, String value) throws CoreException {
        synchronized (xmlFile) {
            setPathPropertyToXml(key, getArrayFromPathString(value), true);
        }
    }

    /**
     * Loads the Xml representation of the PythonNature properties from the project resource. If the project resource does not exist then an empty representation is created, and its storage is
     * requested in the project folder.
     * 
     * @return true if some change has actually happened in the file and false otherwise
     * @throws CoreException
     */
    private boolean loadFromFile() throws CoreException {
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            xmlFile.refreshLocal(IResource.DEPTH_ZERO, null);
            if (!xmlFile.exists()) {
                if (document != null) {
                    // Someone removed the project descriptor, store it from the memory model
                    scheduleStoreJob();
                    return true;
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
                    scheduleStoreJob();
                    return true;
                }
            } else {
                document = parser.parse(xmlFile.getContents());
                modStamp = xmlFile.getModificationStamp();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
            throw new CoreException(status);
        }
    }

    private void migrateProperty(QualifiedName key) throws CoreException {
        // Nothing found, try to migrate from persistent property
        String propertyVal = project.getPersistentProperty(key);
        if (propertyVal != null) {
            setPropertyToXml(key, propertyVal, false);
            project.setPersistentProperty(key, (String) null);
        }
    }

    private void migratePath(QualifiedName key) throws CoreException {
        // Try to migrate from persistent property
        String[] propertyVal = getArrayFromPathString(project.getPersistentProperty(key));
        if (propertyVal != null) {
            // set in the xml
            setPathPropertyToXml(key, propertyVal, false);
            // and remove from the project
            project.setPersistentProperty(key, (String) null);
        }
    }

    /**
     * Get the root node of the project description
     * 
     * @return the root Node object
     * @throws CoreException if root node is not present
     */
    private Node getRootNodeInXml() throws CoreException {
        NodeList nodeList = document.getElementsByTagName(PYDEV_PROJECT_DESCRIPTION);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0);
        }
        throw new CoreException(new Status(IStatus.ERROR, "PythonNatureStore", -3, "Corrupted .pydevproject", null));
    }

    /**
     * Assemble a string representation of a QualifiedName typed key
     * 
     * @param key
     * @return the assembled string key representation
     */
    private String getKeyString(QualifiedName key) {
        String keyString = key.getQualifier() != null ? key.getQualifier() : "";
        return keyString + "." + key.getLocalName();
    }

    /**
     * Finds a property node as a direct child of the root node with the specified type and key.
     * 
     * @param type
     * @param key
     * @return The property node or null if a node with the supplied key and type cannot be found.
     * @throws CoreException
     */
    private Node findPropertyNodeInXml(String type, QualifiedName key) throws CoreException {
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
                            return child;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the text contents of a nodes' children. The children shall have the specified type.
     * 
     * @param node
     * @param type
     * @return the array of strings with the text contents or null if the node has no children.
     */
    private String[] getChildValuesWithType(Node node, String type) {
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
            return result.toArray(retval);
        }
        return null;
    }

    /**
     * Add children to a node with specified type and text contents. For each values array element a new child is created.
     * 
     * @param node
     * @param type
     * @param values
     */
    private void addChildValuesWithType(Node node, String type, String[] values) {
        assert (node != null);
        assert (values != null);
        assert (type != null);
        for (int i = 0; i < values.length; i++) {
            Node child = document.createElement(type);
            setTextContent(values[i], child);
            node.appendChild(child);
        }
    }

    /**
     * Convert an array of path strings to a single string separated by | characters.
     * 
     * @param pathArray
     * @return the assembled string of paths or null if the input was null
     */
    private String getPathStringFromArray(String[] pathArray) {
        if (pathArray != null) {
            StringBuffer s = new StringBuffer("");
            for (int i = 0; i < pathArray.length; i++) {
                if (i > 0) {
                    s.append('|');
                }
                s.append(pathArray[i]);
            }
            return s.toString();
        }
        return null;
    }

    /**
     * Convert a single string of paths separated by | characters to an array of strings.
     * 
     * @param pathString
     * @return the splitted array of strings or null if the input was null
     */
    private String[] getArrayFromPathString(String pathString) {
        if (pathString != null) {
            return pathString.split("\\|");
        }
        return null;
    }

    /**
     * Retrieve a string property with the specified key from the Xml representation. If the key is not in the Xml representation, the eclipse persistent property of the same key is read and migrated
     * into the Xml representation.
     * 
     * @param key
     * @return The value of the property or null if the property is not set.
     * @throws CoreException
     */
    public String getPropertyFromXml(QualifiedName key) throws CoreException {
        try {
            Node propertyNode = findPropertyNodeInXml(PYDEV_NATURE_PROPERTY, key);

            if (propertyNode != null) {
                return getTextContent(propertyNode);
            }

            return null;
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
            throw new CoreException(status);
        }
    }

    /**
     * Store a string property in the Xml representation and request the storage of the changes. If the value is is null, the property is removed.
     * 
     * @param key
     * @param value
     * @throws CoreException
     */
    public void setPropertyToXml(QualifiedName key, String value, boolean scheduleStore) throws CoreException {
        try {
            Node child = findPropertyNodeInXml(PYDEV_NATURE_PROPERTY, key);
            if (child != null) {
                if (value == null) {
                    // remove child from file
                    getRootNodeInXml().removeChild(child);
                } else {
                    setTextContent(value, child);
                }
                scheduleStoreJob();
            } else if (value != null) {
                // The property is not in the file and we need to set it
                Node property = document.createElement(PYDEV_NATURE_PROPERTY);
                Node propertyName = document.createAttribute(PYDEV_NATURE_PROPERTY_NAME);
                propertyName.setNodeValue(getKeyString(key));
                property.getAttributes().setNamedItem(propertyName);
                setTextContent(value, property);
                getRootNodeInXml().appendChild(property);

                if (scheduleStore) {
                    scheduleStoreJob();
                }
            }

        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
            throw new CoreException(status);
        }
    }

    /**
     * This function was gotten as a copy of the Node.setTextContent, because this function 
     * is not available in java 1.4
     */
    public void setTextContent(String textContent, Node self) throws DOMException {
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
    
    
    public String getTextContent(Node self) throws DOMException {
        StringBuffer fBufferStr = new StringBuffer();
        Node child = self.getFirstChild();
        if (child != null) {
            Node next = child.getNextSibling();
            if (next == null) {
                if(hasTextContent(child)){
                    String nodeValue = child.getNodeValue();
                    if(nodeValue != null){
                        return nodeValue;
                    }
                }
                return "";
            }
            if (fBufferStr == null){
                fBufferStr = new StringBuffer();
            }
            else {
                fBufferStr.setLength(0);
            }
            getTextContent(fBufferStr, self);
            return fBufferStr.toString();
        }
        return "";
    }

    
    // internal method taking a StringBuffer in parameter
    void getTextContent(StringBuffer buf, Node self) throws DOMException {
        Node child = self.getFirstChild();
        while (child != null) {
            if (hasTextContent(child)) {
                getTextContent(buf, child);
            }
            child = child.getNextSibling();
        }
    }

    // internal method returning whether to take the given node's text content
    final boolean hasTextContent(Node child) {
        return child.getNodeType() != Node.COMMENT_NODE &&
            child.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE;
    }


    /**
     * Retrieve the value of a path property from the Xml representation. If the property is not found in the Xml document, the eclipse persistent property of the same key is read and migrated to the
     * xml representation.
     * 
     * @param key
     * @return the array of strings representing paths
     * @throws CoreException
     */
    private String[] getPathPropertyFromXml(QualifiedName key) throws CoreException {
        try {
            Node propertyNode = findPropertyNodeInXml(PYDEV_NATURE_PATH_PROPERTY, key);

            if (propertyNode != null) {
                return getChildValuesWithType(propertyNode, PYDEV_PATH);
            }

            return null;
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
            throw new CoreException(status);
        }
    }

    /**
     * Store a path property in the xml document and request the storage of changes. If the paths parameter is null the property is removed from the document.
     * 
     * @param key
     * @param paths
     * @throws CoreException
     */
    private void setPathPropertyToXml(QualifiedName key, String[] paths, boolean scheduleStore) throws CoreException {
        try {
            Node oldChild = findPropertyNodeInXml(PYDEV_NATURE_PATH_PROPERTY, key);
            if (oldChild != null && paths == null) {
                getRootNodeInXml().removeChild(oldChild);
                scheduleStoreJob();
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
                if (scheduleStore) {
                    scheduleStoreJob();
                }
            }

        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
            throw new CoreException(status);
        }
    }

    /**
     * Serializes an Xml document to an array of bytes.
     * 
     * @param doc
     * @return the array of bytes representing the Xml document
     * @throws IOException
     * @throws TransformerException
     */
    private byte[] serializeDocument(Document doc) throws IOException, TransformerException {
        ByteArrayOutputStream s = new ByteArrayOutputStream();

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
        transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$

        DOMSource source = new DOMSource(doc);
        StreamResult outputTarget = new StreamResult(s);
        transformer.transform(source, outputTarget);

        return s.toByteArray();
    }

    /**
     * Schedules a job to store the internal Xml representation as a project resource.
     * 
     */
    private void scheduleStoreJob() {
        if (ProjectModulesManager.IN_TESTS) {
            try {
                doStore(new NullProgressMonitor());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (TransformerException e) {
                throw new RuntimeException(e);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }

        } else {
            Job storeJob = new Job("Store Pydev Project Descriptor") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask("Storing Pydev Project Descriptor", 10);
                        return doStore(monitor);
                    } catch (CoreException e) {
                        return e.getStatus();
                    } catch (Exception e) {
                        return new Status(Status.ERROR, "PythonNatureStore.scheduleStoreJob", -1, e.getMessage(), e);
                    } finally {
                        monitor.done();
                    }
                }
            };

            storeJob.setRule(project);
            storeJob.schedule();
        }
    }

    public void resourceChanged(IResourceChangeEvent event) {
        if (onStoreJob != 0) {
            return; // we're on a store job from the store itself, this means that we must
            // call the rebuildPath programatically (and that the resource change
            // is actually already reflected in our document and file)
        }
        IResourceDelta delta = event.getDelta().findMember(xmlFile.getFullPath());
        if (delta != null) {
            if (xmlFile.getModificationStamp() != modStamp) {
                Job loadJob = new Job("Reload Pydev Project Descriptor") {

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            monitor.beginTask("Loading Pydev Project Descriptor", 10);

                            synchronized (xmlFile) {
                                if (loadFromFile()) {
                                    PythonNature nature = PythonNature.getPythonNature(project);
                                    if (nature != null) {
                                        nature.rebuildPath();
                                    }
                                }
                            }

                            return Status.OK_STATUS;
                        } catch (CoreException e) {
                            return e.getStatus();
                        } catch (Exception e) {
                            return new Status(Status.ERROR, "PythonNatureStore.loadJob", -1, e.getMessage(), e);
                        } finally {
                            monitor.done();
                        }
                    }
                };

                loadJob.setRule(project);
                loadJob.schedule();
            }
        }
    }

    /**
     * This is the function that actually stores the contents of the xml into the file with the configurations.
     */
    private IStatus doStore(IProgressMonitor monitor) throws IOException, TransformerException, CoreException {
        try {
            // that's because while we're in a store job, we don't want to listen to resource changes (we just
            // want resource changes from the user).
            onStoreJob++;
            synchronized (xmlFile) {
                if (document == null) {
                    return new Status(Status.ERROR, "PythonNatureStore.scheduleStoreJob", -2, "document == null", null);
                }

                ByteArrayInputStream is = new ByteArrayInputStream(serializeDocument(document));

                if (!xmlFile.exists()) {
                    xmlFile.create(is, true, monitor);
                    modStamp = xmlFile.getModificationStamp();
                } else {
                    xmlFile.setContents(is, true, false, monitor);
                    modStamp = xmlFile.getModificationStamp();
                }
                xmlFile.refreshLocal(IResource.DEPTH_ZERO, monitor);
            }
            return Status.OK_STATUS;
        } finally {
            onStoreJob--;
        }
    }

}
