# unit tests for the cache module

from logilab.common import get_cycles
import sys
import unittest

class getCycleTestCase(unittest.TestCase):

    def test_known0(self):
        self.assertEqual(get_cycles({1:[2], 2:[3], 3:[1]}), [[1, 2, 3]])
        
    def test_known1(self):
        self.assertEqual(get_cycles({1:[2], 2:[3], 3:[1, 4], 4:[3]}), [[1, 2, 3], [3, 4]])
        
    def test_known2(self):
        self.assertEqual(get_cycles({1:[2], 2:[3], 3:[0], 0:[]}), [])
        
def suite():
    loader = unittest.TestLoader()
    testsuite = loader.loadTestsFromModule(sys.modules[__name__])
    return testsuite
    
    
def Run():
    testsuite = suite()
    runner = unittest.TextTestRunner()
    return runner.run(testsuite)

if __name__ == "__main__":
    Run()
 

