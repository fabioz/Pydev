"""unit tests for logilab.common.fileutils

Some file / path manipulation utilities
"""
__revision__ = "$Id: unittest_fileutils.py,v 1.2 2005-02-16 16:45:45 fabioz Exp $"

import unittest
import sys, os, tempfile, shutil
from os.path import join

from logilab.common.fileutils import *


#import data
DATA_DIR = 'data' #data.__path__[0]
NEWLINES_TXT = join(DATA_DIR,'newlines.txt')

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
        self.assertEqual(is_binary('toto.whatever'), 1)
        
class GetModeTC(unittest.TestCase):
    def test(self):
        self.assertEqual(get_mode('toto.txt'), 'w')
        #self.assertEqual(get_mode('toto.xml'), 'w')
        self.assertEqual(get_mode('toto.bin'), 'wb')
        self.assertEqual(get_mode('toto.sxi'), 'wb')

class NormReadTC(unittest.TestCase):
    def test_known_values_norm_read(self):
        data = norm_read(NEWLINES_TXT)
        self.assertEqual(data.strip(), '\n'.join(['# mixed new lines', '1', '2', '3']))


class LinesTC(unittest.TestCase):
    def test_known_values_lines(self):
        self.assertEqual(lines(NEWLINES_TXT),
                         ['# mixed new lines', '1', '2', '3'])
        
    def test_known_values_lines_comment(self):
        self.assertEqual(lines(NEWLINES_TXT, comments='#'),
                         ['1', '2', '3'])

class GetByExtTC(unittest.TestCase):
    def test_include(self):
        files = files_by_ext(DATA_DIR, include_exts=('.py',))
        files.sort()
        self.assertEquals(files,
                          [join('data', f) for f in ['__init__.py', 'module.py', 'module2.py',
                           'noendingnewline.py', 'nonregr.py', join('sub', 'momo.py')]])
        files = files_by_ext(DATA_DIR, include_exts=('.py',), exclude_dirs=('sub',))
        files.sort()
        self.assertEquals(files,
                          [join('data', f) for f in ['__init__.py', 'module.py', 'module2.py',
                           'noendingnewline.py', 'nonregr.py']])

    def test_exclude(self):
        files = files_by_ext(DATA_DIR, exclude_exts=('.py', '.pyc'))
        files.sort()
        self.assertEquals(files,
                          [join('data', f) for f in ['newlines.txt', join('sub', 'doc.txt')]])

    def test_exclude_base_dir(self):
        self.assertEquals(files_by_ext(DATA_DIR, include_exts=('.py',), exclude_dirs=(DATA_DIR)),
                          [])

class ExportTC(unittest.TestCase):
    def setUp(self):
        self.tempdir = tempfile.mktemp()
        os.mkdir(self.tempdir)

    def test(self):
        export('data', self.tempdir, verbose=0)
        self.assert_(exists(join(self.tempdir, '__init__.py')))
        self.assert_(exists(join(self.tempdir, 'sub')))
        self.assert_(not exists(join(self.tempdir, '__init__.pyc')))
        self.assert_(not exists(join(self.tempdir, 'CVS')))
        
    def tearDown(self):
        shutil.rmtree(self.tempdir)

from logilab.common.testlib import DocTest
class ModuleDocTest(DocTest):
    """relative_path embed tests in docstring"""
    from logilab.common import fileutils as module    
del DocTest # necessary if we don't want it to be executed (we don't...)

if __name__ == '__main__':
    unittest.main()
