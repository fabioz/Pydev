'''
Created on Aug 22, 2011

@author: hussain.bohra
'''

import os
import sys
import unittest

global tempdir

class Test(unittest.TestCase):
    """Test cases to validate custom property implementation in pydevd 
    """
    
    def setUp(self, nused=None):
        global tempdir
        import test_pydevd_property #@UnresolvedImport - importing itself
        tempdir = os.path.join(os.path.dirname(os.path.dirname(test_pydevd_property.__file__)))
        sys.path.insert(0, tempdir)
    
    def tearDown(self, unused=None):
        global tempdir
        sys.path.remove(tempdir)
                
    def testPropertyReplace(self):
        """Test case to validate replacement of the actual property by custom property
        """
        import pydevd_traceproperty
        pydevd_traceproperty.replace_builtin_property()
        x = property()
        self.assertEqual(x.__class__, pydevd_traceproperty.DebugProperty)

    def testProperty(self):
        """Test case to validate custom property
        """
        import pydevd_traceproperty

        class TestProperty():
            def __init__(self):
                pass
            def get_name(self):
                return self.__name
            def set_name(self, value):
                self.__name = value
            def del_name(self):
                del self.__name
            pydevd_traceproperty.replace_builtin_property()
            name = property(get_name, set_name, del_name, "name's docstring")
        
        testObj = TestProperty()
        testObj.name = "Custom"
        self.assertEqual(testObj.name, "Custom")
        
if __name__ == '__main__':
    #this is so that we can run it from the jython tests -- because we don't actually have an __main__ module
    #(so, it won't try importing the __main__ module)
    unittest.TextTestRunner().run(unittest.makeSuite(Test))
    