/*
 * Created on Apr 11, 2006
 */
package com.python.pydev.refactoring.refactorer;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.ModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

public class ClassHierarchySearch extends AdditionalInfoTestsBase  {

    public static void main(String[] args) {
        try {
            ClassHierarchySearch test = new ClassHierarchySearch();
            test.setUp();
            test.tearDown();
            
            junit.textui.TestRunner.run(ClassHierarchySearch.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private Refactorer refactorer;

    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(false);
        refactorer = new Refactorer();
        SourceModule.TESTING = true;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testFindHierarchy() {
        String str ="" +
        "import pickle\n" +
        "class Bar(pickle.Pickler):\n" +
        "    pass\n" +
        "class Foo(Bar):\n" +
        "    pass\n" +
        "\n" +
        "";
        Document doc = new Document(str);
        PySelection ps = new PySelection(doc, 1, 9);
        
        RefactoringRequest request = new RefactoringRequest(null, ps, nature);
        request.moduleName = "foo";
        
        List<AbstractAdditionalInterpreterInfo> additionalInfo = AdditionalProjectInterpreterInfo.getAdditionalInfo(nature);
        additionalInfo.get(0).addAstInfo(request.getAST(), "foo", nature, false);
        ModulesManager modulesManager = (ModulesManager) nature.getAstManager().getModulesManager();
        SourceModule mod = (SourceModule) AbstractModule.createModule(request.getAST(), null, "foo");
        modulesManager.doAddSingleModule(new ModulesKey("foo", null), mod);
        
        
        HierarchyNodeModel node = refactorer.findClassHierarchy(request);
        assertEquals("Bar", node.name);
        assertEquals("foo", node.moduleName);
        
        assertIsIn("Pickler", "pickle", node.parents);
        assertIsIn("Foo", "foo", node.children);
    }

    private void assertIsIn(String name, String modName, List<HierarchyNodeModel> parents) {
        for (HierarchyNodeModel model : parents) {
            if(model.name.equals(name) && model.moduleName.equals(modName)){
                return;
            }
        }
        fail("Unable to find node with name:"+name+" mod:"+modName);
    }
}
