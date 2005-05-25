'''
This module was created to get information available in the interpreter, such as libraries,
paths, etc.
'''
import sys
import os

if __name__ == '__main__':
    executable = sys.executable
    print 'EXECUTABLE:%s|' % executable
    executableFolder = os.path.dirname(executable)
    executableFolderLower = executableFolder.lower()
    
    for p in sys.path:
        if p.lower().startswith(executableFolderLower):
            print '|', p
    