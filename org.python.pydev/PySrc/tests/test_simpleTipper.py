'''
@author Fabio Zadrozny 
'''
import os
import sys
#make it as if we were executing from the directory above this one (so that we can use pycompletionserver
#without the need for it being in the pythonpath)
sys.argv[0] = os.path.dirname(sys.argv[0]) 
#twice the dirname to get the previous level from this file.
sys.path.insert(1, os.path.join(  os.path.dirname( sys.argv[0] )) )

import unittest
import importsTipper
import inspect

class Test(unittest.TestCase):

    def p(self, t):
        for a in t:
            print a
 
    def testImports3(self):
        tip = importsTipper.GenerateTip('os')
        ret = self.assertIn('path', tip)
        self.assertEquals('', ret[2])

    def testImports2(self):
        tip = importsTipper.GenerateTip('OpenGL.GLUT')
        self.assertIn('glutDisplayFunc', tip)
        self.assertIn('glutInitDisplayMode', tip)
        
    def testImports(self):
        '''
        You can print the results to check...
        '''
        tip = importsTipper.GenerateTip('qt')
        self.assertIn('QWidget'        , tip)
        self.assertIn('QDialog'        , tip)
        
        tip = importsTipper.GenerateTip('qt.QWidget')
        self.assertIn('rect'           , tip)
        self.assertIn('rect'           , tip)
        self.assertIn('AltButton'      , tip)

        tip = importsTipper.GenerateTip('qt.QWidget.AltButton')
        self.assertIn('__xor__'      , tip)

        tip = importsTipper.GenerateTip('qt.QWidget.AltButton.__xor__')
        self.assertIn('__class__'      , tip)
        
        tip = importsTipper.GenerateTip('__builtin__')
        self.assertIn('object'         , tip)
        self.assertIn('tuple'          , tip)
        self.assertIn('RuntimeError'   , tip)
        self.assertIn('RuntimeWarning' , tip)
        
        
        tip = importsTipper.GenerateTip('compiler') 
        self.assertArgs('parse', '(buf, mode)', tip)
        self.assertArgs('walk', '(tree, visitor, walker, verbose)', tip)
        self.assertIn('parse'          , tip)
        self.assertIn('parseFile'      , tip)
        
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

    def testInspect(self):
        
        class C(object):
            def metA(self, a, b):
                pass
        
        obj = C.metA
        if inspect.ismethod (obj):
            pass
#            print obj.im_func
#            print inspect.getargspec(obj.im_func)
            
        
        
if __name__ == '__main__':
    unittest.main()
    
