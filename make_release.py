'''
The options available are:

--no-revert: The svn is not reverted to its initial state

--make-pro: To build the pro version

--make-open To build the open source version

E.g.:

To build both without reverting the svn:
d:\bin\python261\python.exe make_release.py --make-open --make-pro --no-revert

To build the docs:
d:\bin\python261\python.exe --up-docs --version=1.4.5

'''

import subprocess
import remove_unversioned_files
import sys
import os

BASE_DIR = r'W:\temp_buildDir'
PYDEV_PRO_DIR = BASE_DIR+r'\pydev_pro'
PYDEV_OPEN_DIR = BASE_DIR+r'\pydev'

global VERSION
VERSION = None

#=======================================================================================================================
# Execute
#=======================================================================================================================
def Execute(cmds, **kwargs):
    '''
    Helper to execute commands.
    '''
    print 'Executing', ' '.join(cmds), ':', subprocess.call(cmds, **kwargs)

#=======================================================================================================================
# ExecutePython
#=======================================================================================================================
def ExecutePython(cmds, **kwargs):
    '''
    Helper to execute a python script
    '''
    Execute([sys.executable] + cmds, **kwargs)


#=======================================================================================================================
# UpdateDocs
#=======================================================================================================================
def UpdateDocs():
    '''
    Will just build the docs
    '''
    ExecutePython([PYDEV_PRO_DIR+'/plugins/com.python.pydev.docs/build_both.py', '--version='+str(VERSION)])
    

MAKE_OPEN = 1
MAKE_PRO = 2

#=======================================================================================================================
# Make
#=======================================================================================================================
def Make(make, revert_and_update_svn=False):
    '''
    Actually builds the open source or the pro plugin.
    
    @param make: which one to build (according to constants)
    @param revert_and_update_svn: should we revert the svn before the build?
    '''
    initial_dir = os.getcwd()
    try:
        os.chdir(PYDEV_PRO_DIR)
        
        if make == MAKE_OPEN:
            d  = PYDEV_OPEN_DIR
            os.chdir(d+r'\builders\org.python.pydev.build')
            build_dir = 'w:/temp_buildDir/pydev'
            deploy_dir = 'w:/temp_deployDir/pydev'
            
        elif make == MAKE_PRO:
            d = PYDEV_PRO_DIR
            os.chdir(d+r'\builders\com.python.pydev.build')
            build_dir = 'w:/temp_buildDir/pydev_pro'
            deploy_dir = 'w:/temp_deployDir/pydev_pro'
            
        else:
            raise AssertionError('Wrong target!')  
    
        if revert_and_update_svn:
            Execute(['svn', 'revert', '-R', d])
            remove_unversioned_files.RemoveFilesFrom(d)
            Execute(['svn', 'up', '--non-interactive', '--force', d])

        env = {}
        env.update(os.environ)
        
        env['JAVA_HOME'] = r'D:\bin\jdk_1_5_09'
        
        cmds = [
             r'W:\eclipse_341_clean\plugins\org.apache.ant_1.7.0.v200803061910\bin\ant.bat',
             '-DbuildDirectory=%s' % (build_dir,),
             '-Dbaseos=win32',
             '-Dbasews=win32',
             '-Dbasearch=x86',
             '-Ddeploy.dir=%s' % (deploy_dir,),
             '-DcleanAfter=false',
             '-Dvanilla.eclipse=W:/eclipse_341_clean',
             '-Dpydev.p2.repo=file:W:/temp_deployDir/pydev', #Only really used when building the pro version
        ]
        Execute(cmds, env=env, shell=True)
    finally:
        os.chdir(initial_dir)

        


#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    args = sys.argv[1:]
    
    for arg in args:
        if arg.startswith('--version='):
            version = arg[len('--version='):]
            VERSION = version
    
    revert_and_update_svn = True
    
    if '--no-revert' in args:
        revert_and_update_svn = False
    
    if '--make-open' in args:
        Make(MAKE_OPEN, revert_and_update_svn)
        
    if '--make-pro' in args:
        Make(MAKE_PRO, revert_and_update_svn)
        
    if '--up-docs' in args:
        UpdateDocs()
        
    