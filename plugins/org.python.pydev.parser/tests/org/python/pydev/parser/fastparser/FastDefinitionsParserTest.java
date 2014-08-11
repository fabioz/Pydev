/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.fastparser;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class FastDefinitionsParserTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private static int parseGeneration = 0;
    private static final int PARSE_GENERATION_DEFAULT = 0;
    private static final int PARSE_GENERATION_ONLY_LOAD = 1;
    private static final int PARSE_GENERATION_FULL_PARSE = 2;
    private static final int PARSE_GENERATION_SYNTAX_PARSE = 3;

    public static void main(String[] args) {
        try {
            FastDefinitionsParserTest test = new FastDefinitionsParserTest();
            test.setUp();

            // Parse results from code below (java 6 on a big library):
            // Only load
            // Time Elapsed (secs):2.757
            // Fast parse
            // Time Elapsed (secs):3.609
            // Syntax parse
            // Time Elapsed (secs):11.787
            // Full parse
            // Time Elapsed (secs):19.161

            // Timer timer = new Timer();
            // parseGeneration = PARSE_GENERATION_ONLY_LOAD;
            // System.out.println("Only load");
            // test.parseFilesInDir(new File("D:/bin/Python27/Lib"), true);
            // timer.printDiff();
            //
            // timer = new Timer();
            // parseGeneration = PARSE_GENERATION_DEFAULT;
            // System.out.println("Fast parse");
            // test.parseFilesInDir(new File("D:/bin/Python27/Lib"), true);
            // timer.printDiff();
            //
            // timer = new Timer();
            // parseGeneration = PARSE_GENERATION_SYNTAX_PARSE;
            // System.out.println("Syntax parse");
            // test.parseFilesInDir(new File("D:/bin/Python27/Lib"), true);
            // timer.printDiff();
            //
            // timer = new Timer();
            // parseGeneration = PARSE_GENERATION_FULL_PARSE;
            // System.out.println("Full parse");
            // test.parseFilesInDir(new File("D:/bin/Python27/Lib"), true);
            // timer.printDiff();

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
        assertTrue("Directory " + file +
                " does not exist", file.exists());
        if (!file.isDirectory()) {
            parseFile(file);
            return;
        }

        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.getAbsolutePath().toLowerCase().endsWith(".py")) {
                parseFile(f);

            } else if (recursive && f.isDirectory()) {
                parseFilesInDir(f, recursive);
            }
        }
    }

    private void parseFile(File f) {
        String fileContents = FileUtils.getFileContents(f);
        try {
            switch (parseGeneration) {
                case PARSE_GENERATION_DEFAULT:
                    FastDefinitionsParser.parse(fileContents);
                    break;
                case PARSE_GENERATION_FULL_PARSE:
                    PyParser.reparseDocumentInternal(new Document(fileContents), true,
                            IGrammarVersionProvider.LATEST_GRAMMAR_VERSION);
                    break;
                case PARSE_GENERATION_SYNTAX_PARSE:
                    PyParser.reparseDocumentInternal(new Document(fileContents), false,
                            IGrammarVersionProvider.LATEST_GRAMMAR_VERSION);
                    break;
                case PARSE_GENERATION_ONLY_LOAD:
                    //do nothing!
                    break;
            }

        } catch (Exception e) {
            System.out.println("Error parsing:" + f);
            e.printStackTrace();
        }
    }

    public void testAttributes() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:\n" +
                "    ATTRIBUTE = 10\n" +
                "\n" +
                "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef) m.body[0]);
        assertEquals("Bar", ((NameTok) classDef.name).id);
        assertEquals(1, classDef.body.length);
        Assign assign = (Assign) classDef.body[0];
        assertEquals(1, assign.targets.length);
        Name name = (Name) assign.targets[0];
        assertEquals("ATTRIBUTE", name.id);
    }

    public void testMultipleAssignAttributes() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:\n" +
                "    ATTRIBUTE1 = ATTRIBUTE2 = 10\n" +
                "\n"
                +
                "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef) m.body[0]);
        assertEquals("Bar", ((NameTok) classDef.name).id);
        assertEquals(1, classDef.body.length);
        Assign assign = (Assign) classDef.body[0];
        assertEquals(2, assign.targets.length);
        Name name = (Name) assign.targets[0];
        assertEquals("ATTRIBUTE1", name.id);
        name = (Name) assign.targets[1];
        assertEquals("ATTRIBUTE2", name.id);
    }

    public void testAttributes2() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:\n" +
                "    XXX.ATTRIBUTE = 10\n" + //we're assigning an attribute, that's not related to the class
                "\n" +
                "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef) m.body[0]);
        assertEquals("Bar", ((NameTok) classDef.name).id);
        assertEquals(0, classDef.body.length); //no attribute
    }

    public void testAttributes3() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:\n" +
                "    def m1(self):\n"
                +
                "        ATTRIBUTE = 10\n" + //local scope: don't get it
                "\n" +
                "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef) m.body[0]);
        assertEquals("Bar", ((NameTok) classDef.name).id);
        assertEquals(1, classDef.body.length); //method

        FunctionDef funcDef = (FunctionDef) classDef.body[0];
        assertEquals("m1", ((NameTok) funcDef.name).id);
        assertNull(funcDef.body);
    }

    public void testAttributes4() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:\n" +
                "    def m1(self):\n"
                +
                "        self.ATTRIBUTE = 10\n" + //local scope: get it because of self.
                "\n" +
                "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef) m.body[0]);
        assertEquals("Bar", ((NameTok) classDef.name).id);
        assertEquals(1, classDef.body.length); //method

        FunctionDef funcDef = (FunctionDef) classDef.body[0];
        assertEquals("m1", ((NameTok) funcDef.name).id);

        assertNull(funcDef.body[1]);
        Assign assign = (Assign) funcDef.body[0];
        assertEquals(1, assign.targets.length);
        Attribute attribute = (Attribute) assign.targets[0];
        NameTok attr = (NameTok) attribute.attr;
        assertEquals("ATTRIBUTE", attr.id.toString());
    }

    public void testAttributes5() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:\n" +
                "    def m1(self):\n"
                +
                "        self.ATTRIBUTE0 = 10\n" + //local scope: get it because of self.
                "        self.ATTRIBUTE1 = 10\n" + //local scope: get it because of self.
                "        self.ATTRIBUTE2 = = 10\n" + //local scope: get it because of self.
                "\n" +
                "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef) m.body[0]);
        assertEquals("Bar", ((NameTok) classDef.name).id);
        assertEquals(1, classDef.body.length); //method

        FunctionDef funcDef = (FunctionDef) classDef.body[0];
        assertEquals("m1", ((NameTok) funcDef.name).id);

        for (int i = 0; i < 3; i++) {
            Assign assign = (Assign) funcDef.body[i];
            assertEquals(1, assign.targets.length);
            Attribute attribute = (Attribute) assign.targets[0];
            NameTok attr = (NameTok) attribute.attr;
            assertEquals("ATTRIBUTE" + i, attr.id.toString());
        }
        assertNull(funcDef.body[3]);
    }

    public void testAttributes6() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:\n" +
                "    def m1(self):\n"
                +
                "        call(ATTRIBUTE = 10)\n" + //inside function call: don't get it
                "\n" +
                "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef) m.body[0]);
        assertEquals("Bar", ((NameTok) classDef.name).id);
        assertEquals(1, classDef.body.length); //method

        FunctionDef funcDef = (FunctionDef) classDef.body[0];
        assertEquals("m1", ((NameTok) funcDef.name).id);
        assertNull(funcDef.body);
    }

    public void testAttributes7() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:\n" +
                "    call(ATTRIBUTE = 10)\n" + //inside function call: don't get it
                "\n" +
                "");
        assertEquals(1, m.body.length);
        ClassDef classDef = ((ClassDef) m.body[0]);
        assertEquals("Bar", ((NameTok) classDef.name).id);
        assertEquals(0, classDef.body.length); //method

    }

    public void testAttributes8() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:\n" +
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
        ClassDef classDef = ((ClassDef) m.body[0]);
        assertEquals("Bar", ((NameTok) classDef.name).id);
        assertEquals(1, classDef.body.length);
        Assign assign = (Assign) classDef.body[0];
        assertEquals(1, assign.targets.length);
        Name name = (Name) assign.targets[0];
        assertEquals("ATTRIBUTE", name.id);
    }

    public void testGlobalAttributes() {
        Module m = (Module) FastDefinitionsParser.parse("GLOBAL_ATTRIBUTE = 10\n" +
                "\n" +
                "");
        assertEquals(1, m.body.length);
        Assign assign = ((Assign) m.body[0]);
        assertEquals("GLOBAL_ATTRIBUTE", ((Name) assign.targets[0]).id);
    }

    public void testGlobalAttributes5() {
        Module m = (Module) FastDefinitionsParser.parse("GLOBAL_ATTRIBUTE = 10\n" +
                "GLOBAL_ATTRIBUTE2 = 10\n" +
                "\n"
                +
                "");
        assertEquals(2, m.body.length);
        Assign assign = ((Assign) m.body[0]);
        assertEquals("GLOBAL_ATTRIBUTE", ((Name) assign.targets[0]).id);
        assign = ((Assign) m.body[1]);
        assertEquals("GLOBAL_ATTRIBUTE2", ((Name) assign.targets[0]).id);
    }

    public void testGlobalAttributes2() {
        String str = "import new\n" +
                "new_a = new.a\n";
        checkSingleGlobalAttr((Module) FastDefinitionsParser.parse(str));
        checkSingleGlobalAttr((Module) FastDefinitionsParser.parse(str.replaceAll("\n", "\r\n")));
    }

    public void testGlobalAttributes3() {
        String str = "#c\n" +
                "'m'\n" +
                "new_a= new.a\n" +
                "";
        checkSingleGlobalAttr((Module) FastDefinitionsParser.parse(str));
        checkSingleGlobalAttr((Module) FastDefinitionsParser.parse(str.replaceAll("\n", "\r\n")));
    }

    public void testGlobalAttributes4() {
        String str = "class A:\n" +
                "    pass\n" +
                "GLOBAL_ATTR = 10\n" +
                "";
        Module m = (Module) FastDefinitionsParser.parse(str);
        assertEquals(2, m.body.length);
        Assign assign = ((Assign) m.body[1]);
        assertEquals("GLOBAL_ATTR", ((Name) assign.targets[0]).id);
    }

    public void testGlobalAttributes6() {
        String str = "# on_fail constants    \n" +
                "RAISE  = 'RAISE'\n" +
                "IGNORE = 'IGNORE'\n" +
                "SKIP   = 'SKIP'\n"
                +
                "\n" +
                "";
        Module m = (Module) FastDefinitionsParser.parse(str);
        assertEquals(3, m.body.length);
        Assign assign = ((Assign) m.body[1]);
        assertEquals("IGNORE", ((Name) assign.targets[0]).id);
    }

    public void NotestGlobalAttributesWX() {
        String str = "# This file was created automatically by SWIG 1.3.29.\n"
                +
                "# Don't modify this file, modify the SWIG interface instead.\n"
                +
                "'''\n"
                +
                "The `XmlResource` class allows program resources defining menus, layout of\n"
                +
                "controls on a panel, etc. to be loaded from an XML file.\n"
                +
                "'''\n"
                +
                "import _xrc\n"
                +
                "import new\n"
                +
                "new_instancemethod = new.instancemethod\n"
                +
                "def _swig_setattr_nondynamic(self,class_type,name,value,static=1):\n"
                +
                "    if (name == 'thisown'): return self.this.own(value)\n"
                +
                "    if (name == 'this'):\n"
                +
                "        if type(value).__name__ == 'PySwigObject':\n"
                +
                "            self.__dict__[name] = value\n"
                +
                "            return\n"
                +
                "    method = class_type.__swig_setmethods__.get(name,None)\n"
                +
                "    if method: return method(self,value)\n"
                +
                "    if (not static) or hasattr(self,name):\n"
                +
                "        self.__dict__[name] = value\n"
                +
                "    else:\n"
                +
                "        raise AttributeError('You cannot add attributes to %s' % self)\n"
                +
                "def _swig_setattr(self,class_type,name,value):\n"
                +
                "    return _swig_setattr_nondynamic(self,class_type,name,value,0)\n"
                +
                "def _swig_getattr(self,class_type,name):\n"
                +
                "    if (name == 'thisown'): return self.this.own()\n"
                +
                "    method = class_type.__swig_getmethods__.get(name,None)\n"
                +
                "    if method: return method(self)\n"
                +
                "    raise AttributeError,name\n"
                +
                "def _swig_repr(self):\n"
                +
                "    try: strthis = 'proxy of ' + self.this.__repr__()\n"
                +
                "    except: strthis = ''\n"
                +
                "    return '<%s.%s; %s >' % (self.__class__.__module__, self.__class__.__name__, strthis,)\n"
                +
                "import types\n"
                +
                "try:\n"
                +
                "    _object = types.ObjectType\n"
                +
                "    _newclass = 1\n"
                +
                "except AttributeError:\n"
                +
                "    class _object : pass\n"
                +
                "    _newclass = 0\n"
                +
                "del types\n"
                +
                "def _swig_setattr_nondynamic_method(set):\n"
                +
                "    def set_attr(self,name,value):\n"
                +
                "        if (name == 'thisown'): return self.this.own(value)\n"
                +
                "        if hasattr(self,name) or (name == 'this'):\n"
                +
                "            set(self,name,value)\n"
                +
                "        else:\n"
                +
                "            raise AttributeError('You cannot add attributes to %s' % self)\n"
                +
                "    return set_attr\n"
                +
                "import _core\n"
                +
                "wx = _core \n"
                +
                "__docfilter__ = wx.__DocFilter(globals()) \n"
                +
                "#---------------------------------------------------------------------------\n"
                +
                "WX_XMLRES_CURRENT_VERSION_MAJOR = _xrc.WX_XMLRES_CURRENT_VERSION_MAJOR\n"
                +
                "WX_XMLRES_CURRENT_VERSION_MINOR = _xrc.WX_XMLRES_CURRENT_VERSION_MINOR\n"
                +
                "WX_XMLRES_CURRENT_VERSION_RELEASE = _xrc.WX_XMLRES_CURRENT_VERSION_RELEASE\n"
                +
                "WX_XMLRES_CURRENT_VERSION_REVISION = _xrc.WX_XMLRES_CURRENT_VERSION_REVISION\n"
                +
                "XRC_USE_LOCALE = _xrc.XRC_USE_LOCALE\n"
                +
                "XRC_NO_SUBCLASSING = _xrc.XRC_NO_SUBCLASSING\n"
                +
                "XRC_NO_RELOADING = _xrc.XRC_NO_RELOADING\n"
                +
                "class XmlResource(_core.Object):\n"
                +
                "    '''Proxy of C++ XmlResource class'''\n"
                +
                "    thisown = property(lambda x: x.this.own(), lambda x, v: x.this.own(v), doc='The membership flag')\n"
                +
                "    __repr__ = _swig_repr\n"
                +
                "    def __init__(self, *args, **kwargs): \n"
                +
                "        '''__init__(self, String filemask, int flags=XRC_USE_LOCALE, String domain=wxEmptyString) -> XmlResource'''\n"
                +
                "        _xrc.XmlResource_swiginit(self,_xrc.new_XmlResource(*args, **kwargs))\n"
                +
                "        self.InitAllHandlers()\n"
                +
                "    __swig_destroy__ = _xrc.delete_XmlResource\n"
                +
                "    __del__ = lambda self : None;\n"
                +
                "    def Load(*args, **kwargs):\n"
                +
                "        '''Load(self, String filemask) -> bool'''\n"
                +
                "        return _xrc.XmlResource_Load(*args, **kwargs)\n"
                +
                "    def LoadFromString(*args, **kwargs):\n"
                +
                "        '''LoadFromString(self, String data) -> bool'''\n"
                +
                "        return _xrc.XmlResource_LoadFromString(*args, **kwargs)\n"
                +
                "    def Unload(*args, **kwargs):\n"
                +
                "        '''Unload(self, String filename) -> bool'''\n"
                +
                "        return _xrc.XmlResource_Unload(*args, **kwargs)\n"
                +
                "    def InitAllHandlers(*args, **kwargs):\n"
                +
                "        '''InitAllHandlers(self)'''\n"
                +
                "        return _xrc.XmlResource_InitAllHandlers(*args, **kwargs)\n"
                +
                "    def AddHandler(*args, **kwargs):\n"
                +
                "        '''AddHandler(self, XmlResourceHandler handler)'''\n"
                +
                "        return _xrc.XmlResource_AddHandler(*args, **kwargs)\n"
                +
                "    def InsertHandler(*args, **kwargs):\n"
                +
                "        '''InsertHandler(self, XmlResourceHandler handler)'''\n"
                +
                "        return _xrc.XmlResource_InsertHandler(*args, **kwargs)\n"
                +
                "    def ClearHandlers(*args, **kwargs):\n"
                +
                "        '''ClearHandlers(self)'''\n"
                +
                "        return _xrc.XmlResource_ClearHandlers(*args, **kwargs)\n"
                +
                "    def AddSubclassFactory(*args, **kwargs):\n"
                +
                "        '''AddSubclassFactory(XmlSubclassFactory factory)'''\n"
                +
                "        return _xrc.XmlResource_AddSubclassFactory(*args, **kwargs)\n"
                +
                "    AddSubclassFactory = staticmethod(AddSubclassFactory)\n"
                +
                "    def LoadMenu(*args, **kwargs):\n"
                +
                "        '''LoadMenu(self, String name) -> Menu'''\n"
                +
                "        return _xrc.XmlResource_LoadMenu(*args, **kwargs)\n"
                +
                "    def LoadMenuBar(*args, **kwargs):\n"
                +
                "        '''LoadMenuBar(self, String name) -> MenuBar'''\n"
                +
                "        return _xrc.XmlResource_LoadMenuBar(*args, **kwargs)\n"
                +
                "    def LoadMenuBarOnFrame(*args, **kwargs):\n"
                +
                "        '''LoadMenuBarOnFrame(self, Window parent, String name) -> MenuBar'''\n"
                +
                "        return _xrc.XmlResource_LoadMenuBarOnFrame(*args, **kwargs)\n"
                +
                "    def LoadToolBar(*args, **kwargs):\n"
                +
                "        '''LoadToolBar(self, Window parent, String name) -> wxToolBar'''\n"
                +
                "        return _xrc.XmlResource_LoadToolBar(*args, **kwargs)\n"
                +
                "    def LoadDialog(*args, **kwargs):\n"
                +
                "        '''LoadDialog(self, Window parent, String name) -> wxDialog'''\n"
                +
                "        return _xrc.XmlResource_LoadDialog(*args, **kwargs)\n"
                +
                "    def LoadOnDialog(*args, **kwargs):\n"
                +
                "        '''LoadOnDialog(self, wxDialog dlg, Window parent, String name) -> bool'''\n"
                +
                "        return _xrc.XmlResource_LoadOnDialog(*args, **kwargs)\n"
                +
                "    def LoadPanel(*args, **kwargs):\n"
                +
                "        '''LoadPanel(self, Window parent, String name) -> wxPanel'''\n"
                +
                "        return _xrc.XmlResource_LoadPanel(*args, **kwargs)\n"
                +
                "    def LoadOnPanel(*args, **kwargs):\n"
                +
                "        '''LoadOnPanel(self, wxPanel panel, Window parent, String name) -> bool'''\n"
                +
                "        return _xrc.XmlResource_LoadOnPanel(*args, **kwargs)\n"
                +
                "    def LoadFrame(*args, **kwargs):\n"
                +
                "        '''LoadFrame(self, Window parent, String name) -> wxFrame'''\n"
                +
                "        return _xrc.XmlResource_LoadFrame(*args, **kwargs)\n"
                +
                "    def LoadOnFrame(*args, **kwargs):\n"
                +
                "        '''LoadOnFrame(self, wxFrame frame, Window parent, String name) -> bool'''\n"
                +
                "        return _xrc.XmlResource_LoadOnFrame(*args, **kwargs)\n"
                +
                "    def LoadObject(*args, **kwargs):\n"
                +
                "        '''LoadObject(self, Window parent, String name, String classname) -> Object'''\n"
                +
                "        return _xrc.XmlResource_LoadObject(*args, **kwargs)\n"
                +
                "    def LoadOnObject(*args, **kwargs):\n"
                +
                "        '''LoadOnObject(self, Object instance, Window parent, String name, String classname) -> bool'''\n"
                +
                "        return _xrc.XmlResource_LoadOnObject(*args, **kwargs)\n"
                +
                "    def LoadBitmap(*args, **kwargs):\n"
                +
                "        '''LoadBitmap(self, String name) -> Bitmap'''\n"
                +
                "        return _xrc.XmlResource_LoadBitmap(*args, **kwargs)\n"
                +
                "    def LoadIcon(*args, **kwargs):\n"
                +
                "        '''LoadIcon(self, String name) -> Icon'''\n"
                +
                "        return _xrc.XmlResource_LoadIcon(*args, **kwargs)\n"
                +
                "    def AttachUnknownControl(*args, **kwargs):\n"
                +
                "        '''AttachUnknownControl(self, String name, Window control, Window parent=None) -> bool'''\n"
                +
                "        return _xrc.XmlResource_AttachUnknownControl(*args, **kwargs)\n"
                +
                "    def GetXRCID(*args, **kwargs):\n"
                +
                "        '''GetXRCID(String str_id, int value_if_not_found=ID_NONE) -> int'''\n"
                +
                "        return _xrc.XmlResource_GetXRCID(*args, **kwargs)\n"
                +
                "    GetXRCID = staticmethod(GetXRCID)\n"
                +
                "    def GetVersion(*args, **kwargs):\n"
                +
                "        '''GetVersion(self) -> long'''\n"
                +
                "        return _xrc.XmlResource_GetVersion(*args, **kwargs)\n"
                +
                "    def CompareVersion(*args, **kwargs):\n"
                +
                "        '''CompareVersion(self, int major, int minor, int release, int revision) -> int'''\n"
                +
                "        return _xrc.XmlResource_CompareVersion(*args, **kwargs)\n"
                +
                "    def Get(*args, **kwargs):\n"
                +
                "        '''Get() -> XmlResource'''\n"
                +
                "        return _xrc.XmlResource_Get(*args, **kwargs)\n"
                +
                "    Get = staticmethod(Get)\n"
                +
                "    def Set(*args, **kwargs):\n"
                +
                "        '''Set(XmlResource res) -> XmlResource'''\n"
                +
                "        return _xrc.XmlResource_Set(*args, **kwargs)\n"
                +
                "    Set = staticmethod(Set)\n"
                +
                "    def GetFlags(*args, **kwargs):\n"
                +
                "        '''GetFlags(self) -> int'''\n"
                +
                "        return _xrc.XmlResource_GetFlags(*args, **kwargs)\n"
                +
                "    def SetFlags(*args, **kwargs):\n"
                +
                "        '''SetFlags(self, int flags)'''\n"
                +
                "        return _xrc.XmlResource_SetFlags(*args, **kwargs)\n"
                +
                "    def GetDomain(*args, **kwargs):\n"
                +
                "        '''GetDomain(self) -> String'''\n"
                +
                "        return _xrc.XmlResource_GetDomain(*args, **kwargs)\n"
                +
                "    def SetDomain(*args, **kwargs):\n"
                +
                "        '''SetDomain(self, String domain)'''\n"
                +
                "        return _xrc.XmlResource_SetDomain(*args, **kwargs)\n"
                +
                "    Domain = property(GetDomain,SetDomain,doc='See `GetDomain` and `SetDomain`') \n"
                +
                "    Flags = property(GetFlags,SetFlags,doc='See `GetFlags` and `SetFlags`') \n"
                +
                "    Version = property(GetVersion,doc='See `GetVersion`') \n"
                +
                "_xrc.XmlResource_swigregister(XmlResource)\n"
                +
                "cvar = _xrc.cvar\n"
                +
                "UTF8String = cvar.UTF8String\n"
                +
                "StyleString = cvar.StyleString\n"
                +
                "SizeString = cvar.SizeString\n"
                +
                "PosString = cvar.PosString\n"
                +
                "BitmapString = cvar.BitmapString\n"
                +
                "IconString = cvar.IconString\n"
                +
                "FontString = cvar.FontString\n"
                +
                "AnimationString = cvar.AnimationString\n"
                +
                "def EmptyXmlResource(*args, **kwargs):\n"
                +
                "    '''EmptyXmlResource(int flags=XRC_USE_LOCALE, String domain=wxEmptyString) -> XmlResource'''\n"
                +
                "    val = _xrc.new_EmptyXmlResource(*args, **kwargs)\n"
                +
                "    val.InitAllHandlers()\n"
                +
                "    return val\n"
                +
                "def XmlResource_AddSubclassFactory(*args, **kwargs):\n"
                +
                "  '''XmlResource_AddSubclassFactory(XmlSubclassFactory factory)'''\n"
                +
                "  return _xrc.XmlResource_AddSubclassFactory(*args, **kwargs)\n"
                +
                "def XmlResource_GetXRCID(*args, **kwargs):\n"
                +
                "  '''XmlResource_GetXRCID(String str_id, int value_if_not_found=ID_NONE) -> int'''\n"
                +
                "  return _xrc.XmlResource_GetXRCID(*args, **kwargs)\n"
                +
                "def XmlResource_Get(*args):\n"
                +
                "  '''XmlResource_Get() -> XmlResource'''\n"
                +
                "  return _xrc.XmlResource_Get(*args)\n"
                +
                "def XmlResource_Set(*args, **kwargs):\n"
                +
                "  '''XmlResource_Set(XmlResource res) -> XmlResource'''\n"
                +
                "  return _xrc.XmlResource_Set(*args, **kwargs)\n"
                +
                "def XRCID(str_id, value_if_not_found = wx.ID_NONE):\n"
                +
                "    return XmlResource_GetXRCID(str_id, value_if_not_found)\n"
                +
                "def XRCCTRL(window, str_id, *ignoreargs):\n"
                +
                "    return window.FindWindowById(XRCID(str_id))\n"
                +
                "#---------------------------------------------------------------------------\n"
                +
                "class XmlSubclassFactory(object):\n"
                +
                "    '''Proxy of C++ XmlSubclassFactory class'''\n"
                +
                "    thisown = property(lambda x: x.this.own(), lambda x, v: x.this.own(v), doc='The membership flag')\n"
                +
                "    __repr__ = _swig_repr\n" +
                "    def __init__(self, *args, **kwargs): \n"
                +
                "        '''__init__(self) -> XmlSubclassFactory'''\n"
                +
                "        _xrc.XmlSubclassFactory_swiginit(self,_xrc.new_XmlSubclassFactory(*args, **kwargs))\n"
                +
                "        XmlSubclassFactory._setCallbackInfo(self, self, XmlSubclassFactory)\n"
                +
                "    def _setCallbackInfo(*args, **kwargs):\n"
                +
                "        '''_setCallbackInfo(self, PyObject self, PyObject _class)'''\n"
                +
                "        return _xrc.XmlSubclassFactory__setCallbackInfo(*args, **kwargs)\n"
                +
                "    Parent = property(GetParent,doc=\"See `GetParent`\") \n"
                +
                "    ParentAsWindow = property(GetParentAsWindow,doc=\"See `GetParentAsWindow`\") \n"
                +
                "    Resource = property(GetResource,doc=\"See `GetResource`\") \n"
                +
                "_xrc.XmlSubclassFactory_swigregister(XmlSubclassFactory)\n"
                +
                "#---------------------------------------------------------------------------\n"
                +
                "XML_ELEMENT_NODE = _xrc.XML_ELEMENT_NODE\n" +
                "XML_ATTRIBUTE_NODE = _xrc.XML_ATTRIBUTE_NODE\n"
                +
                "_xrc.XmlResourceHandler_swigregister(XmlResourceHandler)\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "";
        Module m = (Module) FastDefinitionsParser.parse(str);
        System.out.println("OK");
    }

    private void checkSingleGlobalAttr(Module m) {
        assertEquals(1, m.body.length);
        Assign assign = ((Assign) m.body[0]);
        assertEquals("new_a", ((Name) assign.targets[0]).id);
    }

    public void testDefinitionsParser() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:pass");
        assertEquals(1, m.body.length);
        assertEquals("Bar", ((NameTok) ((ClassDef) m.body[0]).name).id);
    }

    public void testDefinitionsAttributesParser() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar:pass");
        assertEquals(1, m.body.length);
        assertEquals("Bar", ((NameTok) ((ClassDef) m.body[0]).name).id);
    }

    public void testDefinitionsParser2() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar");
        assertEquals(1, m.body.length);
        assertEquals("Bar", ((NameTok) ((ClassDef) m.body[0]).name).id);
    }

    public void testDefinitionsParser3() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):pass");
        assertEquals(1, m.body.length);
        assertEquals("Bar", ((NameTok) ((ClassDef) m.body[0]).name).id);
    }

    public void testDefinitionsParser4() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    def m1(self):pass");
        assertEquals(1, m.body.length);
        ClassDef classDef = (ClassDef) m.body[0];
        assertEquals("Bar", ((NameTok) classDef.name).id);

        FunctionDef funcDef = (FunctionDef) classDef.body[0];
        assertEquals("m1", ((NameTok) funcDef.name).id);
    }

    public void testDefinitionsParser5() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    def m1(self):pass\n"
                +
                "def m2(self):pass\n");
        assertEquals(2, m.body.length);
        ClassDef classDef = (ClassDef) m.body[0];

        assertEquals("Bar", ((NameTok) classDef.name).id);

        FunctionDef funcDef = (FunctionDef) classDef.body[0];
        assertEquals("m1", ((NameTok) funcDef.name).id);

        funcDef = (FunctionDef) m.body[1];
        assertEquals("m2", ((NameTok) funcDef.name).id);
    }

    public void testDefinitionsParser6() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    class Zoo(object):\n"
                +
                "        def m1(self):pass\n" +
                "def m2(self):pass\n");
        assertEquals(2, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

        assertEquals("Bar", ((NameTok) classDefBar.name).id);

        ClassDef classDefZoo = (ClassDef) classDefBar.body[0];
        assertEquals("Zoo", ((NameTok) classDefZoo.name).id);

        assertEquals("m1", ((NameTok) ((FunctionDef) classDefZoo.body[0]).name).id);

        assertEquals("m2", ((NameTok) ((FunctionDef) m.body[1]).name).id);
    }

    public void testDefinitionsParser7() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar(object):\n" +
                        "    class Zoo(object):\n" +
                        "        class PPP(self):pass\n" +

                        "class Bar2(object):\n" +
                        "    class Zoo2(object):\n" +
                        "        class PPP2(self):pass\n");
        assertEquals(2, m.body.length);

        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

        assertEquals("Bar", ((NameTok) classDefBar.name).id);
        ClassDef classDefZoo = (ClassDef) classDefBar.body[0];
        assertEquals("Zoo", ((NameTok) classDefZoo.name).id);
        assertEquals("PPP", ((NameTok) ((ClassDef) classDefZoo.body[0]).name).id);

        //check the 2nd leaf
        classDefBar = (ClassDef) m.body[1];
        assertEquals("Bar2", ((NameTok) classDefBar.name).id);
        classDefZoo = (ClassDef) classDefBar.body[0];
        assertEquals("Zoo2", ((NameTok) classDefZoo.name).id);
        assertEquals("PPP2", ((NameTok) ((ClassDef) classDefZoo.body[0]).name).id);
    }

    public void testDefinitionsParser7a() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar(object):\n" +
                        "    class Zoo(object):\n" +
                        "        pass\n" +

                        "class Bar2(object):\n" +
                        "    class Zoo2(object):\n" +
                        "        pass\n");
        assertEquals(2, m.body.length);

        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

        assertEquals("Bar", ((NameTok) classDefBar.name).id);
        ClassDef classDefZoo = (ClassDef) classDefBar.body[0];
        assertEquals("Zoo", ((NameTok) classDefZoo.name).id);

        //check the 2nd leaf
        classDefBar = (ClassDef) m.body[1];
        assertEquals("Bar2", ((NameTok) classDefBar.name).id);
        classDefZoo = (ClassDef) classDefBar.body[0];
        assertEquals("Zoo2", ((NameTok) classDefZoo.name).id);
    }

    public void testDefinitionsParser7b() {
        Module m = (Module) FastDefinitionsParser.parse(
                "class Bar(object):\n" +
                        "    pass\n" +
                        "class Bar2(object):\n" +
                        "    pass\n");
        assertEquals(2, m.body.length);

        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

        assertEquals("Bar", ((NameTok) classDefBar.name).id);

        //check the 2nd leaf
        classDefBar = (ClassDef) m.body[1];
        assertEquals("Bar2", ((NameTok) classDefBar.name).id);
    }

    public void testDefinitionsParser8() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    class Zoo(object):\n"
                +
                "        def m1(self):pass\n" +
                "        def m2(self):pass\n" +
                "            def m3(self):pass\n"
                +
                "def mGlobal(self):pass\n");
        assertEquals(2, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

        assertEquals("Bar", ((NameTok) classDefBar.name).id);
        assertEquals("mGlobal", ((NameTok) ((FunctionDef) m.body[1]).name).id);

        ClassDef classDefZoo = (ClassDef) classDefBar.body[0];
        assertEquals("Zoo", ((NameTok) classDefZoo.name).id);

        assertEquals(2, classDefZoo.body.length);
        assertEquals("m1", ((NameTok) ((FunctionDef) classDefZoo.body[0]).name).id);

    }

    public void testDefinitionsParser9() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    class \tZoo\t(object):\n"
                +
                "        def     m1(self):pass\n" +
                "        def m2(self):pass\n" +
                "            def m3(self):pass\n"
                +
                "def mGlobal(self):pass\n");
        assertEquals(2, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

        assertEquals("Bar", ((NameTok) classDefBar.name).id);
        assertEquals("mGlobal", ((NameTok) ((FunctionDef) m.body[1]).name).id);

        ClassDef classDefZoo = (ClassDef) classDefBar.body[0];
        assertEquals("Zoo", ((NameTok) classDefZoo.name).id);

        assertEquals(2, classDefZoo.body.length);
        assertEquals("m1", ((NameTok) ((FunctionDef) classDefZoo.body[0]).name).id);

    }

    public void testDefinitionsParser11() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    class \tZoo\t(object):\n"
                +
                "        def     m1(self):pass\n" +
                "        def m2(self):pass\n"
                +
                "            #def m3(self):pass\n" +
                "            'string'\n" +
                "def mGlobal(self):pass\n");
        assertEquals(2, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);

        assertEquals("Bar", ((NameTok) classDefBar.name).id);
        FunctionDef defGlobal = (FunctionDef) m.body[1];
        assertEquals("mGlobal", ((NameTok) (defGlobal).name).id);
        assertEquals(1, defGlobal.beginColumn);
        assertEquals(7, defGlobal.beginLine);

        ClassDef classDefZoo = (ClassDef) classDefBar.body[0];
        assertEquals("Zoo", ((NameTok) classDefZoo.name).id);
        assertEquals(5, classDefZoo.beginColumn);
        assertEquals(2, classDefZoo.beginLine);

        assertEquals(2, classDefZoo.body.length);
        FunctionDef defM1 = (FunctionDef) classDefZoo.body[0];
        assertEquals("m1", ((NameTok) (defM1).name).id);
        assertEquals(9, defM1.beginColumn);
        assertEquals(3, defM1.beginLine);

    }

    public void testDefinitionsParser12() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    #d\n" +
                "    'string'\n"
                +
                "def mGlobal(self):pass\n");

        FunctionDef defGlobal = (FunctionDef) m.body[1];
        assertEquals("mGlobal", ((NameTok) (defGlobal).name).id);
        assertEquals(1, defGlobal.beginColumn);
        assertEquals(4, defGlobal.beginLine);

    }

    public void testDefinitionsParserLines() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    def ra(\n" +
                "\n" +
                "\n"
                +
                "    )\n" +
                "    def m2(self):pass\n" +
                "        #def m3(self):pass\n" +
                "        'string'\n");
        assertEquals(1, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);
        assertEquals("Bar", ((NameTok) classDefBar.name).id);

        FunctionDef defRa = (FunctionDef) classDefBar.body[0];
        assertEquals("ra", ((NameTok) (defRa).name).id);
        assertEquals(5, defRa.beginColumn);
        assertEquals(2, defRa.beginLine);

        FunctionDef defM2 = (FunctionDef) classDefBar.body[1];
        assertEquals("m2", ((NameTok) (defM2).name).id);
        assertEquals(5, defM2.beginColumn);
        assertEquals(6, defM2.beginLine);

    }

    public void testDefinitionsParserLines2() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    def ra(\n" +
                "\n" +
                "\n"
                +
                "    )\n" +
                "    '''some\n" +
                "multi\n" +
                "line\n" +
                "string\n" +
                "    '''\n"
                +
                "    def m2(self):pass\n" +
                "        #def m3(self):pass\n" +
                "        'string'\n");
        assertEquals(1, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);
        assertEquals("Bar", ((NameTok) classDefBar.name).id);

        FunctionDef defRa = (FunctionDef) classDefBar.body[0];
        assertEquals("ra", ((NameTok) (defRa).name).id);
        assertEquals(5, defRa.beginColumn);
        assertEquals(2, defRa.beginLine);

        FunctionDef defM2 = (FunctionDef) classDefBar.body[1];
        assertEquals("m2", ((NameTok) (defM2).name).id);
        assertEquals(5, defM2.beginColumn);
        assertEquals(11, defM2.beginLine);
    }

    public void testDefinitionsParserLines3() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    def ra(\n" +
                "\n" +
                "\n"
                +
                "    )\n" +
                "    a = 10\n" +
                "    a = 10\n" +
                "    a = #comment 10\r\n" +
                "    '''some\n" +
                "multi\n"
                +
                "line\n" +
                "string\n" +
                "    '''\n" +
                "    def m2(self):pass\n" +
                "        #def m3(self):pass\n"
                +
                "        'string'\n");
        assertEquals(1, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);
        assertEquals("Bar", ((NameTok) classDefBar.name).id);

        FunctionDef defRa = (FunctionDef) classDefBar.body[0];
        assertEquals("ra", ((NameTok) (defRa).name).id);
        assertEquals(5, defRa.beginColumn);
        assertEquals(2, defRa.beginLine);

        //2 assigns
        FunctionDef defM2 = (FunctionDef) classDefBar.body[4];
        assertEquals("m2", ((NameTok) (defM2).name).id);
        assertEquals(5, defM2.beginColumn);
        assertEquals(14, defM2.beginLine);
    }

    public void testDefinitionsParserLines4() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    def ra(\n" +
                "\n" +
                "\n"
                +
                "    )\n" +
                "    a = (\n" +
                "        a = 10\n" +
                "    )\n" +
                "    def m2(self):pass\n"
                +
                "        #def m3(self):pass\n" +
                "        'string'\n");
        assertEquals(1, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);
        assertEquals("Bar", ((NameTok) classDefBar.name).id);

        FunctionDef defM2 = (FunctionDef) classDefBar.body[2];
        assertEquals("m2", ((NameTok) (defM2).name).id);
        assertEquals(5, defM2.beginColumn);
        assertEquals(9, defM2.beginLine);
    }

    public void testDefinitionsParserLines5() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    def m2(self):self.a=10\n");
        assertEquals(1, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);
        assertEquals("Bar", ((NameTok) classDefBar.name).id);

        FunctionDef defM2 = (FunctionDef) classDefBar.body[0];
        assertEquals("m2", ((NameTok) (defM2).name).id);
        assertEquals(5, defM2.beginColumn);
        assertEquals(2, defM2.beginLine);

        Assign a = (Assign) defM2.body[0];
        assertEquals("self.a", NodeUtils.getFullRepresentationString(a.targets[0]));
        assertEquals(18, a.beginColumn);
        assertEquals(2, a.beginLine);
    }

    public void testDefinitionsParserLines6() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    def m2(\n" +
                "        self,\n"
                +
                "        a):self.a=10\n");
        assertEquals(1, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);
        assertEquals("Bar", ((NameTok) classDefBar.name).id);

        FunctionDef defM2 = (FunctionDef) classDefBar.body[0];
        assertEquals("m2", ((NameTok) (defM2).name).id);
        assertEquals(5, defM2.beginColumn);
        assertEquals(2, defM2.beginLine);

        Assign a = (Assign) defM2.body[0];
        assertEquals("self.a", NodeUtils.getFullRepresentationString(a.targets[0]));
        assertEquals(12, a.beginColumn);
        assertEquals(4, a.beginLine);
    }

    public void testDefinitionsParserLines7() {
        Module m = (Module) FastDefinitionsParser.parse("class Bar(object):\n" +
                "    def m2 ( \n" +
                "        self,\n"
                +
                "        a ) : self.a=10\n");
        assertEquals(1, m.body.length);
        ClassDef classDefBar = (ClassDef) m.body[0];
        assertEquals(1, classDefBar.beginColumn);
        assertEquals(1, classDefBar.beginLine);
        assertEquals("Bar", ((NameTok) classDefBar.name).id);

        FunctionDef defM2 = (FunctionDef) classDefBar.body[0];
        assertEquals("m2", ((NameTok) (defM2).name).id);
        assertEquals(5, defM2.beginColumn);
        assertEquals(2, defM2.beginLine);

        Assign a = (Assign) defM2.body[0];
        assertEquals("self.a", NodeUtils.getFullRepresentationString(a.targets[0]));
        assertEquals(15, a.beginColumn);
        assertEquals(4, a.beginLine);
    }

    public void testDefinitionsParserLines8() {
        Module m = (Module) FastDefinitionsParser.parse("\r\n" +
                "#=\r\n" +
                "#=\r\n" +
                "#=\r\n"
                +
                "class Test(unittest.TestCase):\r\n" +
                "\r\n" +
                "    def MockMethod(self, *args):\r\n"
                +
                "        return 3\r\n" +
                "\r\n" +
                "    def testMockHandler(self):\r\n" +
                "        c = _MyClass()\r\n"
                +
                "\r\n" +
                "        mock = installMocks(_MyClass, Method=self.MockMethod)\r\n" +
                "        try:\r\n"
                +
                "            self.assertEqual(c.Method(5), 3)\r\n" +
                "        finally:\r\n"
                +
                "            mock.uninstall()\r\n" +
                "\r\n" +
                "        self.assertEqual(c.Method(5), 10)\r\n" +
                "");
        assertEquals(1, m.body.length);
        ClassDef classDef = (ClassDef) m.body[0];
        assertEquals(1, classDef.beginColumn);
        assertEquals(5, classDef.beginLine);
        assertEquals("Test", ((NameTok) classDef.name).id);

        FunctionDef def = (FunctionDef) classDef.body[0];
        assertEquals("MockMethod", ((NameTok) (def).name).id);
        assertEquals(5, def.beginColumn);
        assertEquals(7, def.beginLine);

        def = (FunctionDef) classDef.body[1];
        assertEquals("testMockHandler", ((NameTok) (def).name).id);
        assertEquals(5, def.beginColumn);
        assertEquals(10, def.beginLine);
    }

    public void testDefinitionsParser10() {
        Module m = (Module) FastDefinitionsParser.parse("" //empty
                );
        assertEquals(0, m.body.length);
    }

    public void testDefinitionsParser13() {
        Module m = (Module) FastDefinitionsParser
                .parse("\n"
                        +
                        "def get_validation_errors(outfile, app=None):\n"
                        +
                        "    '''\n"
                        +
                        "    Validates all models that are part of the specified app. If no app name is provided,\n"
                        +
                        "    validates all models of all installed apps. Writes errors, if any, to outfile.\n"
                        +
                        "    Returns number of errors.\n"
                        +
                        "    '''\n"
                        +
                        "    e = ModelErrorCollection(outfile)\n"
                        +
                        "    for (app_name, error) in get_app_errors().items():\n"
                        +
                        "        e.add(app_name, error)\n"
                        +
                        "\n"
                        +
                        "    for cls in models.get_models(app):\n"
                        +
                        "        opts = cls._meta\n"
                        +
                        "\n"
                        +
                        "        # Do field-specific validation.\n"
                        +
                        "        for f in opts.local_fields:\n"
                        +
                        "            if f.name == 'id' and not f.primary_key and opts.pk.name == 'id':\n"
                        +
                        "                e.add(opts, ''%s': You can\'t use 'id' as a field name, because each model automatically gets an 'id' field if none of the fields have primary_key=True. You need to either remove/rename your 'id' field or add primary_key=True to a field.' % f.name)\n"
                        +
                        "            if f.name.endswith('_'):\n"
                        +
                        "                e.add(opts, ''%s': Field names cannot end with underscores, because this would lead to ambiguous queryset filters.' % f.name)\n"
                        +
                        "            if isinstance(f, models.CharField):\n"
                        +
                        "                try:\n"
                        +
                        "                    max_length = int(f.max_length)\n"
                        +
                        "                    if max_length <= 0:\n"
                        +
                        "                        e.add(opts, ''%s': CharFields require a 'max_length' attribute that is a positive integer.' % f.name)\n"
                        +
                        "                except (ValueError, TypeError):\n"
                        +
                        "                    e.add(opts, ''%s': CharFields require a 'max_length' attribute that is a positive integer.' % f.name)\n"
                        +
                        "            if isinstance(f, models.DecimalField):\n"
                        +
                        "                decimalp_ok, mdigits_ok = False, False\n"
                        +
                        "                decimalp_msg = ''%s': DecimalFields require a 'decimal_places' attribute that is a non-negative integer.'\n"
                        +
                        "                try:\n"
                        +
                        "                    decimal_places = int(f.decimal_places)\n"
                        +
                        "                    if decimal_places < 0:\n"
                        +
                        "                        e.add(opts, decimalp_msg % f.name)\n"
                        +
                        "                    else:\n"
                        +
                        "                        decimalp_ok = True\n"
                        +
                        "                except (ValueError, TypeError):\n"
                        +
                        "                    e.add(opts, decimalp_msg % f.name)\n"
                        +
                        "                mdigits_msg = ''%s': DecimalFields require a 'max_digits' attribute that is a positive integer.'\n"
                        +
                        "" +
                        "");
        assertEquals(1, m.body.length);
        FunctionDef d = (FunctionDef) m.body[0];
        assertEquals("get_validation_errors", NodeUtils.getRepresentationString(d.name));
        assertNull(d.body);
    }

    public void testDefinitionsParser14() {
        Module m = (Module) FastDefinitionsParser.parse("\n" +
                "def method():\n" +
                "    a = 10\n" +
                "class F:\n"
                +
                "    def method2(self):\n" +
                "        self.bar = 10\n" +
                "def another():\n" +
                "    b = 20\n" +
                "");
        assertEquals(3, m.body.length);
        FunctionDef d = (FunctionDef) m.body[0];
        assertEquals("method", NodeUtils.getRepresentationString(d.name));
        assertNull(d.body);

        ClassDef cd = (ClassDef) m.body[1];
        assertEquals("F", NodeUtils.getRepresentationString(cd.name));
        assertEquals(1, cd.body.length);

        d = (FunctionDef) m.body[2];
        assertEquals("another", NodeUtils.getRepresentationString(d.name));

    }

    public void testDefinitionsParser15() {
        FastStringBuffer buf = new FastStringBuffer();
        for (int i = 0; i < 2000; i++) {
            buf.append("class Spam(object): pass\n");
        }
        Module m = (Module) FastDefinitionsParser.parse(buf.toString());
    }

    public void testDefinitionsParser16() {
        Module m = (Module) FastDefinitionsParser.parse("class");
    }

    public void testDefinitionsParser17() {
        Module m = (Module) FastDefinitionsParser.parse("class\n");
    }

    public void testDefinitionsParser18() {
        Module m = (Module) FastDefinitionsParser.parse("def");
    }

    public void testDefinitionsParser19() {
        Module m = (Module) FastDefinitionsParser.parse("def\n");
    }

    public void testEmpty() {
        Module m = (Module) FastDefinitionsParser.parse("# This file was created automatically by SWIG 1.3.29.\n" +
                ""
                +
                "" //empty
        );
        assertEquals(0, m.body.length);
    }

}
