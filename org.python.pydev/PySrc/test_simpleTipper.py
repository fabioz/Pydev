'''
@author Fabio Zadrozny 
'''

from coilib import unittest
import simpleTipper
import importsTipper

class Test(unittest.TestCase):

    def setUp(self):
        unittest.TestCase.setUp(self)

    def tearDown(self):
        unittest.TestCase.tearDown(self)
    
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
        importsTipper.GenerateTip('qt.') 
        importsTipper.GenerateTip('scbr.') 
        importsTipper.GenerateImportsTip(['scbr']) 
        importsTipper.GenerateImportsTip([ ] ) 
        importsTipper.GenerateImportsTip(['os']) 
        importsTipper.GenerateImportsTip(['os','path']) 
        importsTipper.GenerateImportsTip(['unittest']) 
        importsTipper.GenerateImportsTip(['scbr', 'app', 'actions', 'db']) 
        importsTipper.GenerateImportsTip(['scbr', 'app', 'actions', 'db', 'EditCont']) 
        
        
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
        comps = simpleTipper.GenerateTip(self.getDoc4(), None, False)

    def getDoc4(self):
        s = \
'''import test


class KKKK(object):
    pass
    
f = KKKK()

d = test.LL()
d.vv()
'''
        return s

if __name__ == '__main__':
    
    from coilib import unittest
#    unittest.TestMethod(Test, 'Test.testEnv3')
    unittest.main()
    
