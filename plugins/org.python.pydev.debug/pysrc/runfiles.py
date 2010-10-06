
if __name__ == '__main__':
    
    import sys
    
    #Separate the nose params and the pydev params.
    pydev_params = []
    nose_params = []
    found_nose_param = False
    for arg in sys.argv[1:]:
        if arg != '--nose-params':
            pydev_params.append(arg)
            
        else:
            if not found_nose_param:
                found_nose_param = True
            else:
                nose_params.append(arg)
                
    
    #Here we'll run either with nose or with the pydev_runfiles.
    import pydev_runfiles
    try:
        import nose
    except ImportError:
        sys.stderr.write('Warning: Could not import the nose test runner. Running with the default pydev unittest runner.\n')
        sys.argv = [sys.argv[0]] + pydev_params
        pydev_runfiles.main()
        
    else:
        #We'll convert the parameters to what nose expects.
        #The received parameters are: 
        #runfiles.py  -v|--verbosity <level>  -f|--filter <regex>  -t|--tests <Test.test1,Test2>  dirs|files --nose-params xxx yyy zzz
        #(all after --nose-params should be passed directly to nose)
        
        #In java: 
        #--tests = Constants.ATTR_UNITTEST_TESTS
        #--verbosity = PydevPrefs.getPreferences().getString(PyunitPrefsPage.PYUNIT_VERBOSITY)
        #--filter = PydevPrefs.getPreferences().getString(PyunitPrefsPage.PYUNIT_TEST_FILTER)
        
        DEBUG = True
        files_or_dirs, verbosity, test_filter, tests = pydev_runfiles.parse_cmdline()
        
        #Nose usage: http://somethingaboutorange.com/mrl/projects/nose/0.11.2/usage.html
        
        #Should pass a --id-file that pydev controls! (so .nosetests is not created in the working dir)
        rerun_failures = False
        if not rerun_failures:
            show_stdout_option = ['-s']
#            show_stdout_option = []
            processes_option = ['--processes=2']
#            processes_option = []
            
            if tests:
                new_files_or_dirs = []
                for f in files_or_dirs:
                    for t in tests:
                        new_files_or_dirs.append(f+':'+t)
                files_or_dirs = new_files_or_dirs
            argv = ['--verbosity='+str(verbosity)] + processes_option + show_stdout_option + nose_params + files_or_dirs
        else:
            argv = ['--with-id', '--failed'] + files_or_dirs
        
        argv.insert(0, sys.argv[0])
        
        if DEBUG:
            print 'Nose args:', sys.argv[1:]

        from nose.plugins.base import Plugin
        class PydevPlugin(Plugin):
            
            def startTest(self, test):
                print "Pydev: started %s" % test
                
        
            def addTestCase(self, kind, test, err=None):
                print "Pydev: add test case %s" % test
        
            def addError(self, test, err):
                print "Pydev: add error %s" % test
        
            def addFailure(self, test, err):
                print "Pydev: add failure %s" % test
        
            def addSuccess(self, test):
                print "Pydev: add success %s" % test
                
        argv.append('--with-pydevplugin')
        nose.run(argv=argv, addplugins=[PydevPlugin()])
