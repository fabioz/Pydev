'''
    This module will:
    - set the default encoding for python so that it'll print things correctly to the console.
    - change the input() and raw_input() commands to change \r\n or \r into \n
    - execute the user site customize -- if available
    - change raw_input() and input() to also remove any trailing \r
'''

DEBUG = 0 #0 or 1 because of jython
import os

import sys
encoding = None


#----------------------------------------------------------------------------------------------------------------------- 
#check if the encoding has been specified for this launch...    

#set the encoding with the encoding_config file that should've been created
#before launching the last application (it'll be removed after we get its contents)
try:
    new_encoding = os.environ.get('PYDEV_CONSOLE_ENCODING')
    if new_encoding and new_encoding.strip():
        encoding = new_encoding.strip()
        if DEBUG:
            print 'encoding from env (PYDEV_CONSOLE_ENCODING): ', encoding
except:
    #ok, just ignore it if we couldn't get it
    if DEBUG:
        import traceback;traceback.print_exc() #@Reimport
                


#----------------------------------------------------------------------------------------------------------------------- 
if not encoding:
    #Jython
    try:
        from java.lang import System
    except ImportError:
        pass
    else:
        #that's the way that the encoding is specified in WorkbenchEncoding.getWorkbenchDefaultEncoding
        encoding = System.getProperty("file.encoding", "")
        if DEBUG:
            print 'encoding from "file.encoding": ', encoding


#----------------------------------------------------------------------------------------------------------------------- 
if not encoding:
    #Python: get the default system locale (if possible)
    try:
        import locale
    except ImportError:
        if DEBUG:
            import traceback;traceback.print_exc() #@Reimport
    else:
        loc = locale.getdefaultlocale()
        if loc[1]:
            #ok, default locale is set (if the user didn't specify any encoding, the system default should be used)
            encoding = loc[1]
            if DEBUG:
                print 'encoding from "locale": ', encoding
    

#----------------------------------------------------------------------------------------------------------------------- 
#if unable to get the encoding, the 'default' encoding is UTF-8
if not encoding:
    encoding = "UTF-8"



#----------------------------------------------------------------------------------------------------------------------- 
#and finally, set the encoding
try:
    if encoding:
        if DEBUG:
            print 'Setting default encoding', encoding
        sys.setdefaultencoding(encoding) #@UndefinedVariable (it's deleted after the site.py is executed -- so, it's undefined for code-analysis)
except:
    #ignore if we cannot set it correctly
    if DEBUG:
        import traceback;traceback.print_exc() #@Reimport




#----------------------------------------------------------------------------------------------------------------------- 
#now that we've finished the needed pydev sitecustomize, let's run the default one (if available)


#remove the pydev site customize (and the pythonpath for it)
try:
    for c in sys.path[:]:
        if c.find('pydev_sitecustomize') != -1:
            sys.path.remove(c)
            
    del sys.modules['sitecustomize'] #this module
except:
    #print the error... should never happen (so, always show, and not only on debug)!
    import traceback;traceback.print_exc() #@Reimport
else:
    #and now execute the default sitecustomize
    try:
        import sitecustomize #@UnusedImport
    except ImportError:
        pass
        




try:
    #Redefine input and raw_input only after the original sitecustomize was executed
    #(because otherwise, the original raw_input and input would still not be defined)
    import __builtin__
    original_raw_input = __builtin__.raw_input
    original_input = __builtin__.input
    
    
    def raw_input(prompt=''):
        #the original raw_input would only remove a trailing \n, so, at
        #this point if we had a \r\n the \r would remain (which is valid for eclipse)
        #so, let's remove the remaining \r which python didn't expect.
        ret = original_raw_input(prompt)
            
        if ret.endswith('\r'):
            return ret[:-1]
            
        return ret
    raw_input.__doc__ = original_raw_input.__doc__
        
    def input(prompt=''):
        #input must also be rebinded for using the new raw_input defined
        return eval(raw_input(prompt))
    input.__doc__ = original_input.__doc__
    
    
    __builtin__.raw_input = raw_input
    __builtin__.input = input
except:
    #Don't report errors at this stage
    if DEBUG:
        import traceback;traceback.print_exc() #@Reimport

