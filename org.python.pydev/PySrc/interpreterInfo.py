'''
This module was created to get information available in the interpreter, such as libraries,
paths, etc.

what is what:
sys.builtin_module_names: contains the builtin modules embeeded in python (rigth now, we specify all manually).
sys.prefix: A string giving the site-specific directory prefix where the platform independent Python files are installed

format is something as 
EXECUTABLE:python.exe|libs@compiled_dlls$builtin_mods

all internal are separated by |
'''
import sys
import os
True, False = 1,0 

if sys.platform == "cygwin":
    sys.path.append(os.path.join(sys.path[0],'ThirdParty'))
    import ctypes
    def nativePath(path):
        MAX_PATH=512  # On cygwin NT, its 260 lately, but just need BIG ENOUGH buffer
        '''Get the native form of the path, like c:\\Foo for /cygdrive/c/Foo'''

        retval = ctypes.create_string_buffer(MAX_PATH)
        path = fullyNormalizePath(path)
        ctypes.cdll.cygwin1.cygwin_conv_to_win32_path(path, retval)
        return retval.value
else:
    def nativePath(path):
        return fullyNormalizePath(path)
    
def fullyNormalizePath(path):
    '''fixes the path so that the format of the path really reflects the directories in the system
    '''
    return os.path.normcase(os.path.normpath(path))


if __name__ == '__main__':
    try:
        #just give some time to get the reading threads attached (just in case)
        sleep(0.5)
    except:
        pass
    
    try:
        executable = nativePath(sys.executable)
    except:
        executable = sys.executable
        
    s = 'Version%s.%s' % (sys.version_info[0], sys.version_info[1])
    print s
            
    print 'EXECUTABLE:%s|' % executable
    
    #this is the new implementation to get the system folders 
    #(still need to check if it works in linux)
    #(previously, we were getting the executable dir, but that is not always correct...)
    prefix = nativePath(sys.prefix)
    print 'prefix is', prefix
    

    result = []
    for p in sys.path:
        p = nativePath(p)
        if p.startswith(prefix):
            result.append((p,True))
        else:
            result.append((p, False))
            
    for p,b in result:
        if b:
            print '|%s%s'% (p,'INS_PATH')
        else:
            print '|%s%s'% (p,'OUT_PATH')
    
    print '@' #no compiled libs
    print '$' #the forced libs
    
    for builtinMod in sys.builtin_module_names:
        print '|', builtinMod
        
    
    try:
        sys.stdout.flush()
        sys.stderr.flush()
        #and give some time to let it read things (just in case)
        sleep(0.5)
    except:
        pass
    
    raise RuntimeError('Ok, this is so that it shows the output (ugly hack for some platforms, so that it releases the output).')