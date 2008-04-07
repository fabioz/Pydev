'''
    This module will set the default encoding for python so that it'll print things correctly to the console.
    (and then will execute the user site customize -- if available)
'''

DEBUG = 0 #0 or 1 because of jython
import os

import sys
encoding = None




#----------------------------------------------------------------------------------------------------------------------- 
#check if the encoding has been specified for this launch...    
config_file_str = os.path.dirname(__file__)
config_file_str = os.path.join(config_file_str, 'encoding_config')

#set the encoding with the encoding_config file that should've been created
#before launching the last application (it'll be removed after we get its contents)
if os.path.exists(config_file_str):
    try:
        config_file = open(config_file_str, 'r')
    except:
        if DEBUG:
            import traceback;traceback.print_exc() #@Reimport
        
    else:
        #ok, file opened
        try:
            try:
                new_encoding = config_file.read()
                if new_encoding.strip():
                    encoding = new_encoding.strip()
                    if DEBUG:
                        print 'encoding read: ', encoding
            except:
                #ok, just ignore it if we couldn't get it
                if DEBUG:
                    import traceback;traceback.print_exc() #@Reimport
                
        finally:
            try:
                config_file.close()
            except:
                if DEBUG:
                    import traceback;traceback.print_exc() #@Reimport
    
    try:
        #remove the file (it's valid only for a single launch)
        os.remove(config_file_str)
    except:
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
    

#----------------------------------------------------------------------------------------------------------------------- 
#if unable to get the encoding, the 'default' encoding is UTF-8
if not encoding:
    encoding = "UTF-8"



#----------------------------------------------------------------------------------------------------------------------- 
#and finally, set the encoding
try:
    if encoding:
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
        
