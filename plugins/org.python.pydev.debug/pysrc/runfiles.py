
if __name__ == '__main__':
    import sys
    
    #Separate the nose params and the pydev params.
    pydev_params = []
    nose_params = []
    found_nose_param = False
    for arg in sys.argv[1:]:
        if not found_nose_param and arg != '--nose-params':
            pydev_params.append(arg)
            
        else:
            if not found_nose_param:
                found_nose_param = True
            else:
                nose_params.append(arg)
                
    
    #Here we'll run either with nose or with the pydev_runfiles.
    import pydev_runfiles
    import pydev_runfiles_xml_rpc
    import pydevd_constants
    
    DEBUG = 1
    if DEBUG:
        print 'Received parameters', sys.argv
        print 'Params for pydev', pydev_params
        if found_nose_param:
            print 'Params for nose', nose_params
        
    try:
        configuration = pydev_runfiles.parse_cmdline([sys.argv[0]] + pydev_params)
    except:
        sys.stderr.write('Command line received: %s\n' % (sys.argv,))
        raise
    pydev_runfiles_xml_rpc.InitializeServer(configuration.port) #Note that if the port is None, a Null server will be initialized.

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
        #The supported parameters are: 
        #runfiles.py  --config-file|-t|--tests <Test.test1,Test2>  dirs|files --nose-params xxx yyy zzz
        #(all after --nose-params should be passed directly to nose)
        
        #In java: 
        #--tests = Constants.ATTR_UNITTEST_TESTS
        #--config-file = Constants.ATTR_UNITTEST_CONFIGURATION_FILE
        
        #Nose usage: http://somethingaboutorange.com/mrl/projects/nose/0.11.2/usage.html
        #show_stdout_option = ['-s']
        #processes_option = ['--processes=2']
            
        #The only thing actually handled here are the tests that we want to run, which we'll 
        #handle and pass as nose expects.
        
        config_file_contents = configuration.config_file_contents
        if config_file_contents:
            config_file_contents = config_file_contents.strip()
            
        if config_file_contents:
            #Handling through the file contents (file where each line is a test)
            files_or_dirs = []
            files_to_tests = {}
            for line in config_file_contents.splitlines():
                file_and_test = line.split('|')
                if len(file_and_test) == 2:
                    file, test = file_and_test
                    if pydevd_constants.DictContains(files_to_tests, file):
                        files_to_tests[file].append(test)
                    else:
                        files_to_tests[file] = [test]  
                        
            for file, tests in files_to_tests.items():
                for test in tests:
                    files_or_dirs.append(file+':'+test)
        else:
            if configuration.tests:
                #Tests passed (works together with the files_or_dirs)
                files_or_dirs = []
                for f in configuration.files_or_dirs:
                    for t in configuration.tests:
                        files_or_dirs.append(f+':'+t)
            else:
                #Only files or dirs passed (let it do the test-loading based on those paths)
                files_or_dirs = configuration.files_or_dirs
                
        argv = nose_params + files_or_dirs
        
        argv.insert(0, sys.argv[0])
        
        if DEBUG:
            print 'Final nose args:', argv[1:]

        from pydev_runfiles_nose import PYDEV_NOSE_PLUGIN_SINGLETON
        argv.append('--with-pydevplugin')
        nose.run(argv=argv, addplugins=[PYDEV_NOSE_PLUGIN_SINGLETON])
