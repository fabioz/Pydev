'''
    This module provides utilities to get the absolute filenames so that we can be sure that:
        - The case of a file will match the actual file in the filesystem (otherwise breakpoints won't be hit).
        - Providing means for the user to make path conversions when doing a remote debugging session in
          one machine and debugging in another.
    
    To do that, the PATHS_FROM_CLIENT_TO_SERVER constant must be filled with the appropriate paths.
    
    E.g.: 
        If the server has the structure
            /user/projects/my_project/src/package/module1.py  
        
        and the client has: 
            c:\my_project\src\package\module1.py  
            
        the PATHS_FROM_CLIENT_TO_SERVER would have to be:
            PATHS_FROM_CLIENT_TO_SERVER = [(r'c:\my_project\src', r'/user/projects/my_project/src')]
    
    @note: DEBUG_CLIENT_SERVER_TRANSLATION can be set to True to debug the result of those translations
    
    @note: the case of the paths is important! Note that this can be tricky to get right when one machine
    uses a case-independent filesystem and the other uses a case-dependent filesystem (if the system being
    debugged is case-independent, 'normcase()' should be used on the paths defined in PATHS_FROM_CLIENT_TO_SERVER). 
    
    @note: all the paths with breakpoints must be translated (otherwise they won't be found in the server)
    
    @note: to enable remote debugging in the target machine (pydev extensions in the eclipse installation)
        import pydevd;pydevd.settrace(host, stdoutToServer, stderrToServer, port, suspend)
        
        see parameter docs on pydevd.py
        
    @note: for doing a remote debugging session, all the pydevd_ files must be on the server accessible 
        through the PYTHONPATH (and the PATHS_FROM_CLIENT_TO_SERVER only needs to be set on the target 
        machine for the paths that'll actually have breakpoints).
'''




from pydevd_constants import * #@UnusedWildImport
import os.path
import sys
import traceback

normcase = os.path.normcase
basename = os.path.basename
exists = os.path.exists
join = os.path.join

try:
    rPath = os.path.realpath #@UndefinedVariable
except:
    # jython does not support os.path.realpath
    # realpath is a no-op on systems without islink support
    rPath = os.path.abspath 
  
#defined as a list of tuples where the 1st element of the tuple is the path in the client machine
#and the 2nd element is the path in the server machine.
#see module docstring for more details.
PATHS_FROM_CLIENT_TO_SERVER = []

#example:
#PATHS_FROM_CLIENT_TO_SERVER = [
#(normcase(r'd:\temp\temp_workspace_2\test_python\src\yyy\yyy'),
# normcase(r'd:\temp\temp_workspace_2\test_python\src\hhh\xxx'))]

DEBUG_CLIENT_SERVER_TRANSLATION = False

#caches filled as requested during the debug session
NORM_FILENAME_CONTAINER = {}
NORM_FILENAME_AND_BASE_CONTAINER = {}
NORM_FILENAME_TO_SERVER_CONTAINER = {}
NORM_FILENAME_TO_CLIENT_CONTAINER = {}


def _NormFile(filename):
    try:
        return NORM_FILENAME_CONTAINER[filename]
    except KeyError:
        r = normcase(rPath(filename))
        #cache it for fast access later
        NORM_FILENAME_CONTAINER[filename] = r
        return r
    
#Now, let's do a quick test to see if we're working with a version of python that has no problems
#related to the names generated...
try:
    if not exists(_NormFile(rPath.func_code.co_filename)):
        print >> sys.stderr, '-------------------------------------------------------------------------------'
        print >> sys.stderr, 'pydev debugger: CRITICAL WARNING: This version of python seems to be incorrectly compiled (internal generated filenames are not absolute)'
        print >> sys.stderr, 'pydev debugger: The debugger may still function, but it will work slower and may miss breakpoints.'
        print >> sys.stderr, 'pydev debugger: Related bug: http://bugs.python.org/issue1666807'
        print >> sys.stderr, '-------------------------------------------------------------------------------'
        
        initial_norm_file = _NormFile
        def _NormFile(filename): #Let's redefine _NormFile to work with paths that may be incorrect
            ret = initial_norm_file(filename)
            if not exists(ret):
                #We must actually go on and check if we can find it as if it was a relative path for some of the paths in the pythonpath
                for path in sys.path:
                    ret = join(path, filename)
                    if exists(ret):
                        break
                else:
                    print >> sys.stderr, 'pydev debugger: Unable to find real location for: %s' % filename
                    ret = filename
                
            return ret
except:
    #Don't fail if there's something not correct here -- but at least print it to the user so that we can correct that
    traceback.print_exc()


if PATHS_FROM_CLIENT_TO_SERVER:
    #only setup translation functions if absolutely needed! 
    def NormFileToServer(filename): 
        try:
            return NORM_FILENAME_TO_SERVER_CONTAINER[filename]
        except KeyError:
            #used to translate a path from the client to the debug server
            translated = normcase(filename)
            for client_prefix, server_prefix in PATHS_FROM_CLIENT_TO_SERVER:
                if translated.startswith(client_prefix):
                    if DEBUG_CLIENT_SERVER_TRANSLATION:
                        print >> sys.stderr, 'pydev debugger: replacing to server', translated
                    translated = translated.replace(client_prefix, server_prefix)
                    if DEBUG_CLIENT_SERVER_TRANSLATION:
                        print >> sys.stderr, 'pydev debugger: sent to server', translated
                    break
            else:
                if DEBUG_CLIENT_SERVER_TRANSLATION:
                    print >> sys.stderr, 'pydev debugger: unable to find matching prefix for: %s in %s' % \
                        (translated, [x[0] for x in PATHS_FROM_CLIENT_TO_SERVER])
                    
            ret = _NormFile(translated)
            NORM_FILENAME_TO_SERVER_CONTAINER[filename] = translated
            return ret
        
    
    def NormFileToClient(filename): 
        try:
            return NORM_FILENAME_TO_CLIENT_CONTAINER[filename]
        except KeyError:
            #used to translate a path from the debug server to the client
            translated = normcase(filename)
            for client_prefix, server_prefix in PATHS_FROM_CLIENT_TO_SERVER:
                if translated.startswith(server_prefix):
                    if DEBUG_CLIENT_SERVER_TRANSLATION:
                        print >> sys.stderr, 'pydev debugger: replacing to client', translated
                    translated = translated.replace(server_prefix, client_prefix)
                    if DEBUG_CLIENT_SERVER_TRANSLATION:
                        print >> sys.stderr, 'pydev debugger: sent to client', translated
                    break
            else:
                if DEBUG_CLIENT_SERVER_TRANSLATION:
                    print >> sys.stderr, 'pydev debugger: unable to find matching prefix for: %s in %s' % \
                        (translated, [x[1] for x in PATHS_FROM_CLIENT_TO_SERVER])
                        
            ret = _NormFile(translated)
            NORM_FILENAME_TO_CLIENT_CONTAINER[filename] = ret
            return ret
        
else:
    #no translation step needed (just inline the calls)
    NormFileToClient = _NormFile
    NormFileToServer = _NormFile
    

def GetFilenameAndBase(frame):
    #This one is just internal (so, does not need any kind of client-server translation)
    f = frame.f_code.co_filename
    try:
        return NORM_FILENAME_AND_BASE_CONTAINER[f]
    except KeyError:
        filename = _NormFile(f)
        base = basename(filename)
        NORM_FILENAME_AND_BASE_CONTAINER[f] = filename, base
        return filename, base
