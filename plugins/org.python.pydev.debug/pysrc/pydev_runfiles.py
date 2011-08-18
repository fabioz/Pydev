import fnmatch
import os.path
import re
import unittest
import pydev_runfiles_unittest
from pydevd_constants import * #@UnusedWildImport
import time
from pydev_runfiles_coverage import StartCoverageSupport


#=======================================================================================================================
# Configuration
#=======================================================================================================================
class Configuration:
    
    def __init__(
        self, 
        files_or_dirs='', 
        verbosity=2, 
        test_filter=None, 
        tests=None, 
        port=None, 
        files_to_tests=None, 
        jobs=1,
        split_jobs='tests',
        coverage_output_dir=None, 
        coverage_include=None,
        coverage_output_file=None, 
        ):
        self.files_or_dirs = files_or_dirs
        self.verbosity = verbosity
        self.test_filter = test_filter
        self.tests = tests
        self.port = port
        self.files_to_tests = files_to_tests
        self.jobs = jobs
        self.split_jobs = split_jobs
        
        self.coverage_output_dir = coverage_output_dir 
        self.coverage_include = coverage_include
        self.coverage_output_file = coverage_output_file
        

#=======================================================================================================================
# parse_cmdline
#=======================================================================================================================
def parse_cmdline(argv=None):
    """ parses command line and returns test directories, verbosity, test filter and test suites
        usage: 
            runfiles.py  -v|--verbosity <level>  -f|--filter <regex>  -t|--tests <Test.test1,Test2>  dirs|files
            
        Multiprocessing options:
        jobs=number (with the number of jobs to be used to run the tests)
        split_jobs='module'|'tests' 
            if == module, a given job will always receive all the tests from a module
            if == tests, the tests will be split independently of their originating module (default)
    """
    if argv is None:
        argv = sys.argv
        
    verbosity = 2
    test_filter = None
    tests = None
    port = None
    jobs = 1
    split_jobs = 'tests'
    files_to_tests = {}
    coverage_output_dir = None
    coverage_include = None

    from _pydev_getopt import gnu_getopt
    optlist, dirs = gnu_getopt(
        argv[1:], "v:f:t:p:c:j:s:d:i", 
        [
            "verbosity=", 
            "filter=", 
            "tests=", 
            "port=", 
            "config_file=", 
            "jobs=", 
            "split_jobs=", 
            "coverage_output_dir=", 
            "coverage_include=", 
        ]
    )
    
    for opt, value in optlist:
        if opt in ("-v", "--verbosity"):
            verbosity = value

        elif opt in ("-p", "--port"):
            port = int(value)

        elif opt in ("-j", "--jobs"):
            jobs = int(value)
            
        elif opt in ("-s", "--split_jobs"):
            split_jobs = value
            if split_jobs not in ('module', 'tests'):
                raise AssertionError('Expected split to be either "module" or "tests". Was :%s' % (split_jobs,))
            
        elif opt in ("-d", "--coverage_output_dir",):
            coverage_output_dir = value.strip()
            
        elif opt in ("-i", "--coverage_include",):
            coverage_include = value.strip()
            
        elif opt in ("-f", "--filter"):
            test_filter = value.split(',')

        elif opt in ("-t", "--tests"):
            tests = value.split(',')
            
        elif opt in ("-c", "--config_file"):
            config_file = value.strip()
            if os.path.exists(config_file):
                f = open(config_file, 'rU')
                try:
                    config_file_contents = f.read()
                finally:
                    f.close()
                    
                if config_file_contents:
                    config_file_contents = config_file_contents.strip()
                    
                if config_file_contents:
                    for line in config_file_contents.splitlines():
                        file_and_test = line.split('|')
                        if len(file_and_test) == 2:
                            file, test = file_and_test
                            if DictContains(files_to_tests, file):
                                files_to_tests[file].append(test)
                            else:
                                files_to_tests[file] = [test]  
                    
            else:
                sys.stderr.write('Could not find config file: %s\n' % (config_file,))

    if type([]) != type(dirs):
        dirs = [dirs]

    ret_dirs = []
    for d in dirs:
        if '|' in d:
            #paths may come from the ide separated by |
            ret_dirs.extend(d.split('|'))
        else:
            ret_dirs.append(d)

    return Configuration(
        ret_dirs, 
        int(verbosity), 
        test_filter, 
        tests, 
        port, 
        files_to_tests, 
        jobs, 
        split_jobs, 
        coverage_output_dir, 
        coverage_include, 
    )

            
     
#=======================================================================================================================
# PydevTestRunner
#=======================================================================================================================
class PydevTestRunner(object):
    """ finds and runs a file or directory of files as a unit test """

    __py_extensions = ["*.py", "*.pyw"]
    __exclude_files = ["__init__.*"]
    
    #Just to check that only this attributes will be written to this file
    __slots__= [
        'verbosity', #Always used
         
        'files_to_tests', #If this one is given, the ones below are not used
        
        'files_or_dirs', #Files or directories received in the command line
        'test_filter', #The filter used to collect the tests
        'tests',  #Strings with the tests to be run
        
        'jobs', #Integer with the number of jobs that should be used to run the test cases
        'split_jobs', #String with 'tests' or 'module' (how should the jobs be split)
        
        'configuration',
        'coverage',
    ]

    def __init__(self, configuration):
        self.verbosity = configuration.verbosity
        
        self.jobs = configuration.jobs
        self.split_jobs = configuration.split_jobs
        
        files_to_tests = configuration.files_to_tests
        if files_to_tests:
            self.files_to_tests = files_to_tests
            self.files_or_dirs = list(files_to_tests.keys())
            self.test_filter = None
            self.tests = None
        else:
            self.files_to_tests = {}
            self.files_or_dirs = configuration.files_or_dirs
            self.test_filter = self.__setup_test_filter(configuration.test_filter)
            self.tests = configuration.tests
            
        self.configuration = configuration
        self.__adjust_path()


    def __adjust_path(self):
        """ add the current file or directory to the python path """
        path_to_append = None
        for n in xrange(len(self.files_or_dirs)):
            dir_name = self.__unixify(self.files_or_dirs[n])
            if os.path.isdir(dir_name):
                if not dir_name.endswith("/"):
                    self.files_or_dirs[n] = dir_name + "/"
                path_to_append = os.path.normpath(dir_name)
            elif os.path.isfile(dir_name):
                path_to_append = os.path.dirname(dir_name)
            else:
                msg = ("unknown type. \n%s\nshould be file or a directory.\n" % (dir_name))
                raise RuntimeError(msg)
        if path_to_append is not None:
            #Add it as the last one (so, first things are resolved against the default dirs and 
            #if none resolves, then we try a relative import).
            sys.path.append(path_to_append)

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


    def find_import_files(self):
        """ return a list of files to import """
        if self.files_to_tests:
            return self.files_to_tests.keys()
        
        pyfiles = []

        for base_dir in self.files_or_dirs:
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

    def __get_module_from_str(self, modname, print_exception, pyfile):
        """ Import the module in the given import path.
            * Returns the "final" module, so importing "coilib40.subject.visu" 
            returns the "visu" module, not the "coilib40" as returned by __import__ """
        try:
            mod = __import__(modname)
            for part in modname.split('.')[1:]:
                mod = getattr(mod, part)
            return mod
        except:
            if print_exception:
                import pydev_runfiles_xml_rpc
                import pydevd_io
                buf_err = pydevd_io.StartRedirect(keep_original_redirection=True, std='stderr')
                buf_out = pydevd_io.StartRedirect(keep_original_redirection=True, std='stdout')
                try:
                    import traceback;traceback.print_exc()
                    sys.stderr.write('ERROR: Module: %s could not be imported (file: %s).\n' % (modname, pyfile))
                finally:
                    pydevd_io.EndRedirect('stderr')
                    pydevd_io.EndRedirect('stdout')
                
                pydev_runfiles_xml_rpc.notifyTest(
                    'error', buf_out.getvalue(), buf_err.getvalue(), pyfile, modname, 0)
                
            return None

    def find_modules_from_files(self, pyfiles):
        """ returns a list of modules given a list of files """
        #let's make sure that the paths we want are in the pythonpath...
        imports = [(s, self.__importify(s)) for s in pyfiles]

        system_paths = []
        for s in sys.path:
            system_paths.append(self.__importify(s, True))


        ret = []
        for pyfile, imp in imports:
            if imp is None:
                continue #can happen if a file is not a valid module
            choices = []
            for s in system_paths:
                if imp.startswith(s):
                    add = imp[len(s) + 1:]
                    if add:
                        choices.append(add)
                    #sys.stdout.write(' ' + add + ' ')

            if not choices:
                sys.stdout.write('PYTHONPATH not found for file: %s\n' % imp)
            else:
                for i, import_str in enumerate(choices):
                    print_exception = i == len(choices) - 1
                    mod = self.__get_module_from_str(import_str, print_exception, pyfile)
                    if mod is not None:
                        ret.append((pyfile, mod, import_str))
                        break


        return ret
    
    #===================================================================================================================
    # GetTestCaseNames
    #===================================================================================================================
    class GetTestCaseNames:
        """Yes, we need a class for that (cannot use outer context on jython 2.1)"""

        def __init__(self, accepted_classes, accepted_methods):
            self.accepted_classes = accepted_classes
            self.accepted_methods = accepted_methods

        def __call__(self, testCaseClass):
            """Return a sorted sequence of method names found within testCaseClass"""
            testFnNames = []
            className = testCaseClass.__name__

            if DictContains(self.accepted_classes, className):
                for attrname in dir(testCaseClass):
                    #If a class is chosen, we select all the 'test' methods'
                    if attrname.startswith('test') and hasattr(getattr(testCaseClass, attrname), '__call__'):
                        testFnNames.append(attrname)

            else:
                for attrname in dir(testCaseClass):
                    #If we have the class+method name, we must do a full check and have an exact match.
                    if DictContains(self.accepted_methods, className + '.' + attrname):
                        if hasattr(getattr(testCaseClass, attrname), '__call__'):
                            testFnNames.append(attrname)

            #sorted() is not available in jython 2.1
            testFnNames.sort()
            return testFnNames
        
        
    def _decorate_test_suite(self, suite, pyfile, module_name):
        if isinstance(suite, unittest.TestSuite):
            add = False
            suite.__pydev_pyfile__ = pyfile
            suite.__pydev_module_name__ = module_name
            
            for t in suite._tests:
                t.__pydev_pyfile__ = pyfile
                t.__pydev_module_name__ = module_name
                if self._decorate_test_suite(t, pyfile, module_name):
                    add = True
                    
            return add
                    
        elif isinstance(suite, unittest.TestCase):
            return True
        
        else:
            return False
                


    def find_tests_from_modules(self, file_and_modules_and_module_name):
        """ returns the unittests given a list of modules """
        #Use our own suite!
        unittest.TestLoader.suiteClass = pydev_runfiles_unittest.PydevTestSuite
        loader = unittest.TestLoader()
        
        ret = []
        if self.files_to_tests:
            for pyfile, m, module_name in file_and_modules_and_module_name:
                accepted_classes = {}
                accepted_methods = {}
                tests = self.files_to_tests[pyfile]
                for t in tests:
                    accepted_methods[t] = t
                
                loader.getTestCaseNames = self.GetTestCaseNames(accepted_classes, accepted_methods)
                
                suite = loader.loadTestsFromModule(m)
                if self._decorate_test_suite(suite, pyfile, module_name):
                    ret.append(suite)
            return ret
        
        
        if self.tests:
            accepted_classes = {}
            accepted_methods = {}

            for t in self.tests:
                splitted = t.split('.')
                if len(splitted) == 1:
                    accepted_classes[t] = t

                elif len(splitted) == 2:
                    accepted_methods[t] = t

            loader.getTestCaseNames = self.GetTestCaseNames(accepted_classes, accepted_methods)


        for pyfile, m, module_name in file_and_modules_and_module_name:
            suite = loader.loadTestsFromModule(m)
            if self._decorate_test_suite(suite, pyfile, module_name):
                ret.append(suite)

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



    def run_tests(self, handle_coverage=True):
        """ runs all tests """
        sys.stdout.write("Finding files... ")
        files = self.find_import_files()
        if self.verbosity > 3:
            sys.stdout.write('%s ... done.\n' % (self.files_or_dirs))
        else:
            sys.stdout.write('done.\n')
        sys.stdout.write("Importing test modules ... ")
        

        if handle_coverage:
            coverage_files, coverage = StartCoverageSupport(self.configuration)
        
        file_and_modules_and_module_name = self.find_modules_from_files(files)
        sys.stdout.write("done.\n")
        
        all_tests = self.find_tests_from_modules(file_and_modules_and_module_name)
        if self.test_filter or self.tests:

            if self.test_filter:
                sys.stdout.write('Test Filter: %s\n' % ([p.pattern for p in self.test_filter],))

            if self.tests:
                sys.stdout.write('Tests to run: %s\n' % (self.tests,))

            all_tests = self.filter_tests(all_tests)
            
        test_suite = unittest.TestSuite(all_tests)
        import pydev_runfiles_xml_rpc
        pydev_runfiles_xml_rpc.notifyTestsCollected(test_suite.countTestCases())
        
        executed_in_parallel = False
        start_time = time.time()
        if self.jobs > 1:
            import pydev_runfiles_parallel
            
            #What may happen is that the number of jobs needed is lower than the number of jobs requested
            #(e.g.: 2 jobs were requested for running 1 test) -- in which case ExecuteTestsInParallel will
            #return False and won't run any tests.
            executed_in_parallel = pydev_runfiles_parallel.ExecuteTestsInParallel(
                all_tests, self.jobs, self.split_jobs, self.verbosity, coverage_files, self.configuration.coverage_include)
            
        if not executed_in_parallel:
            #If in coverage, we don't need to pass anything here (coverage is already enabled for this execution).
            runner = pydev_runfiles_unittest.PydevTextTestRunner(stream=sys.stdout, descriptions=1, verbosity=self.verbosity)
            sys.stdout.write('\n')
            runner.run(test_suite)
            
        if handle_coverage:
            coverage.stop()
            coverage.save()
        
        total_time = 'Finished in: %.2f secs.' % (time.time() - start_time,)
        pydev_runfiles_xml_rpc.notifyTestRunFinished(total_time)


#=======================================================================================================================
# main
#=======================================================================================================================
def main(configuration):
    PydevTestRunner(configuration).run_tests()
