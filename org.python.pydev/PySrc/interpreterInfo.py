'''
This module was created to get information available in the interpreter, such as libraries,
paths, etc.

TODO: get the builtin modules embeeded in python from sys.builtin_module_names... 
(for putting in org.python.pydev.ui.pythonpathconf.InterpreterInfo.forcedLibs)


what is what:
sys.builtin_module_names: contains the builtin modules embeeded in python (rigth now, we specify all manually).
sys.prefix: A string giving the site-specific directory prefix where the platform independent Python files are installed

'''
import sys
import os

if __name__ == '__main__':
    executable = sys.executable
    print 'EXECUTABLE:%s|' % executable
    
    def formatPath(p):
        '''fixes the path so that the format of the path really reflects the directories in the system
        '''
        return os.path.normcase(os.path.normpath(p))

    #this is the new implementation to get the system folders 
    #(still need to check if it works in linux)
    #(previously, we were getting the executable dir, but that is not always correct...)
    prefix = formatPath(sys.prefix)
    

    result = []
    for p in sys.path:
        p = formatPath(p)
        if p.startswith(prefix):
            if p not in result: #a path should not appear more than once...
                result.append(p)
            
    for p in result:
        print '|', p