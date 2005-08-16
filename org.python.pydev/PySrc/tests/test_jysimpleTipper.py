import unittest
import os
import sys
#make it as if we were executing from the directory above this one (so that we can use pycompletionserver
#without the need for it being in the pythonpath)
sys.argv[0] = os.path.dirname(sys.argv[0]) 
#twice the dirname to get the previous level from this file.
sys.path.insert(1, os.path.join(  os.path.dirname( sys.argv[0] )) )

from jyimportsTipper import ismethod
from jyimportsTipper import isclass
from jyimportsTipper import dirObj
import jyimportsTipper
from java.lang.reflect import Method
from java.lang import System
from java.lang import String
from java.lang.System import arraycopy
from java.lang.System import out

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
        raise AssertionError('%s not in %s', tok, tips)

    def testImports3(self):
        tip = jyimportsTipper.GenerateTip('os')
        ret = self.assertIn('path', tip)
        self.assertEquals('', ret[2])
        
    def testImports(self):
        tip = jyimportsTipper.GenerateTip('__builtin__')
        self.assertIn('tuple'          , tip)
        self.assertIn('RuntimeError'   , tip)
        self.assertIn('RuntimeWarning' , tip)


class TestCompl(unittest.TestCase):

    def setUp(self):
        unittest.TestCase.setUp(self)

    def tearDown(self):
        unittest.TestCase.tearDown(self)

    def testGettingInfoOnJython(self):
        
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
