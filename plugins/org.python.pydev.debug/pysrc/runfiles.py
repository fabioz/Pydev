
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
    import pydev_runfiles_xml_rpc
    
    configuration = pydev_runfiles.parse_cmdline([sys.argv[0]] + pydev_params)
    pydev_runfiles_xml_rpc.InitializeServer(configuration.port)

    try:
        if found_nose_param:
            import nose
        else:
            raise ImportError()
    except ImportError:
        if found_nose_param:
            sys.stderr.write('Warning: Could not import the nose test runner. Running with the default pydev unittest runner.\n')
            
        pydev_runfiles.main(configuration)
        
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
        
        #Nose usage: http://somethingaboutorange.com/mrl/projects/nose/0.11.2/usage.html
#        show_stdout_option = ['-s']
        show_stdout_option = []
#        processes_option = ['--processes=2']
        processes_option = []
            
        if configuration.tests:
            new_files_or_dirs = []
            for f in configuration.files_or_dirs:
                for t in configuration.tests:
                    new_files_or_dirs.append(f+':'+t)
            files_or_dirs = new_files_or_dirs
        argv = ['--verbosity='+str(configuration.verbosity)] + processes_option + show_stdout_option + nose_params + files_or_dirs
        
        argv.insert(0, sys.argv[0])
        
        if DEBUG:
            print 'Nose args:', argv[1:]

        from pydev_runfiles_nose import PYDEV_NOSE_PLUGIN_SINGLETON
        argv.append('--with-pydevplugin')
        nose.run(argv=argv, addplugins=[PYDEV_NOSE_PLUGIN_SINGLETON])
