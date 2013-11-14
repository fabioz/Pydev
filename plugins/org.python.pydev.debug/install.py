'''
This module should be called to regenerate cog code (it doesn't need to be rerun every time, as the
generated code itself is commited).
'''

from string import Template
import os
import sys
parent_dir = os.path.split(__file__)[0]

#=======================================================================================================================
# RunCog
#=======================================================================================================================
def RunCog():
    #Add cog to the pythonpath
    cog_dir = parent_dir[:parent_dir.index('plugins')]
    cog_src_dir = os.path.join(cog_dir, 'builders', 'org.python.pydev.build', 'cog_src')
    assert os.path.exists(cog_src_dir), '%s does not exist' % (cog_src_dir,)
    sys.path.append(cog_src_dir)
    
    import cog
    cog.RunCogInFiles([os.path.join(parent_dir, 'src_console', 'org', 'python', 'pydev', 'debug', 'newconsole', 'prefs', 'ColorManager.java')])
    cog.RunCogInFiles([os.path.join(parent_dir, 'plugin.xml')])


#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    RunCog()