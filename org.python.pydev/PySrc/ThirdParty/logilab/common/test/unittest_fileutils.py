"""unit tests for logilab.common.fileutils

Some file / path manipulation utilities
"""
__revision__ = "$Id: unittest_fileutils.py,v 1.1 2005-01-21 17:46:21 fabioz Exp $"

import unittest
import sys
from os.path import join
from os import getcwd, linesep
from logilab.common import fileutils
from logilab.common.fileutils import *

from logilab.common.testlib import DocTest

#import data
DATA_DIR = 'data' #data.__path__[0]
NEWLINES_TXT = join(DATA_DIR,'newlines.txt')


class RelativePathDocTest(DocTest):
    """relative_path embed tests in docstring"""
    module = fileutils
    
    
class FirstleveldirectoryTC(unittest.TestCase):

    def test_known_values_first_level_directory(self):
        """return the first level directory of a path"""
        self.assertEqual(first_level_directory('truc/bidule/chouette'), 'truc', None)
        self.assertEqual(first_level_directory('/truc/bidule/chouette'), '/', None)
        
class IsBinaryTC(unittest.TestCase):
    def test(self):
        self.assertEqual(is_binary('toto.txt'), 0)
        #self.assertEqual(is_binary('toto.xml'), 0)
        self.assertEqual(is_binary('toto.bin'), 1)
        self.assertEqual(is_binary('toto.sxi'), 1)
        
class GetModeTC(unittest.TestCase):
    def test(self):
        self.assertEqual(get_mode('toto.txt'), 'w')
        #self.assertEqual(get_mode('toto.xml'), 'w')
        self.assertEqual(get_mode('toto.bin'), 'wb')
        self.assertEqual(get_mode('toto.sxi'), 'wb')

class NormReadTC(unittest.TestCase):
    def test_known_values_norm_read(self):
        data = norm_read(NEWLINES_TXT)
        self.assertEqual(data, linesep.join(['# mixed new lines', '1', '2', '3', '']))


class LinesTC(unittest.TestCase):
    def test_known_values_lines(self):
        self.assertEqual(lines(NEWLINES_TXT),
                         ['# mixed new lines', '1', '2', '3'])
        
    def test_known_values_lines_comment(self):
        self.assertEqual(lines(NEWLINES_TXT, comments='#'),
                         ['1', '2', '3'])

class GetByExtTC(unittest.TestCase):
    def test_kv_include(self):
        files = get_by_ext(DATA_DIR, include_exts=('.py',))
        files.sort()
        self.assertEquals(files,
                          ['data/__init__.py', 'data/module.py', 'data/module2.py',
                           'data/noendingnewline.py', 'data/nonregr.py', 'data/sub/momo.py'])

    def test_kv_exclude(self):
        files = get_by_ext(DATA_DIR, exclude_exts=('.py', '.pyc'))
        files.sort()
        self.assertEquals(files,
                          ['data/newlines.txt', 'data/sub/doc.txt'])

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
        # uncomment next line to write tests results in a file
        #runner.__init__(open('tests.log','w+'))    
    return runner.run(testsuite)
        
if __name__ == '__main__':
    unittest.main()
