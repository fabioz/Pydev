
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
    __builtin__.True = 1
    __builtin__.False = 0

#=======================================================================================================================
# parse_cmdline
#=======================================================================================================================
def parse_cmdline():
    """ parses command line and returns test directories, verbosity, and test filter
        usage: 
            runfiles.py  -v|--verbosity <level>  -f|--filter <regex>  dirs|files
    """
    verbosity = 2
    test_filter = None
    
    optlist, dirs = getopt.getopt(sys.argv[1:], "v:f:", ["verbosity=", "filter="])
    for opt,value in optlist:
        if opt in ("-v","--verbosity"):
            verbosity = value
        elif opt in ("-f","--filter"):
            if "," in value:
                test_filter = value.split(',')
            else:
                test_filter = [value]
    if type([]) != type(dirs):
        dirs = [dirs]
        
    ret_dirs = []
    for d in dirs:
        if '|' in d:
            #paths may come from the ide separated by |
            ret_dirs.extend(d.split('|'))
        else:
            ret_dirs.append(d)
            
    return ret_dirs, int(verbosity), test_filter    


#=======================================================================================================================
# PydevTestRunner
#=======================================================================================================================
class PydevTestRunner:
    """ finds and runs a file or directory of files as a unit test """
    
    __py_extensions = ["*.py", "*.pyw"]
    __exclude_files = ["__init__.*"]
    
    def __init__(self, test_dir, test_filter=None, verbosity=2):
        self.test_dir = test_dir
        self.__adjust_path()
        self.test_filter = self.__setup_test_filter(test_filter)
        self.verbosity = verbosity

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
                msg = ("unknown type. \n%s\nshould be file or a directory.\n" % (dir_name) )
                raise RuntimeError(msg)
        if path_to_append is not None:
            sys.path.insert(0, path_to_append)
        return

    def __setup_test_filter(self, test_filter):
        """ turn a filter string into a list of filter regexes """
        if test_filter is None or len(test_filter) == 0:
            return None
        return [re.compile("test%s"%f) for f in test_filter]

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
        return os.path.normpath(s).replace( os.sep, "/" )

    def __importify(self, s, dir=False):
        """ turns directory separators into dots and removes the ".py*" extension 
            so the string can be used as import statement """
        if not dir:
            dirname, fname = os.path.split(s)
        
            if fname.count('.') > 1:
                #if there's a file named xxx.xx.py, it is not a valid module, so, let's not load it...
                return 
            
            imp_stmt_pieces = [dirname.replace("\\","/").replace("/", "."), os.path.splitext(fname)[0]]
            
            if len(imp_stmt_pieces[0]) == 0:
                imp_stmt_pieces = imp_stmt_pieces[1:]
                
            return ".".join(imp_stmt_pieces)
        
        else: #handle dir
            return s.replace("\\","/").replace("/", ".")

    def __add_files(self, pyfiles, root, files):
        """ if files match, appends them to pyfiles. used by os.path.walk fcn """
        for fname in files:
            if self.__is_valid_py_file(fname):
                name_without_base_dir = self.__unixify(os.path.join(root, fname)) 
                pyfiles.append( name_without_base_dir )
        return

    
    def find_import_files(self):
        """ return a list of files to import """
        pyfiles = []
        
        for base_dir in self.test_dir:
            if os.path.isdir(base_dir):
                # argh, it would be nice to use os.walk, but jython2.1 is too old
                os.path.walk(base_dir, self.__add_files, pyfiles)
                
            elif os.path.isfile(base_dir):
                pyfiles.append(base_dir) 
                
        return pyfiles
    
    def __get_module_from_str(self, modname):
        """ Import the module in the given import path.
            * Returns the "final" module, so importing "coilib40.subject.visu" 
            return the "visu" module, not the "coilib40" as returned by __import__ """
        try:
            mod = __import__( modname )
            for part in modname.split('.')[1:]:
                mod = getattr(mod, part)
            return mod
        except ImportError:
            import traceback;traceback.print_exc()
            print >> sys.stderr, 'ERROR: Module: %s could not be imported (alternative reason: the dir does not have __init__.py folders for all the packages?)' % (modname,)
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
                    new_imports.append(imp[len(s)+1:])
                    break
            else:
                print 'PYTHONPATH not found for file: %s' % imp
                
        imports = new_imports
        ret = [self.__get_module_from_str(import_str) for import_str in imports if import_str is not None]
        return ret
    
    def find_tests_from_modules(self, modules):
        """ returns the unittests given a list of modules """
        return [unittest.defaultTestLoader.loadTestsFromModule(m) for m in modules]

    def filter_tests(self, test_objs):
        """ based on a filter name, only return those tests that have
            the test case names that match """
        test_suite = []
        for test_obj in test_objs:
            if isinstance(test_obj, unittest.TestSuite):
                test_obj._tests = self.filter_tests(test_obj._tests)
                test_suite.append( test_obj )
            elif isinstance(test_obj, unittest.TestCase):
                test_cases = []
                for tc in test_objs:
                    try:
                        testMethodName = tc._TestCase__testMethodName
                    except AttributeError:
                        #changed in python 2.5
                        testMethodName = tc._testMethodName
                        
                    if self.__match(self.test_filter, testMethodName):
                        test_cases.append( tc )
                return test_cases
        return test_suite
    
    
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
        print "Finding files...",
        files = self.find_import_files()
        print self.test_dir, '... done'
        print "Importing test modules ...",
        modules = self.find_modules_from_files(files)
        print "done."
        all_tests = self.find_tests_from_modules(modules)
        if self.test_filter is not None:
            print 'Test Filter: %s' % [p.pattern for p in self.test_filter]
            all_tests = self.filter_tests(all_tests)
        print
        runner = unittest.TextTestRunner(stream=sys.stdout, descriptions=1, verbosity=verbosity)
        runner.run(unittest.TestSuite(all_tests))
        return

#=======================================================================================================================
# main        
#=======================================================================================================================
if __name__ == '__main__':
    dirs, verbosity, test_filter = parse_cmdline()
    PydevTestRunner(dirs, test_filter, verbosity).run_tests()
