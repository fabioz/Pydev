package com.python.pydev.analysis.additionalinfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.python.pydev.analysis.actions.AdditionalInfoAndIInfo;

import junit.framework.TestCase;

public class InfoFactoryTest extends TestCase{

    public void testInfoFactory() throws Exception{
        InfoFactory infoFactory = new InfoFactory(new AdditionalInfoAndIInfo(null, null));
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        Element root = document.createElement("root");
        IMemento memento = new XMLMemento(document, root);
        infoFactory.saveState(memento);
        
        assertNull(infoFactory.createElement(memento));
        
        ClassInfo info = new ClassInfo();
        infoFactory = new InfoFactory(new AdditionalInfoAndIInfo(null, info));
        infoFactory.saveState(memento);
        assertNull(infoFactory.createElement(memento));
    }
}
