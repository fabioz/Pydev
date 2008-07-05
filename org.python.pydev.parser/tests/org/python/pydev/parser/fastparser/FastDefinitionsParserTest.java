package org.python.pydev.parser.fastparser;

import java.io.File;

import junit.framework.TestCase;

import org.python.pydev.core.REF;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
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
            test.testDefinitionsParser11();
            
            
            //only loading files
            //java6: time elapsed: 0.593
            //java5: time elapsed: 0.984
            
            //fast parser
            //java6: time elapsed: 0.844
            //java5: time elapsed: 1.375
            
            //regular parser
            //java6: time elapsed: 9.25
            //java5: time elapsed: 6.89
            
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
                try{
                    FastDefinitionsParser.parse(fileContents);
                }catch(Exception e){
                    System.out.println("Error parsing:"+f);
//                    e.printStackTrace();
                }
                
            }else if(recursive && f.isDirectory()){
                parseFilesInDir(f, recursive);
            }
        }
    }
    
    
    public void testAttributes() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar:\n" +
                "    ATTRIBUTE = 10\n" +
                "\n" +
                "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef)m.body[0]);
        assertEquals("Bar", ((NameTok)classDef.name).id);
        assertEquals(1, classDef.body.length);
        Assign assign = (Assign) classDef.body[0];
        assertEquals(1, assign.targets.length);
        Name name = (Name) assign.targets[0];
        assertEquals("ATTRIBUTE", name.id);
    }
    
    
    public void testAttributes2() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar:\n" +
                "    XXX.ATTRIBUTE = 10\n" + //we're assigning an attribute, that's not related to the class
                "\n" +
        "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef)m.body[0]);
        assertEquals("Bar", ((NameTok)classDef.name).id);
        assertEquals(0, classDef.body.length); //no attribute
    }
    
    
    public void testAttributes3() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar:\n" +
                "    def m1(self):\n" +
                "        ATTRIBUTE = 10\n" + //local scope: don't get it
                "\n" +
        "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef)m.body[0]);
        assertEquals("Bar", ((NameTok)classDef.name).id);
        assertEquals(1, classDef.body.length); //method
        
        FunctionDef funcDef = (FunctionDef)classDef.body[0];
        assertEquals("m1", ((NameTok)funcDef.name).id);
        assertNull(funcDef.body); 
    }
    

    
    public void testAttributes4() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar:\n" +
                "    def m1(self):\n" +
                "        self.ATTRIBUTE = 10\n" + //local scope: get it because of self.
                "\n" +
        "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef)m.body[0]);
        assertEquals("Bar", ((NameTok)classDef.name).id);
        assertEquals(1, classDef.body.length); //method
        
        FunctionDef funcDef = (FunctionDef)classDef.body[0];
        assertEquals("m1", ((NameTok)funcDef.name).id);
        
        assertNull(funcDef.body[1]);
        Assign assign = (Assign) funcDef.body[0];
        assertEquals(1, assign.targets.length);
        Attribute attribute = (Attribute) assign.targets[0];
        NameTok attr = (NameTok) attribute.attr;
        assertEquals("ATTRIBUTE", attr.id.toString());
    }
    
    public void testAttributes5() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar:\n" +
                "    def m1(self):\n" +
                "        self.ATTRIBUTE0 = 10\n" + //local scope: get it because of self.
                "        self.ATTRIBUTE1 = 10\n" + //local scope: get it because of self.
                "        self.ATTRIBUTE2 = 10\n" + //local scope: get it because of self.
                "\n" +
        "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef)m.body[0]);
        assertEquals("Bar", ((NameTok)classDef.name).id);
        assertEquals(1, classDef.body.length); //method
        
        FunctionDef funcDef = (FunctionDef)classDef.body[0];
        assertEquals("m1", ((NameTok)funcDef.name).id);
        
        for(int i=0;i<3;i++){
            Assign assign = (Assign) funcDef.body[i];
            assertEquals(1, assign.targets.length);
            Attribute attribute = (Attribute) assign.targets[0];
            NameTok attr = (NameTok) attribute.attr;
            assertEquals("ATTRIBUTE"+i, attr.id.toString());
        }
        assertNull(funcDef.body[3]);
    }
    
    
    public void testAttributes6() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar:\n" +
                "    def m1(self):\n" +
                "        call(ATTRIBUTE = 10)\n" + //inside function call: don't get it
                "\n" +
        "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef)m.body[0]);
        assertEquals("Bar", ((NameTok)classDef.name).id);
        assertEquals(1, classDef.body.length); //method
        
        FunctionDef funcDef = (FunctionDef)classDef.body[0];
        assertEquals("m1", ((NameTok)funcDef.name).id);
        assertNull(funcDef.body); 
    }
    
    public void testAttributes7() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar:\n" +
                "    call(ATTRIBUTE = 10)\n" + //inside function call: don't get it
                "\n" +
        "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef)m.body[0]);
        assertEquals("Bar", ((NameTok)classDef.name).id);
        assertEquals(0, classDef.body.length); //method
        
    }
    
    public void testAttributes8() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar:\n" +
                "    ATTRIBUTE = dict(\n" + //inside function call: don't get it
                "       b=20,\n" +
                "       c=30\n" +
                "    )\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
        "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef)m.body[0]);
        assertEquals("Bar", ((NameTok)classDef.name).id);
        assertEquals(1, classDef.body.length);
        Assign assign = (Assign) classDef.body[0];
        assertEquals(1, assign.targets.length);
        Name name = (Name) assign.targets[0];
        assertEquals("ATTRIBUTE", name.id);
    }
    
    public void testDefinitionsParser() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:pass");
        assertEquals(1, m.body.length);
        assertEquals("Bar", ((NameTok)((ClassDef)m.body[0]).name).id);
    }
    
    
    public void testDefinitionsAttributesParser() {
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
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

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
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

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
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

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
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

        assertEquals("Bar", ((NameTok)classDefBar.name).id);
        assertEquals("mGlobal", ((NameTok)((FunctionDef)m.body[1]).name).id);
        
        ClassDef classDefZoo = (ClassDef)classDefBar.body[0];
        assertEquals("Zoo", ((NameTok)classDefZoo.name).id);
        
        assertEquals(2, classDefZoo.body.length);
        assertEquals("m1", ((NameTok)((FunctionDef)classDefZoo.body[0]).name).id);
        
    }
    
    
    public void testDefinitionsParser11() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar(object):\n" +
                "    class \tZoo\t(object):\n" +
                "        def     m1(self):pass\n"+
                "        def m2(self):pass\n"+
                "            #def m3(self):pass\n"+
                "            'string'\n"+
                "def mGlobal(self):pass\n"
        );
        assertEquals(2, m.body.length);
        ClassDef classDefBar = (ClassDef)m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);
        
        assertEquals("Bar", ((NameTok)classDefBar.name).id);
        FunctionDef defGlobal = (FunctionDef)m.body[1];
        assertEquals("mGlobal", ((NameTok)(defGlobal).name).id);
        assertEquals(1, defGlobal.beginColumn);
        assertEquals(7, defGlobal.beginLine);
        
        ClassDef classDefZoo = (ClassDef)classDefBar.body[0];
        assertEquals("Zoo", ((NameTok)classDefZoo.name).id);
        assertEquals(5, classDefZoo.beginColumn);
        assertEquals(2, classDefZoo.beginLine);
        
        assertEquals(2, classDefZoo.body.length);
        FunctionDef defM1 = (FunctionDef)classDefZoo.body[0];
        assertEquals("m1", ((NameTok)(defM1).name).id);
        assertEquals(9, defM1.beginColumn);
        assertEquals(3, defM1.beginLine);
        
    }
    
    
    public void testDefinitionsParser10() {
        Module m = (Module) FastDefinitionsParser.parse(
                "" //empty
        );
        assertEquals(0, m.body.length);
    }
    
    public void testEmpty() {
        Module m = (Module) FastDefinitionsParser.parse(
                "# This file was created automatically by SWIG 1.3.29.\n" +
                "" +
                "" //empty
        );
        assertEquals(0, m.body.length);
    }
    
    

    
}
