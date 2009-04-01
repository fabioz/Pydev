import os
import sys




for arg in args:
    if arg.startswith('--version='):
        version = arg[len('--version='):]
        LAST_VERSION_TAG = version
else:
    LAST_VERSION_TAG = '1.4.5' #Not specified (let's leave one there)


if __name__ == '__main__':
    d1 = 'open_source/scripts/'
    d2 = 'new_homepage/scripts/'
    
    sys.path.insert(0, './scripts')
    sys.path.insert(0, '.')
    
    os.chdir('open_source/scripts')
    import build_org #@UnresolvedImport
    build_org.LAST_VERSION_TAG = LAST_VERSION_TAG
    os.chdir('..')
    build_org.DoIt()
    
    os.chdir('..')
    os.chdir('new_homepage/scripts/')
    import build_com #@UnresolvedImport
    build_com.LAST_VERSION_TAG = LAST_VERSION_TAG
    build_com.DoIt()
    
    sys.stdout.write('finished both\n')