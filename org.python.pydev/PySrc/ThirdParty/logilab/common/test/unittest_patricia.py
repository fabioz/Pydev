"""
unit tests for module logilab.common.patricia
"""

__revision__ = "$Id: unittest_patricia.py,v 1.2 2005-02-16 16:45:45 fabioz Exp $"

import unittest
import sys
from logilab.common.patricia import *

    
class PatriciaTrieClassTest(unittest.TestCase):
    
    def test_knownValues(self):
        """ 
        remove a child node
        """
        p = PatriciaTrie()
        i = 0
        words_list = ['maitre', 'maman', 'mange', 'manger', 'mangouste',
                      'manigance', 'manitou']
        words_list.sort()
        #
        for i in range(len(words_list)):
            p.insert(words_list[i], i)
        for i in range(len(words_list)):
            assert p.lookup(words_list[i]) == [i]
        try:
            p.lookup('not in list')
            raise AssertionError()
        except KeyError:
            pass
        #
        l = p.pfx_search('')
        l.sort()
        assert l == words_list
        l = p.pfx_search('ma')
        l.sort()
        assert l == words_list
        l = p.pfx_search('mai')
        assert l == ['maitre']
        l = p.pfx_search('not in list')
        assert l == []
        l = p.pfx_search('man', 2)
        assert l == ['mange']
        l = p.pfx_search('man', 1)
        assert l == []
        p.remove('maitre')
        try:
            p.lookup('maitre')
            raise AssertionError()
        except KeyError:
            pass
        #print p
    

def suite():
    """return the unitest suite"""
    loader = unittest.TestLoader()
    testsuite = loader.loadTestsFromModule(sys.modules[__name__])
    return testsuite
    
    
def Run(runner=None):
    """run tests"""
    testsuite = suite()
    if runner is None:
        runner = unittest.TextTestRunner()
    return runner.run(testsuite)
    
if __name__ == '__main__':
    Run()
