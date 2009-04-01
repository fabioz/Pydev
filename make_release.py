import subprocess
import remove_unversioned_files
import sys
import os


#=======================================================================================================================
# Execute
#=======================================================================================================================
def Execute(cmds):
    print 'Executing', ' '.join(cmds), ':', subprocess.call(cmds)


#=======================================================================================================================
# Make
#=======================================================================================================================
def Make():
    base_dir = r'W:\temp_buildDir'
    pydev_pro_dir = base_dir+r'\pydev_pro'
    pydev_dir = base_dir+r'\pydev'
    
    os.chdir(pydev_pro_dir)
    
    Execute(['svn', 'revert', '-R', pydev_dir])
    Execute(['svn', 'revert', '-R', pydev_pro_dir])
    
    remove_unversioned_files.RemoveFilesFrom(pydev_dir)
    remove_unversioned_files.RemoveFilesFrom(pydev_pro_dir)
    
    Execute(['svn', 'up', pydev_dir])
    Execute(['svn', 'up', pydev_pro_dir])

#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    Make()