# unit tests for the cache module

from logilab.common.cache import Cache
import sys
import unittest

class CacheTestCase(unittest.TestCase):

    def setUp(self):
        self.cache = Cache(5)

    def test_setitem1(self):
        """Checks that the setitem method works"""
        self.cache[1] = 'foo'
        self.assert_(self.cache.data[1] == 'foo',"1 : 'foo' is not in cache.data")
        self.assert_(len(self.cache._usage) == 1, "lenght of usage list is not 1")
        self.assert_(self.cache._usage[-1] == 1, '1 is not the most recently used key')
        self.assert_(self.cache._usage.sort() == self.cache.data.keys().sort(), "usage list and data keys are different")

    def test_setitem2(self):
        """Checks that the setitem method works for multiple items"""
        self.cache[1] = 'foo'
        self.cache[2] = 'bar'
        self.assert_(self.cache.data[2] == 'bar',"2 : 'bar' is not in cache.data")
        self.assert_(len(self.cache._usage) == 2, "lenght of usage list is not 2")
        self.assert_(self.cache._usage[-1] == 2, '1 is not the most recently used key')
        self.assert_(self.cache._usage.sort() == self.cache.data.keys().sort(), "usage list and data keys are different")

    def test_setitem3(self):
        """Checks that the setitem method works when replacing an element in the cache"""
        self.cache[1] = 'foo'
        self.cache[1] = 'bar'
        self.assert_(self.cache.data[1] == 'bar',"1 : 'bar' is not in cache.data")
        self.assert_(len(self.cache._usage) == 1, "lenght of usage list is not 1")
        self.assert_(self.cache._usage[-1] == 1, '1 is not the most recently used key')
        self.assert_(self.cache._usage.sort() == self.cache.data.keys().sort(), "usage list and data keys are different")

    def test_recycling1(self):
        """Checks the removal of old elements"""
        self.cache[1] = 'foo'
        self.cache[2] = 'bar'
        self.cache[3] = 'baz'
        self.cache[4] = 'foz'
        self.cache[5] = 'fuz'
        self.cache[6] = 'spam'
        self.assert_(not self.cache.data.has_key(1), 'key 1 has not been suppressed from the cache dictionnary')
        self.assert_(1 not in self.cache._usage, 'key 1 has not been suppressed from the cache LRU list')
        self.assert_(len(self.cache._usage) == 5, "lenght of usage list is not 5")
        self.assert_(self.cache._usage[-1] == 6, '6 is not the most recently used key')
        self.assert_(self.cache._usage.sort() == self.cache.data.keys().sort(), "usage list and data keys are different")

    def test_recycling2(self):
        """Checks that accessed elements get in the front of the list"""
        self.cache[1] = 'foo'
        self.cache[2] = 'bar'
        self.cache[3] = 'baz'
        self.cache[4] = 'foz'
        a = self.cache[1]
        self.assert_(a == 'foo')
        self.assert_(self.cache._usage[-1] == 1, '1 is not the most recently used key')
        self.assert_(self.cache._usage.sort() == self.cache.data.keys().sort(), "usage list and data keys are different")

    def test_delitem(self):
        """Checks that elements are removed from both element dict and element
        list.
        """
        self.cache['foo'] = 'bar'
        del self.cache['foo']
        self.assert_('foo' not in self.cache.data.keys(),"Element 'foo' was not removed cache dictionnary")
        self.assert_('foo' not in self.cache._usage,"Element 'foo' was not removed usage list")
        self.assert_(self.cache._usage.sort() == self.cache.data.keys().sort(), "usage list and data keys are different")


    def test_nullsize(self):
        """Checks that a 'NULL' size cache doesn't store anything
        """
        null_cache = Cache(0)
        null_cache['foo'] = 'bar'
        self.assert_(null_cache.size == 0, 'Cache size should be O, not %d' % \
                     null_cache.size)
        self.assert_(len(null_cache) == 0, 'Cache should be empty !')
        # Assert null_cache['foo'] raises a KeyError
        self.assertRaises(KeyError, null_cache.__getitem__, 'foo')
        # Deleting element should not raise error
        del null_cache['foo']
    
        
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
 

