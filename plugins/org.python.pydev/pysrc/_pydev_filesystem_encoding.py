def getfilesystemencoding():
    '''
    Note: there's a copy of this method in interpreterInfo.py
    '''
    import sys
    try:
        ret = sys.getfilesystemencoding()
        if not ret:
            raise RuntimeError('Unable to get encoding.')
        return ret
    except:
        try:
            #Handle Jython
            from java.lang import System
            env = System.getProperty("os.name").lower()
            if env.find('win') != -1:
                return 'ISO-8859-1' #mbcs does not work on Jython, so, use a (hopefully) suitable replacement
            return 'utf-8'
        except:
            pass
        
        #Only available from 2.3 onwards.
        if sys.platform == 'win32':
            return 'mbcs'
        return 'utf-8'