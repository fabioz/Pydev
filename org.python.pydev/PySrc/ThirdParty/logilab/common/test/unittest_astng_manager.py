import unittest
import os
from os.path import join
from logilab.common.astng.manager import ASTNGManager


class ASTNGManagerTC(unittest.TestCase):
    def setUp(self):
        self.manager = ASTNGManager()

    def test_from_directory(self):
        obj = self.manager.from_directory('data')
        self.assertEquals(obj.name, 'data')
        self.assertEquals(obj.path, join(os.getcwd(), 'data'))
        
    def test_package_node(self):
        obj = self.manager.from_directory('data')
        expected_short = ['__init__', 'module', 'module2', 'noendingnewline', 'nonregr']
        expected_long = ['data.__init__', 'data.module', 'data.module2', 'data.noendingnewline', 'data.nonregr']
        self.assertEquals(obj.keys(), expected_short)
        self.assertEquals([m.name for m in obj.values()], expected_long)
        self.assertEquals([m for m in list(obj)], expected_short)
        self.assertEquals([(name, m.name) for name, m in obj.items()],
                          zip(expected_short, expected_long))
        #self.assertEquals(obj.has_key, [])
        #self.assertEquals(obj.get, [])
        #for key in obj:
        #    print key,
        #    print obj[key]
        
if __name__ == '__main__':
    unittest.main()

    
