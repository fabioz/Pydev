import subprocess
import remove_unversioned_files
import sys
import os


#=======================================================================================================================
# Execute
#=======================================================================================================================
def Execute(cmds, env=None):
    print 'Executing', ' '.join(cmds), ':', subprocess.call(cmds, env=env)


#=======================================================================================================================
# Make
#=======================================================================================================================
def Make():
    base_dir = r'W:\temp_buildDir'
    pydev_pro_dir = base_dir+r'\pydev_pro'
    pydev_dir = base_dir+r'\pydev'
    
    os.chdir(pydev_pro_dir)
    
    for d n (pydev_dir, pydev_pro_dir):
    
        Execute(['svn', 'revert', '-R', d])
        remove_unversioned_files.RemoveFilesFrom(d)
        Execute(['svn', 'up', '--non-interactive', '--force', d])

        env = {}
        env['PATH'] = r'W:\eclipse_341_clean\plugins\org.apache.ant_1.7.0.v200803061910\bin'
        env['JAVA_HOME'] = r'D:\bin\jdk_1_5_09'
        
        Execute(['svn', 'up', d], env=env)
        
        cd pydev\builders\org.python.pydev.build
        ant -DbuildDirectory=W:/temp_buildDir/pydev -Dbaseos=win32 -Dbasews=win32 -Dbasearch=x86 -Ddeploy.dir=w:/temp_deployDir/pydev -DcleanAfter=false -Dvanilla.eclipse=W:/eclipse_341_clean


#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    Make()