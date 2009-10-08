import os
import sys


# stupid jython. plain old __file__ isnt working for some reason
import test_runfiles #@UnresolvedImport - importing the module itself
this_file_name = test_runfiles.__file__

desired_runfiles_path = os.path.normpath(os.path.dirname(this_file_name) + "/..")
sys.path.insert(0, desired_runfiles_path)

#remove existing runfiles from modules (if any), so that we can be sure we have the correct version
if 'runfiles' in sys.modules:
    del sys.modules['runfiles']


import runfiles
import unittest
import tempfile
import re

#this is an early test because it requires the sys.path changed
orig_syspath = sys.path
a_file = runfiles.__file__
runfiles.PydevTestRunner(test_dir=[a_file])
file_dir = os.path.dirname(a_file)
assert file_dir in sys.path
sys.path = orig_syspath[:]

#remove it so that we leave it ok for other tests
sys.path.remove(desired_runfiles_path)

class RunfilesTest(unittest.TestCase):
    def _setup_scenario(self, path, t_filter, tests=None):
        self.MyTestRunner = runfiles.PydevTestRunner(test_dir=path,
                                                     test_filter=t_filter,
                                                     verbosity=1,
                                                     tests=tests)
        self.files = self.MyTestRunner.find_import_files()
        self.modules = self.MyTestRunner.find_modules_from_files(self.files)
        self.all_tests = self.MyTestRunner.find_tests_from_modules(self.modules)
        self.filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)

    def setUp(self):
        self.file_dir = [os.path.abspath(os.path.join(desired_runfiles_path, 'tests/samples'))]
        self._setup_scenario(self.file_dir, None)

    def test_parse_cmdline(self):
        sys.argv = "runfiles.py ./".split()
        test_dir, verbosity, test_filter, tests = runfiles.parse_cmdline()
        self.assertEquals([sys.argv[1]], test_dir)
        self.assertEquals(2, verbosity)        # default value
        self.assertEquals(None, test_filter)   # default value

        sys.argv = "runfiles.py ../images c:/temp".split()
        test_dir, verbosity, test_filter, tests = runfiles.parse_cmdline()
        self.assertEquals(sys.argv[1:3], test_dir)
        self.assertEquals(2, verbosity)

        sys.argv = "runfiles.py --verbosity 3 ../junk c:/asdf ".split()
        test_dir, verbosity, test_filter, tests = runfiles.parse_cmdline()
        self.assertEquals(sys.argv[3:], test_dir)
        self.assertEquals(int(sys.argv[2]), verbosity)

        sys.argv = "runfiles.py -f Abc.test_def ./".split()
        test_dir, verbosity, test_filter, tests = runfiles.parse_cmdline()
        self.assertEquals([sys.argv[-1]], test_dir)
        self.assertEquals([sys.argv[2]], test_filter)

        sys.argv = "runfiles.py -f Abc.test_def,Mod.test_abc c:/junk/".split()
        test_dir, verbosity, test_filter, tests = runfiles.parse_cmdline()
        self.assertEquals([sys.argv[-1]], test_dir)
        self.assertEquals(sys.argv[2].split(','), test_filter)

        sys.argv = ('C:\\eclipse-SDK-3.2-win32\\eclipse\\plugins\\org.python.pydev.debug_1.2.2\\pysrc\\runfiles.py ' + 
                    '--verbosity 1 ' + 
                    'C:\\workspace_eclipse\\fronttpa\\tests\\gui_tests\\calendar_popup_control_test.py ').split()
        test_dir, verbosity, test_filter, tests = runfiles.parse_cmdline()
        self.assertEquals([sys.argv[-1]], test_dir)
        self.assertEquals(1, verbosity)

        sys.argv = "runfiles.py --verbosity 1 -f Mod.test_abc c:/junk/ ./".split()
        test_dir, verbosity, test_filter, tests = runfiles.parse_cmdline()
        self.assertEquals(sys.argv[5:], test_dir)
        self.assertEquals(int(sys.argv[2]), verbosity)
        self.assertEquals([sys.argv[4]], test_filter)
        return
    
    def test___adjust_python_path_works_for_directories(self):
        orig_syspath = sys.path
        tempdir = tempfile.gettempdir()
        runfiles.PydevTestRunner(test_dir=[tempdir])
        self.assertEquals(1, tempdir in sys.path)
        sys.path = orig_syspath[:]
    
    
    def test___adjust_python_path_breaks_for_unkown_type(self):
        self.assertRaises(RuntimeError, runfiles.PydevTestRunner, ["./LIKE_THE_NINJA_YOU_WONT_FIND_ME.txt"])

    def test___setup_test_filter(self):
        setup_tf = self.MyTestRunner._PydevTestRunner__setup_test_filter
        self.assertEquals (None, setup_tf(""))
        self.assertEquals (None, setup_tf(None))
        self.assertEquals ([re.compile("test.*")], setup_tf([".*"]))
        self.assertEquals ([re.compile("test.*"), re.compile("test^$")], setup_tf([".*", "^$"]))
    
    def test___is_valid_py_file(self):
        isvalid = self.MyTestRunner._PydevTestRunner__is_valid_py_file
        self.assertEquals(1, isvalid("test.py"))
        self.assertEquals(0, isvalid("asdf.pyc"))
        self.assertEquals(0, isvalid("__init__.py"))
        self.assertEquals(0, isvalid("__init__.pyc"))
        self.assertEquals(1, isvalid("asdf asdf.pyw"))

    def test___unixify(self):
        unixify = self.MyTestRunner._PydevTestRunner__unixify
        self.assertEquals("c:/temp/junk/asdf.py", unixify("c:SEPtempSEPjunkSEPasdf.py".replace('SEP', os.sep)))

    def test___importify(self):
        importify = self.MyTestRunner._PydevTestRunner__importify
        self.assertEquals("temp.junk.asdf", importify("temp/junk/asdf.py"))
        self.assertEquals("asdf", importify("asdf.py"))
        self.assertEquals("abc.def.hgi", importify("abc/def/hgi"))
        
    def test_finding_a_file_from_file_system(self):
        test_file = "simple_test.py"
        self.MyTestRunner.test_dir = [self.file_dir[0] + test_file]
        files = self.MyTestRunner.find_import_files()
        self.assertEquals(1, len(files))
        self.assertEquals(files[0], self.file_dir[0] + test_file)

    def test_finding_files_in_dir_from_file_system(self):
        self.assertEquals(1, len(self.files) > 0)
        for import_file in self.files:
            self.assertEquals(-1, import_file.find(".pyc"))
            self.assertEquals(-1, import_file.find("__init__.py"))
            self.assertEquals(-1, import_file.find("\\"))
            self.assertEquals(-1, import_file.find(".txt"))

    def test___get_module_from_str(self):
        my_importer = self.MyTestRunner._PydevTestRunner__get_module_from_str
        my_os_path = my_importer("os.path", True)
        from os import path
        import os.path as path2
        self.assertEquals(path, my_os_path)
        self.assertEquals(path2, my_os_path)
        self.assertNotEquals(__import__("os.path"), my_os_path)
        self.assertNotEquals(__import__("os"), my_os_path)

    def test_finding_modules_from_import_strings(self):
        self.assertEquals(1, len(self.modules) > 0)

    def test_finding_tests_when_no_filter(self):
        # unittest.py will create a TestCase with 0 tests in it
        # since it just imports what is given
        self.assertEquals(1, len(self.all_tests) > 0)
        files_with_tests = [1 for t in self.all_tests if len(t._tests) > 0]
        self.assertNotEquals(len(self.files), len(files_with_tests))
        
    def count_tests(self, tests):
        total = 0
        for t in tests:
            total += t.countTestCases()
        return total

    def test___match(self):
        matcher = self.MyTestRunner._PydevTestRunner__match
        self.assertEquals(1, matcher(None, "aname"))
        self.assertEquals(1, matcher([".*"], "aname"))
        self.assertEquals(0, matcher(["^x$"], "aname"))
        self.assertEquals(0, matcher(["abc"], "aname"))
        self.assertEquals(1, matcher(["abc", "123"], "123"))

    def test_finding_tests_from_modules_with_bad_filter_returns_0_tests(self):
        self._setup_scenario(self.file_dir, ["NO_TESTS_ARE_SURE_TO_HAVE_THIS_NAME"])
        self.assertEquals(0, self.count_tests(self.all_tests))
        
    def test_finding_test_with_unique_name_returns_1_test(self):
        self._setup_scenario(self.file_dir, ["_i_am_a_unique_test_name"])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEquals(1, self.count_tests(filtered_tests))

    def test_finding_test_with_non_unique_name(self):
        self._setup_scenario(self.file_dir, ["_non_unique_name"])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEquals(1, self.count_tests(filtered_tests) > 2)

    def test_finding_tests_with_regex_filters(self):
        self._setup_scenario(self.file_dir, ["_non.*"])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEquals(1, self.count_tests(filtered_tests) > 2)

        self._setup_scenario(self.file_dir, ["^$"])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEquals(0, self.count_tests(filtered_tests))

        self._setup_scenario(self.file_dir, ["_[x]+.*$"])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEquals(1, self.count_tests(filtered_tests) > 0)

        self._setup_scenario(self.file_dir, ["_[x]+.*$", "_non.*"])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEquals(1, self.count_tests(filtered_tests) > 0)

        self._setup_scenario(self.file_dir, ["I$^NVALID_REGE$$$X$#@!"])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEquals(0, self.count_tests(filtered_tests))
        
    def test_matching_tests(self):
        self._setup_scenario(self.file_dir, None, ['StillYetAnotherSampleTest'])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEqual(1, self.count_tests(filtered_tests))
        
        self._setup_scenario(self.file_dir, None, ['SampleTest.test_xxxxxx1'])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEqual(1, self.count_tests(filtered_tests))
        
        self._setup_scenario(self.file_dir, None, ['SampleTest'])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEqual(8, self.count_tests(filtered_tests))
        
        self._setup_scenario(self.file_dir, None, ['AnotherSampleTest.todo_not_tested'])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEqual(1, self.count_tests(filtered_tests))
        
        self._setup_scenario(self.file_dir, None, ['StillYetAnotherSampleTest', 'SampleTest.test_xxxxxx1'])
        filtered_tests = self.MyTestRunner.filter_tests(self.all_tests)
        self.assertEqual(2, self.count_tests(filtered_tests))

        

if __name__ == "__main__":
    #this is so that we can run it frem the jython tests -- because we don't actually have an __main__ module
    #(so, it won't try importing the __main__ module)
    unittest.TextTestRunner().run(unittest.makeSuite(RunfilesTest))
