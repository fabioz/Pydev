"""unittest module for logilab.comon.testlib"""

__revision__ = '$Id: unittest_testlib.py,v 1.1 2005-02-16 16:45:45 fabioz Exp $'

import unittest
from logilab.common import testlib

class MockTestCase(testlib.TestCase):
    def __init__(self):
        # Do not call unittest.TestCase's __init__
        pass

    def fail(self, msg):
        raise AssertionError(msg)

class TestlibTC(unittest.TestCase):

    def setUp(self):
        self.tc = MockTestCase()

    def test_dict_equals(self):
        """tests TestCase.assertDictEquals"""
        d1 = {'a' : 1, 'b' : 2}
        d2 = {'a' : 1, 'b' : 3}
        d3 = dict(d1)
        self.assertRaises(AssertionError, self.tc.assertDictEquals, d1, d2)
        self.tc.assertDictEquals(d1, d3)
        self.tc.assertDictEquals(d3, d1)
        self.tc.assertDictEquals(d1, d1)

    def test_list_equals(self):
        """tests TestCase.assertListEquals"""
        l1 = range(10)
        l2 = range(5)
        l3 = range(10)
        self.assertRaises(AssertionError, self.tc.assertListEquals, l1, l2)
        self.tc.assertListEquals(l1, l1)
        self.tc.assertListEquals(l1, l3)
        self.tc.assertListEquals(l3, l1)

    def test_lines_equals(self):
        """tests assertLineEquals"""
        t1 = """some
        text
"""
        t2 = """some
        
        text"""
        t3 = """some
        text"""
        self.assertRaises(AssertionError, self.tc.assertLinesEquals, t1, t2)
        self.tc.assertLinesEquals(t1, t3)
        self.tc.assertLinesEquals(t3, t1)
        self.tc.assertLinesEquals(t1, t1)

    def test_xml_valid(self):
        """tests xml is valid"""
        valid = """<root>
        <hello />
        <world>Logilab</world>
        </root>"""
        invalid = """<root><h2> </root>"""
        self.tc.assertXMLStringValid(valid)
        self.assertRaises(AssertionError, self.tc.assertXMLStringValid, invalid)
        invalid = """<root><h2 </h2> </root>"""
        self.assertRaises(AssertionError, self.tc.assertXMLStringValid, invalid)
        
    
if __name__ == '__main__':
    unittest.main()

