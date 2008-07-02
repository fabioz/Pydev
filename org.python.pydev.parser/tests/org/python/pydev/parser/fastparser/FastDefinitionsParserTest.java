package org.python.pydev.parser.fastparser;

import java.io.File;

import junit.framework.TestCase;

import org.python.pydev.core.REF;
import org.python.pydev.core.performanceeval.Timer;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.NameTok;

public class FastDefinitionsParserTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    
    public static void main(String[] args) {
        try {
            FastDefinitionsParserTest test = new FastDefinitionsParserTest();
            test.setUp();
            test.testDefinitionsParser9();
//            Timer timer = new Timer();
//            test.parseFilesInDir(new File("D:/bin/Python251/Lib/site-packages/wx-2.8-msw-unicode"), true);
//            test.parseFilesInDir(new File("D:/bin/Python251/Lib/"), false);
//            timer.printDiff();
            
            test.tearDown();
            
            
            junit.textui.TestRunner.run(FastDefinitionsParserTest.class);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    
    
    /**
     * @param file
     */
    private void parseFilesInDir(File file, boolean recursive) {
        assertTrue("Directory "+file+" does not exist", file.exists());
        assertTrue(file.isDirectory());
        
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if(f.getAbsolutePath().toLowerCase().endsWith(".py")){
                String fileContents = REF.getFileContents(f);
                FastDefinitionsParser.parse(fileContents);
                
            }else if(recursive && f.isDirectory()){
                parseFilesInDir(f, recursive);
            }
        }
    }
    
    
    public void testDefinitionsParser() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:pass");
        assertEquals(1, m.body.length);
        assertEquals("Bar", ((NameTok)((ClassDef)m.body[0]).name).id);
    }
    
    public void testDefinitionsParser2() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar");
        assertEquals(1, m.body.length);
        assertEquals("Bar", ((NameTok)((ClassDef)m.body[0]).name).id);
    }
    
    public void testDefinitionsParser3() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):pass");
        assertEquals(1, m.body.length);
        assertEquals("Bar", ((NameTok)((ClassDef)m.body[0]).name).id);
    }
    
    public void testDefinitionsParser4() {
        Module m = (Module) FastDefinitionsParser.parse(
            "class Bar(object):\n" +
            "    def m1(self):pass"
        );
        assertEquals(1, m.body.length);
        ClassDef classDef = (ClassDef)m.body[0];
        assertEquals("Bar", ((NameTok)classDef.name).id);
        
        FunctionDef funcDef = (FunctionDef)classDef.body[0];
        assertEquals("m1", ((NameTok)funcDef.name).id);
    }
    
    public void testDefinitionsParser5() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar(object):\n" +
                "    def m1(self):pass\n"+
                "def m2(self):pass\n"
        );
        assertEquals(2, m.body.length);
        ClassDef classDef = (ClassDef)m.body[0];
        assertEquals("Bar", ((NameTok)classDef.name).id);
        
        FunctionDef funcDef = (FunctionDef)classDef.body[0];
        assertEquals("m1", ((NameTok)funcDef.name).id);
        
        funcDef = (FunctionDef)m.body[1];
        assertEquals("m2", ((NameTok)funcDef.name).id);
    }
    
    public void testDefinitionsParser6() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar(object):\n" +
                "    class Zoo(object):\n" +
                "        def m1(self):pass\n"+
                "def m2(self):pass\n"
        );
        assertEquals(2, m.body.length);
        ClassDef classDefBar = (ClassDef)m.body[0];
        assertEquals("Bar", ((NameTok)classDefBar.name).id);
        
        ClassDef classDefZoo = (ClassDef)classDefBar.body[0];
        assertEquals("Zoo", ((NameTok)classDefZoo.name).id);
        
        assertEquals("m1", ((NameTok)((FunctionDef)classDefZoo.body[0]).name).id);
        
        assertEquals("m2", ((NameTok)((FunctionDef)m.body[1]).name).id);
    }
    
    public void testDefinitionsParser7() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar(object):\n" +
                "    class Zoo(object):\n" +
                "        class PPP(self):pass\n"+
                
                "class Bar2(object):\n" +
                "    class Zoo2(object):\n" +
                "        class PPP2(self):pass\n"
        );
        assertEquals(2, m.body.length);
        
        ClassDef classDefBar = (ClassDef)m.body[0];
        assertEquals("Bar", ((NameTok)classDefBar.name).id);
        ClassDef classDefZoo = (ClassDef)classDefBar.body[0];
        assertEquals("Zoo", ((NameTok)classDefZoo.name).id);
        assertEquals("PPP", ((NameTok)((ClassDef)classDefZoo.body[0]).name).id);
        
        //check the 2nd leaf
        classDefBar = (ClassDef)m.body[1];
        assertEquals("Bar2", ((NameTok)classDefBar.name).id);
        classDefZoo = (ClassDef)classDefBar.body[0];
        assertEquals("Zoo2", ((NameTok)classDefZoo.name).id);
        assertEquals("PPP2", ((NameTok)((ClassDef)classDefZoo.body[0]).name).id);
    }

    

    public void testDefinitionsParser8() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar(object):\n" +
                "    class Zoo(object):\n" +
                "        def m1(self):pass\n"+
                "        def m2(self):pass\n"+
                "            def m3(self):pass\n"+
                "def mGlobal(self):pass\n"
        );
        assertEquals(2, m.body.length);
        ClassDef classDefBar = (ClassDef)m.body[0];
        assertEquals("Bar", ((NameTok)classDefBar.name).id);
        assertEquals("mGlobal", ((NameTok)((FunctionDef)m.body[1]).name).id);
        
        ClassDef classDefZoo = (ClassDef)classDefBar.body[0];
        assertEquals("Zoo", ((NameTok)classDefZoo.name).id);
        
        assertEquals(2, classDefZoo.body.length);
        assertEquals("m1", ((NameTok)((FunctionDef)classDefZoo.body[0]).name).id);
        
    }
    
    
    public void testDefinitionsParser9() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar(object):\n" +
                "    class \tZoo\t(object):\n" +
                "        def     m1(self):pass\n"+
                "        def m2(self):pass\n"+
                "            def m3(self):pass\n"+
                "def mGlobal(self):pass\n"
        );
        assertEquals(2, m.body.length);
        ClassDef classDefBar = (ClassDef)m.body[0];
        assertEquals("Bar", ((NameTok)classDefBar.name).id);
        assertEquals("mGlobal", ((NameTok)((FunctionDef)m.body[1]).name).id);
        
        ClassDef classDefZoo = (ClassDef)classDefBar.body[0];
        assertEquals("Zoo", ((NameTok)classDefZoo.name).id);
        
        assertEquals(2, classDefZoo.body.length);
        assertEquals("m1", ((NameTok)((FunctionDef)classDefZoo.body[0]).name).id);
        
    }
    
    
    public void testDefinitionsParser10() {
        Module m = (Module) FastDefinitionsParser.parse(
                "" //empty
        );
        assertEquals(0, m.body.length);
    }
    
    

    
}
