import os.path
import sys

if __name__ == '__main__':
    d1 = 'open_source/scripts/'
    d2 = 'new_homepage/scripts/'
    
    sys.path.insert(0, './scripts')
    sys.path.insert(0, '.')
    
    os.chdir('open_source/scripts')
    import build_org
    os.chdir('..')
    build_org.DoIt()
    
    os.chdir('..')
    os.chdir('new_homepage/scripts/')
    import build_com
    build_com.DoIt()
    
    print 'finished both'