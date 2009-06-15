
import fnmatch
import getopt
import os.path
import re
import sys
import unittest


try:
    __setFalse = False
except:
    import __builtin__
    setattr(__builtin__, 'True', 1)
    setattr(__builtin__, 'False', 0)


try:
    xrange
except:
    #Python 3k does not have it
    xrange = range

#=======================================================================================================================
# parse_cmdline
#=======================================================================================================================
def parse_cmdline():
    """ parses command line and returns test directories, verbosity, test filter and test suites
        usage: 
            runfiles.py  -v|--verbosity <level>  -f|--filter <regex>  -t|--tests <Test.test1,Test2>  dirs|files
    """
    verbosity = 2
    test_filter = None
    tests = None

    optlist, dirs = getopt.getopt(sys.argv[1:], "v:f:t:", ["verbosity=", "filter=", "tests="])
    for opt, value in optlist:
        if opt in ("-v", "--verbosity"):
            verbosity = value
            
        elif opt in ("-f", "--filter"):
            test_filter = value.split(',')
                
        elif opt in ("-t", "--tests"):
            tests = value.split(',')
                
    if type([]) != type(dirs):
        dirs = [dirs]

    ret_dirs = []
    for d in dirs:
        if '|' in d:
            #paths may come from the ide separated by |
            ret_dirs.extend(d.split('|'))
        else:
            ret_dirs.append(d)

    return ret_dirs, int(verbosity), test_filter, tests


#=======================================================================================================================
# PydevTestRunner
#=======================================================================================================================
class PydevTestRunner:
    """ finds and runs a file or directory of files as a unit test """

    __py_extensions = ["*.py", "*.pyw"]
    __exclude_files = ["__init__.*"]

    def __init__(self, test_dir, test_filter=None, verbosity=2, tests=None):
        self.test_dir = test_dir
        self.__adjust_path()
        self.test_filter = self.__setup_test_filter(test_filter)
        self.verbosity = verbosity
        self.tests = tests
        

    def __adjust_path(self):
        """ add the current file or directory to the python path """
        path_to_append = None
        for n in xrange(len(self.test_dir)):
            dir_name = self.__unixify(self.test_dir[n])
            if os.path.isdir(dir_name):
                if not dir_name.endswith("/"):
                    self.test_dir[n] = dir_name + "/"
                path_to_append = os.path.normpath(dir_name)
            elif os.path.isfile(dir_name):
                path_to_append = os.path.dirname(dir_name)
            else:
                msg = ("unknown type. \n%s\nshould be file or a directory.\n" % (dir_name))
                raise RuntimeError(msg)
        if path_to_append is not None:
            sys.path.insert(0, path_to_append)
        return

    def __setup_test_filter(self, test_filter):
        """ turn a filter string into a list of filter regexes """
        if test_filter is None or len(test_filter) == 0:
            return None
        return [re.compile("test%s" % f) for f in test_filter]

    def __is_valid_py_file(self, fname):
        """ tests that a particular file contains the proper file extension 
            and is not in the list of files to exclude """
        is_valid_fname = 0
        for invalid_fname in self.__class__.__exclude_files:
            is_valid_fname += int(not fnmatch.fnmatch(fname, invalid_fname))
        if_valid_ext = 0
        for ext in self.__class__.__py_extensions:
            if_valid_ext += int(fnmatch.fnmatch(fname, ext))
        return is_valid_fname > 0 and if_valid_ext > 0

    def __unixify(self, s):
        """ stupid windows. converts the backslash to forwardslash for consistency """
        return os.path.normpath(s).replace(os.sep, "/")

    def __importify(self, s, dir=False):
        """ turns directory separators into dots and removes the ".py*" extension 
            so the string can be used as import statement """
        if not dir:
            dirname, fname = os.path.split(s)

            if fname.count('.') > 1:
                #if there's a file named xxx.xx.py, it is not a valid module, so, let's not load it...
                return

            imp_stmt_pieces = [dirname.replace("\\", "/").replace("/", "."), os.path.splitext(fname)[0]]

            if len(imp_stmt_pieces[0]) == 0:
                imp_stmt_pieces = imp_stmt_pieces[1:]

            return ".".join(imp_stmt_pieces)

        else: #handle dir
            return s.replace("\\", "/").replace("/", ".")

    def __add_files(self, pyfiles, root, files):
        """ if files match, appends them to pyfiles. used by os.path.walk fcn """
        for fname in files:
            if self.__is_valid_py_file(fname):
                name_without_base_dir = self.__unixify(os.path.join(root, fname))
                pyfiles.append(name_without_base_dir)
        return


    def find_import_files(self):
        """ return a list of files to import """
        pyfiles = []

        for base_dir in self.test_dir:
            if os.path.isdir(base_dir):
                if hasattr(os, 'walk'):
                    for root, dirs, files in os.walk(base_dir):
                        self.__add_files(pyfiles, root, files)
                else:
                    # jython2.1 is too old for os.walk!
                    os.path.walk(base_dir, self.__add_files, pyfiles)

            elif os.path.isfile(base_dir):
                pyfiles.append(base_dir)

        return pyfiles

    def __get_module_from_str(self, modname):
        """ Import the module in the given import path.
            * Returns the "final" module, so importing "coilib40.subject.visu" 
            returns the "visu" module, not the "coilib40" as returned by __import__ """
        try:
            mod = __import__(modname)
            for part in modname.split('.')[1:]:
                mod = getattr(mod, part)
            return mod
        except:
            import traceback;traceback.print_exc()
            sys.stderr.write('ERROR: Module: %s could not be imported.\n' % (modname,))
            return None

    def find_modules_from_files(self, pyfiles):
        """ returns a lisst of modules given a list of files """
        #let's make sure that the paths we want are in the pythonpath...
        imports = [self.__importify(s) for s in pyfiles]

        system_paths = []
        for s in sys.path:
            system_paths.append(self.__importify(s, True))


        new_imports = []
        for imp in imports:
            if imp is None:
                continue #can happen if a file is not a valid module
            for s in system_paths:
                if imp.startswith(s):
                    new_imports.append(imp[len(s) + 1:])
                    break
            else:
                sys.stdout.write('PYTHONPATH not found for file: %s\n' % imp)

        imports = new_imports
        ret = [self.__get_module_from_str(import_str) for import_str in imports if import_str is not None]
        return ret

    def find_tests_from_modules(self, modules):
        """ returns the unittests given a list of modules """
        loader = unittest.defaultTestLoader
        
        ret = []
        loaded = 0
        if self.tests:
            prefixes = []
            for t in self.tests:
                splitted = t.split('.')
                if len(splitted) == 2:
                    prefixes.append(splitted[1])
                    
            if prefixes:
                #If we have any forced prefix, only load matching test names (don't load all the default tests
                #because they'll be filtered away anyways)
                loaded = 1
                for prefix in prefixes:
                    loader = unittest.TestLoader()
                    initial = loader.testMethodPrefix
                    try:
                        loader.testMethodPrefix = prefix
                        ret.extend([loader.loadTestsFromModule(m) for m in modules])
                    finally:
                        loader.testMethodPrefix = initial
        
        if not loaded:
            #Now, if we didn't have any prefixes to load, load the default modules
            ret.extend([loader.loadTestsFromModule(m) for m in modules])
            
        return ret


    def filter_tests(self, test_objs):
        """ based on a filter name, only return those tests that have
            the test case names that match """
        test_suite = []
        for test_obj in test_objs:
            
            if isinstance(test_obj, unittest.TestSuite):
                if test_obj._tests:
                    test_obj._tests = self.filter_tests(test_obj._tests)
                    if test_obj._tests:
                        test_suite.append(test_obj)
                
            elif isinstance(test_obj, unittest.TestCase):
                test_cases = []
                for tc in test_objs:
                    try:
                        testMethodName = tc._TestCase__testMethodName
                    except AttributeError:
                        #changed in python 2.5
                        testMethodName = tc._testMethodName

                    if self.__match(self.test_filter, testMethodName) and self.__match_tests(self.tests, tc, testMethodName):
                        test_cases.append(tc)
                return test_cases
        return test_suite


    def __match_tests(self, tests, test_case, test_method_name):
        if not tests:
            return 1
        
        for t in tests:
            class_and_method = t.split('.')
            if len(class_and_method) == 1:
                #only class name
                if class_and_method[0] == test_case.__class__.__name__:
                    return 1
                
            elif len(class_and_method) == 2:
                if class_and_method[0] == test_case.__class__.__name__ and class_and_method[1] == test_method_name:
                    return 1
                
        return 0
                
                
        

    def __match(self, filter_list, name):
        """ returns whether a test name matches the test filter """
        if filter_list is None:
            return 1
        for f in filter_list:
            if re.match(f, name):
                return 1
        return 0


    def run_tests(self):
        """ runs all tests """
        sys.stdout.write("Finding files...\n")
        files = self.find_import_files()
        sys.stdout.write('%s %s\n' % (self.test_dir, '... done'))
        sys.stdout.write("Importing test modules ... ")
        modules = self.find_modules_from_files(files)
        sys.stdout.write("done.\n")
        all_tests = self.find_tests_from_modules(modules)
        if self.test_filter or self.tests:
            
            if self.test_filter:
                sys.stdout.write('Test Filter: %s' % ([p.pattern for p in self.test_filter],))
                
            if self.tests:
                sys.stdout.write('Tests to run: %s' % (self.tests,))
                
            all_tests = self.filter_tests(all_tests)
            
        sys.stdout.write('\n')
        runner = unittest.TextTestRunner(stream=sys.stdout, descriptions=1, verbosity=verbosity)
        runner.run(unittest.TestSuite(all_tests))
        return

#=======================================================================================================================
# main        
#=======================================================================================================================
if __name__ == '__main__':
    dirs, verbosity, test_filter, tests = parse_cmdline()
    PydevTestRunner(dirs, test_filter, verbosity, tests).run_tests()
