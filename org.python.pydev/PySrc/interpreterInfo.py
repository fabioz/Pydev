'''
This module was created to get information available in the interpreter, such as libraries,
paths, etc.

TODO: get the builtin modules embeeded in python from sys.builtin_module_names... 
sys.builtin_module_names: contains the builtin modules embeeded in python (rigth now, we specify all manually).

sys.prefix: A string giving the site-specific directory prefix where the platform independent Python files are installed

'''
import sys
import os

if __name__ == '__main__':
    executable = sys.executable
    print 'EXECUTABLE:%s|' % executable
    
    #this is the new implementation to get the system folders 
    #(still need to check if it works in linux)
    #(previously, we were getting the executable dir, but that is not always correct...)
    prefix = sys.prefix.lower()
    
    for p in sys.path:
        if p.lower().startswith(prefix):
            print '|', p
    