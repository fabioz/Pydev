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
import simpleTipper
import importsTipper
import inspect

class Test(unittest.TestCase):

    
    def getDoc1(self):
        s = \
'''

import math

class C(object):
    \'\'\'
        CDescription
    \'\'\'
    def __init__(self):
        
        print dir(self)
    
    def a(self):
        \'\'\'
        ADescription
        \'\'\'
        pass
        
    def b(self):
        self
'''
    
        return s
        

    def testEnv1(self):
        comps = simpleTipper.GenerateTip(self.getDoc1(), None, True)
        import math, inspect
        
        checkedMath = False
        checkedC = False
        for tup in comps:
        
            if tup[0] == 'math':
                checkedMath = True
                self.assertEquals(inspect.getdoc(math),tup[1])
        
            elif tup[0] == 'C':
                checkedC = True
                self.assert_('CDescription' in tup[1])

        self.assert_(checkedC and checkedMath)


    def testEnv1CToken(self):
        comps = simpleTipper.GenerateTip(self.getDoc1(), 'C', True)
        checkedA = False
        for tup in comps:
            if tup[0] == 'a':
                checkedA = True
                self.assert_('ADescription' in tup[1])

        self.assert_(checkedA)


    def getDoc2(self):
        s = \
'''
class C(object):
    def __init__(self):
        self.a = 1
        self.b = 2
'''
        return s

    def testEnv2(self):
        '''
        Now, check completion for C - should return object methods, 'a' and 'b'
        '''
        comps = simpleTipper.GenerateTip(self.getDoc2(), 'C', True)
#        print comps
        checkedA = False
        for tup in comps:
            if tup[0] == 'a':
                checkedA = True
        self.assert_(checkedA)
        

        
    def testImports(self):
        '''
        You can print the results to check...
        '''
        t =  importsTipper.GenerateTip('qt.QWidget') 
        for a in t:
            print a
#        importsTipper.GenerateTip('compiler.') 
#        importsTipper.GenerateImportsTip(['compiler']) 
#        importsTipper.GenerateImportsTip([ ] ) 
#        importsTipper.GenerateImportsTip(['os']) 
#        importsTipper.GenerateImportsTip(['os','path']) 
#        importsTipper.GenerateImportsTip(['unittest']) 
#        importsTipper.GenerateImportsTip(['compiler', 'ast']) 
#        importsTipper.GenerateImportsTip(['compiler', 'ast', 'Node']) 
        
        
    def testEnv3(self):
        comps = simpleTipper.GenerateTip(self.getDoc3(), None, False)
        
        
    def getDoc3(self):
        s= \
'''
import sys

class TestLocals(object):
    
    sys.path
'''
    
        return s


    def testEnv4(self):
        comps = simpleTipper.GenerateTip(self.getDoc4(), 'C', True)
        print comps
        
        
    def getDoc4(self):
        s= \
'''
class C(object):
    def metA(self, a, b):
        pass
        
'''
    
        return s

    def testInspect(self):
        return 
        
        class C(object):
            def metA(self, a, b):
                pass
        
        obj = C.metA
        if inspect.ismethod (obj):
            print obj.im_func
            print inspect.getargspec(obj.im_func)
            
        
        
if __name__ == '__main__':
    from coilib import unittest
    unittest.TestMethod(Test, 'Test.testImports')
#    unittest.main()
    
