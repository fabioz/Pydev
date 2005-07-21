/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Import;
import org.python.parser.ast.Name;
import org.python.parser.ast.VisitorBase;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.IMessage;
import com.python.pydev.analysis.Message;

public class OcurrencesVisitor extends VisitorBase{

    private PythonNature nature;
    private String moduleName;
    private Stack stack = new Stack();
    private List messages = new ArrayList();
    
    public static class Found{
        public IToken tok;
        public boolean used = false;
        Found(IToken tok){
            this.tok = tok;
        }
    }

    public IMessage[] getMessages() {
        endScope();
        return (IMessage[]) messages.toArray(new IMessage[0]);
    }
    
    public OcurrencesVisitor(PythonNature nature, String moduleName) {
        this.nature = nature;
        this.moduleName = moduleName;
        startScope(); //initial scope 
    }
    
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }
    
    public Object visitClassDef(ClassDef node) throws Exception {
        startScope();
        Object object = super.visitClassDef(node);
        endScope();
        return object;
    }

    public Object visitFunctionDef(FunctionDef node) throws Exception {
        startScope();
        Object object = super.visitFunctionDef(node);
        endScope();
        return object;
    }
    
    public Object visitImport(Import node) throws Exception {
        List list = AbstractVisitor.makeImportToken(node, null, moduleName);
        Map m = (Map) stack.peek();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            IToken o = (IToken) iter.next();
            String representation = o.getRepresentation();
            m.put(representation, new Found(o));
        }
        return null;
    }
    
    private void startScope() {
        stack.push(new HashMap());
    }
    
    private void endScope() {
        Map m = (Map) stack.pop(); //clear the last
        for (Iterator iter = m.values().iterator(); iter.hasNext();) {
            Found f = (Found) iter.next();
            if(!f.used){
                addMessage(IMessage.WARNING, IMessage.UNUSED_IMPORT, f);
            }
        }
    }

    /**
     * Visiting some name
     * 
     * @see org.python.parser.ast.VisitorIF#visitName(org.python.parser.ast.Name)
     */
    public Object visitName(Name node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if (node.ctx == Name.Load) {
            markRead(node);
        }
        return null;
    }

    private void markRead(Name node) {
        for (Iterator iter = stack.iterator(); iter.hasNext();) {
            Map m = (Map) iter.next();
            Found f = (Found) m.get(node.id);
            if(f != null){
                f.used = true;
            }
        }
    }

    private String[] getAllDefinedTokens() {
        List strings = new ArrayList();
        for (Iterator iter = stack.iterator(); iter.hasNext();) {
            Map m = (Map) iter.next();
            for (Iterator iterator = m.keySet().iterator(); iterator.hasNext();) {
                strings.add(iterator.next());
            }
        }
        return (String[]) strings.toArray(new String[0]);
    }

    private void addMessage(int type, int subType, Found f) {
        messages.add(new Message(type, subType, f.tok.getRepresentation()));
    }
}
