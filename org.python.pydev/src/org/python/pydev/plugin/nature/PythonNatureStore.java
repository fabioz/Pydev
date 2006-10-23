package org.python.pydev.plugin.nature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/**
 * This class stores PythonNature and PythonPathNature properties inside the 
 * project in a file instead of persistent properties. This allows PYTHONPATH and 
 * Python project version to be checked in into version control systems.
 * 
 * @author Gergely Kis <gergely.kis@gmail.com>
 * 
 */

class PythonNatureStore {
    final static String STORE_FILE_NAME = ".pydevproject";

    final static String PYDEV_PROJECT_DESCRIPTION = "pydev_project";

    final static String PYDEV_NATURE_PROPERTY = "pydev_property";

    final static String PYDEV_NATURE_PROPERTY_NAME = "name";

    private PythonNature nature;

    /**
     * This is the dom document that is used to manipulate the xml info.
     */
    private Document document = null;

    protected PythonNatureStore(PythonNature nature) {
        assert (nature != null);
        this.nature = nature;
    }

    public PythonNature getNature() {
        return nature;
    }

    public String getProperty(QualifiedName key) throws CoreException {
        synchronized (this) {
            if (document == null)
                loadFromFile();
        }
        synchronized (document) {
            return getPropertyFromXml(key);
        }
    }

    public void setProperty(QualifiedName key, String value) throws CoreException {
        synchronized (this) {
            if (document == null)
                loadFromFile();
        }
        synchronized (document) {
            setPropertyToXml(key, value);
        }
    }

    private void loadFromFile() throws CoreException {
        try {
            IFile xmlFile = nature.getProject().getFile(STORE_FILE_NAME);
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            xmlFile.refreshLocal(IResource.DEPTH_ZERO, null);
            if (!xmlFile.exists()) {
                document = parser.newDocument();
                synchronized (document) {
                    ProcessingInstruction version = document.createProcessingInstruction("eclipse-pydev", "version=\"1.0\""); //$NON-NLS-1$ //$NON-NLS-2$
                    document.appendChild(version);
                    Element configRootElement = document.createElement(PYDEV_PROJECT_DESCRIPTION);
                    document.appendChild(configRootElement);
                    scheduleStoreJob();
                }
            } else {
                document = parser.parse(xmlFile.getContents());
            }
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
            throw new CoreException(status);
        }
    }

    private String getPropertyFromXml(QualifiedName key) throws CoreException {
        try {
            NodeList nodeList = document.getElementsByTagName(PYDEV_PROJECT_DESCRIPTION);
            if (nodeList != null && nodeList.getLength() > 0) {
                Node root = nodeList.item(0);
                NodeList childNodes = root.getChildNodes();
                if (childNodes != null && childNodes.getLength() > 0) {
                    String keyString = key.getQualifier() != null ? key.getQualifier() : "";
                    keyString = keyString + "." + key.getLocalName();
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node child = childNodes.item(i);
                        if (child.getNodeName().equals(PYDEV_NATURE_PROPERTY)) {
                            NamedNodeMap attrs = child.getAttributes();
                            if (attrs != null && attrs.getLength() > 0) {
                                String name = attrs.getNamedItem(PYDEV_NATURE_PROPERTY_NAME).getNodeValue();
                                if (name != null && name.equals(keyString)) {
                                    return child.getTextContent();
                                }
                            }
                        }
                    }
                }
            }

            // Nothing found, try to migrate from persistent property
            String propertyVal = nature.getProject().getPersistentProperty(key);
            if (propertyVal != null) {
                setPropertyToXml(key, propertyVal);
                nature.getProject().setPersistentProperty(key, (String) null);
                return propertyVal;
            }

            return null;
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
            throw new CoreException(status);
        }
    }

    private void setPropertyToXml(QualifiedName key, String value) throws CoreException {
        try {
            NodeList nodeList = document.getElementsByTagName(PYDEV_PROJECT_DESCRIPTION);
            String keyString = key.getQualifier() != null ? key.getQualifier() : "";
            keyString = keyString + "." + key.getLocalName();
            if (nodeList != null && nodeList.getLength() > 0) {
                Node root = nodeList.item(0);
                NodeList childNodes = root.getChildNodes();
                if (childNodes != null && childNodes.getLength() > 0) {
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node child = childNodes.item(i);
                        if (child.getNodeName().equals(PYDEV_NATURE_PROPERTY)) {
                            NamedNodeMap attrs = child.getAttributes();
                            if (attrs != null && attrs.getLength() > 0) {
                                String name = attrs.getNamedItem(PYDEV_NATURE_PROPERTY_NAME).getNodeValue();
                                if (name != null && name.equals(keyString)) {
                                    if (value == null) {
                                        // remove child from file
                                        root.removeChild(child);
                                    } else {
                                        child.setTextContent(value);
                                    }
                                    scheduleStoreJob();
                                    return;
                                }
                            }
                        }
                    }
                }
                if (value != null) {
                    // The property is not in the file and we need to set it
                    Node property = document.createElement(PYDEV_NATURE_PROPERTY);
                    Node propertyName = document.createAttribute(PYDEV_NATURE_PROPERTY_NAME);
                    propertyName.setNodeValue(keyString);
                    property.getAttributes().setNamedItem(propertyName);
                    property.setTextContent(value);
                    root.appendChild(property);
                    scheduleStoreJob();
                    return;
                }

            }
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, "PythonNatureStore", -1, e.toString(), e);
            throw new CoreException(status);
        }
    }

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

    private void scheduleStoreJob() {
        Job storeJob = new Job("Store Pydev Project Descriptor") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask("Storing Pydev Project Descriptor", 10);
                    if (document == null) {
                        return new Status(Status.ERROR, "PythonNatureStore.scheduleStoreJob", -2, "document == null", null);
                    }
                    IFile xmlFile = nature.getProject().getFile(STORE_FILE_NAME);
                    ByteArrayInputStream is = null;
                    synchronized (document) {
                        is = new ByteArrayInputStream(serializeDocument(document));
                    }
                    if (!xmlFile.exists()) {
                        xmlFile.create(is, true, monitor);
                    } else {
                        xmlFile.setContents(is, true, false, monitor);
                    }
                    xmlFile.refreshLocal(IResource.DEPTH_ZERO, monitor);

                    return Status.OK_STATUS;
                } catch (CoreException e) {
                    return e.getStatus();
                } catch (Exception e) {
                    return new Status(Status.ERROR, "PythonNatureStore.scheduleStoreJob", -1, e.getMessage(), e);
                } finally {
                    monitor.done();
                }
            }
        };

        storeJob.setRule(nature.getProject());
        storeJob.schedule();
    }

}
