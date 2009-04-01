import os
import sys

args = sys.argv[1:]
this_script_path = sys.argv[0]
this_script_dir = os.path.split(this_script_path)[0]

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
    
    os.chdir(os.path.join(this_script_dir, d1))
    import build_org #@UnresolvedImport
    build_org.LAST_VERSION_TAG = LAST_VERSION_TAG
    os.chdir('..')
    build_org.DoIt()
    
    os.chdir('..')
    os.chdir(os.path.join(this_script_dir, d2))
    import build_com #@UnresolvedImport
    build_com.LAST_VERSION_TAG = LAST_VERSION_TAG
    build_com.DoIt()
    
    sys.stdout.write('finished both\n')