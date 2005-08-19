import unittest
import os
import sys
#make it as if we were executing from the directory above this one (so that we can use pycompletionserver
#without the need for it being in the pythonpath)
sys.argv[0] = os.path.dirname(sys.argv[0]) 
#twice the dirname to get the previous level from this file.
sys.path.insert(1, os.path.join(  os.path.dirname( sys.argv[0] )) )
sys.path.insert(1, r"C:\bin\eclipse310\plugins\org.junit_3.8.1\junit.jar" ) #some late loading jar tests

from jyimportsTipper import ismethod
from jyimportsTipper import isclass
from jyimportsTipper import dirObj
import jyimportsTipper
from java.lang.reflect import Method
from java.lang import System
from java.lang import String
from java.lang.System import arraycopy
from java.lang.System import out
import java.lang.String

__DBG = 0
def dbg(s):
    if __DBG:
        print s
        


class TestMod(unittest.TestCase):
    
    def assertArgs(self, tok, args, tips):
        for a in tips:
            if tok == a[0]:
                self.assertEquals(args, a[2])
                return
        raise AssertionError('%s not in %s', tok, tips)

    def assertIn(self, tok, tips):
        self.assertEquals(4, len(tips[0]))
        for a in tips:
            if tok == a[0]:
                return a
        s = ''
        for a in tips:
            s += str(a)
            s += '\n'
        raise AssertionError('%s not in %s' % (tok, s))

    def testImports1(self):
        tip = jyimportsTipper.GenerateTip('junit.framework.TestCase')
        ret = self.assertIn('assertEquals', tip)
        self.assertEquals('', ret[2])
        
    def testImports2(self):
        tip = jyimportsTipper.GenerateTip('junit.framework')
        ret = self.assertIn('TestCase', tip)
        self.assertEquals('', ret[2])
        
    def testImports3(self):
        tip = jyimportsTipper.GenerateTip('os')
        ret = self.assertIn('path', tip)
        self.assertEquals('', ret[2])
        
    def testImports(self):
        tip = jyimportsTipper.GenerateTip('__builtin__')
        self.assertIn('tuple'          , tip)
        self.assertIn('RuntimeError'   , tip)
        self.assertIn('RuntimeWarning' , tip)

    def testImports(self):
        tip = jyimportsTipper.GenerateTip('java.lang')
        tup = self.assertIn('String' , tip)
        self.assertEquals(str(jyimportsTipper.TYPE_CLASS), tup[3])
        
        tip = jyimportsTipper.GenerateTip('java')
        tup = self.assertIn('lang' , tip)
        self.assertEquals(str(jyimportsTipper.TYPE_IMPORT), tup[3])
        
        tip = jyimportsTipper.GenerateTip('java.lang.String')
        tup = self.assertIn('indexOf'          , tip)
        self.assertEquals(str(jyimportsTipper.TYPE_FUNCTION), tup[3])

        tip = jyimportsTipper.GenerateTip('java.lang.String')
        tup = self.assertIn('charAt'          , tip)
        self.assertEquals(str(jyimportsTipper.TYPE_FUNCTION), tup[3])
        self.assertEquals('(int)', tup[2])

        tup = self.assertIn('format'          , tip)
        self.assertEquals(str(jyimportsTipper.TYPE_FUNCTION), tup[3])
        self.assertEquals('(string, objectArray)', tup[2])
        self.assert_(tup[1].find('[Ljava.lang.Object;') == -1)

        tup = self.assertIn('getBytes'          , tip)
        self.assertEquals(str(jyimportsTipper.TYPE_FUNCTION), tup[3])
        self.assert_(tup[1].find('[B') == -1)
        self.assert_(tup[1].find('byte[]') != -1)

        tip = jyimportsTipper.GenerateTip('__builtin__.str')
        self.assertIn('find'          , tip)

        tip = jyimportsTipper.GenerateTip('__builtin__.dict')
        self.assertIn('get'          , tip)


import org.python.core.PyDictionary
class TestCompl(unittest.TestCase):

    def setUp(self):
        unittest.TestCase.setUp(self)

    def tearDown(self):
        unittest.TestCase.tearDown(self)

    def testGettingInfoOnJython(self):
        
        dbg( '\n\n--------------------------- java')
        assert not ismethod(java)[0]
        assert not isclass(java)
        assert jyimportsTipper.ismodule(java)
            
        dbg( '\n\n--------------------------- java.lang')
        assert not ismethod(java.lang)[0]
        assert not isclass(java.lang)
        assert jyimportsTipper.ismodule(java.lang)
            
        dbg( '\n\n--------------------------- Method')
        assert not ismethod(Method)[0]
        assert isclass(Method)
            
        dbg( '\n\n--------------------------- System')
        assert not ismethod(System)[0]
        assert isclass(System)
            
        dbg( '\n\n--------------------------- String')
        assert not ismethod(System)[0]
        assert isclass(String)
        assert len(dirObj(String)) > 10
            
        dbg( '\n\n--------------------------- arraycopy')
        isMet = ismethod(arraycopy)
        assert isMet[0]
        assert isMet[1][0].basicAsStr() == "function:arraycopy args=['java.lang.Object', 'int', 'java.lang.Object', 'int', 'int'], varargs=None, kwargs=None, docs:None"
        assert not isclass(arraycopy)
            
        dbg( '\n\n--------------------------- out')
        isMet = ismethod(out)
        assert not isMet[0]
        assert not isclass(out)
            
        dbg( '\n\n--------------------------- out.println')
        isMet = ismethod(out.println)
        assert isMet[0]
        assert len(isMet[1]) == 10
        assert isMet[1][0].basicAsStr() == "function:println args=[], varargs=None, kwargs=None, docs:None"
        assert isMet[1][1].basicAsStr() == "function:println args=['long'], varargs=None, kwargs=None, docs:None"
        assert not isclass(out.println)
        
        dbg( '\n\n--------------------------- str')
        isMet = ismethod(str)
        assert isMet[0]
        assert isMet[1][0].basicAsStr() == "function:str args=['org.python.core.PyObject'], varargs=None, kwargs=None, docs:None"
        assert not isclass(str)
        
        
        def met1():
            a=3
            return a
        
        dbg( '\n\n--------------------------- met1')
        isMet = ismethod(met1)
        assert isMet[0]
        assert isMet[1][0].basicAsStr() == "function:met1 args=[], varargs=None, kwargs=None, docs:None"
        assert not isclass(met1)
        
        def met2(arg1, arg2, *vararg, **kwarg):
            '''docmet2'''
            
            a=1
            return a
        
        dbg( '\n\n--------------------------- met2')
        isMet = ismethod(met2)
        assert isMet[0]
        assert isMet[1][0].basicAsStr() == "function:met2 args=['arg1', 'arg2'], varargs=vararg, kwargs=kwarg, docs:docmet2"
        assert not isclass(met2)
        


if __name__ == '__main__':
    unittest.main()
