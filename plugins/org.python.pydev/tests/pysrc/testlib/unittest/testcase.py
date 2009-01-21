'''
This module is suited for code completion tests.

It features a TestCase class that should have all methods inherited from the original unittest plus 6 added methods.
'''
import unittest


class TestCase(unittest.TestCase):

    def assertImagesNotEqual(self, f1, f2, *args, **kwargs):
        pass
        
        
    assertBMPsNotEqual = assertImagesNotEqual

    def assertImagesEqual(self, f1, f2, *args, **kwargs):
        pass

    
    assertBMPsEqual = assertImagesEqual


    def failIfRaises(self, excClass, callableObj, *args, **kwargs):
        pass

    assertNotRaises = failIfRaises


