'''
@author Fabio Zadrozny 
'''

import unittest
import simpleTipper
    
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
        comps = simpleTipper.GenerateTip(self.getDoc1(), None)
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
        comps = simpleTipper.GenerateTip(self.getDoc1(), 'C')
        checkedA = False
        for tup in comps:
            if tup[0] == 'a':
                checkedA = True
                self.assert_('ADescription' in tup[1])

        self.assert_(checkedA)
        
if __name__ == '__main__':
    unittest.main()
