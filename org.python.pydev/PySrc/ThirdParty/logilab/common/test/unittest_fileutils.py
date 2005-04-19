"""unit tests for logilab.common.fileutils

Some file / path manipulation utilities
"""
__revision__ = "$Id: unittest_fileutils.py,v 1.3 2005-04-19 14:39:13 fabioz Exp $"

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
        self.assertEqual(write_open_mode('toto.txt'), 'w')
        #self.assertEqual(write_open_mode('toto.xml'), 'w')
        self.assertEqual(write_open_mode('toto.bin'), 'wb')
        self.assertEqual(write_open_mode('toto.sxi'), 'wb')

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
                          [join('data', f) for f in ['newlines.txt',
                                                     'normal_file.txt',
                                                     join('sub', 'doc.txt'),
                                                     'write_protected_file.txt',
                                                     ]])

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

class ProtectedFileTC(unittest.TestCase):
    def setUp(self):
        self.rpath = 'data/write_protected_file.txt'
        self.rwpath = 'data/normal_file.txt'
        # Make sure rwpath is writable !
        os.chmod(self.rwpath, 33188)

    def test_mode_change(self):
        """tests that mode is changed when needed"""
        # test on non-writable file
        self.assertEquals(os.stat(self.rpath)[0], 33060)
        wp_file = ProtectedFile(self.rpath, 'w')
        self.assertEquals(os.stat(self.rpath)[0], 33188)
        # test on writable-file
        self.assertEquals(os.stat(self.rwpath)[0], 33188)
        wp_file = ProtectedFile(self.rwpath, 'w')
        self.assertEquals(os.stat(self.rwpath)[0], 33188)

    def test_restore_on_close(self):
        """tests original mode is restored on close"""
        # test on non-writable file
        self.assertEquals(os.stat(self.rpath)[0], 33060)
        ProtectedFile(self.rpath, 'w').close()
        self.assertEquals(os.stat(self.rpath)[0], 33060)
        # test on writable-file
        self.assertEquals(os.stat(self.rwpath)[0], 33188)
        ProtectedFile(self.rwpath, 'w').close()
        self.assertEquals(os.stat(self.rwpath)[0], 33188)

    def test_mode_change_on_append(self):
        """tests that mode is changed when file is opened in 'a' mode"""
        self.assertEquals(os.stat(self.rpath)[0], 33060)
        wp_file = ProtectedFile(self.rpath, 'a')
        self.assertEquals(os.stat(self.rpath)[0], 33188)
        wp_file.close()
        self.assertEquals(os.stat(self.rpath)[0], 33060)
        

from logilab.common.testlib import DocTest
class ModuleDocTest(DocTest):
    """relative_path embed tests in docstring"""
    from logilab.common import fileutils as module    
del DocTest # necessary if we don't want it to be executed (we don't...)

if __name__ == '__main__':
    unittest.main()
