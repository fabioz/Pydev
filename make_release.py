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

To update the site:
d:\bin\python261\python.exe --up-site --version=1.4.5


'''
from zipfile import ZipFile
import shutil
import urllib

import subprocess
import remove_unversioned_files
import sys
import os

BASE_DIR = r'W:\temp_buildDir'
PYDEV_PRO_DIR = BASE_DIR + r'\pydev_pro'
PYDEV_OPEN_DIR = BASE_DIR + r'\pydev'
BASE_DEPLOY_DIR = 'w:/temp_deployDir'
PYDEV_OPEN_DEPLOY_DIR = BASE_DEPLOY_DIR + '/pydev'
PYDEV_PRO_DEPLOY_DIR = BASE_DEPLOY_DIR + '/pydev_pro'





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
    Will just build the docs (in place)
    '''
    ExecutePython([PYDEV_PRO_DIR + '/plugins/com.python.pydev.docs/build_both.py', '--version=' + str(VERSION)])
    
    
    
import sys, zipfile, os, os.path

#=======================================================================================================================
# Unzip
#=======================================================================================================================
def Unzip(file, dir=None, only_to_memory=False):
    if only_to_memory:
        assert dir is None
    if dir is not None:
        assert not only_to_memory
        
    written = {}
    zip = zipfile.ZipFile(file)
    for name in zip.namelist():
        contents = zip.read(name)
        if not only_to_memory:
            outfile = open(os.path.join(dir, name), 'wb')
            outfile.write(contents)
            outfile.close()
        written[name] = contents
    return written




#=======================================================================================================================
# GetProSiteContents
#=======================================================================================================================
def GetProSiteContents():
    TEMPLATE_FOR_PRO = '''<feature url="features/org.python.pydev.feature_%(version_with_svn)s.jar" id="org.python.pydev.feature" version="%(version_with_svn)s">
      <category name="PyDev"/>
   </feature>
  
  
   <feature url="features/com.python.pydev.extensions_%(version_with_svn)s.jar" id="com.python.pydev.extensions" version="%(version_with_svn)s">
      <category name="PyDev Extensions"/>
   </feature>

   '''
    return TEMPLATE_FOR_PRO % {'version_with_svn':"1.4.5.2724"}


#=======================================================================================================================
# GetOpenSiteContents
#=======================================================================================================================
def GetOpenSiteContents():
    TEMPLATE_FOR_PRO = '''<feature url="features/org.python.pydev.feature_%(version_with_svn)s.jar" id="org.python.pydev.feature" version="%(version_with_svn)s">
      <category name="PyDev"/>
   </feature>

   '''
    return TEMPLATE_FOR_PRO % {'version_with_svn':"1.4.5.2724"}


#=======================================================================================================================
# ReadFromURL
#=======================================================================================================================
def ReadFromURL(path):
    site_stream = urllib.urlopen(path)
    try:
        site_contents = site_stream.read()
    finally:
        site_stream.close()
    return site_contents


#=======================================================================================================================
# WriteToFile
#=======================================================================================================================
def WriteToFile(filename, contents):
    stream = open(filename, 'wb')
    try:
        stream.write(contents)
    finally:
        stream.close()


#=======================================================================================================================
# BuildP2
#=======================================================================================================================
def BuildP2(deploy_dir, update_site):
    #Now, lets put the new site in the content.jar and artifacts.jar
    Execute(
        [
            '%s/pydev/eclipse/eclipse.exe' % BASE_DIR,
            '-application',
            'org.eclipse.equinox.p2.metadata.generator.EclipseGenerator',
            '-updateSite',
            deploy_dir,
            '-site',
            'file:' + update_site + '/site.xml',
            '-metadataRepository file:' + deploy_dir,
            '-metadataRepositoryName',
            '"Pydev Update Site"',
            '-artifactRepository',
            'file:' + update_site,
            '-artifactRepositoryName "Pydev Artifacts"',
            '-compress',
            '-append',
            '-reusePack200Files',
            '-noDefaultIUs',
            '-vmargs',
            '-Xmx256m',
         ])

#=======================================================================================================================
# UpdateSite
#=======================================================================================================================
def UpdateSite():
    '''
    Ok, this will update the update site. For that, it'll download the contents from both update sites,
    add them to
    deployDir/updateSite/pydev
    deployDir/updateSite/pydev_pro
    
    and will update the correspondent site.xml, artifacts.jar and content.jar
    '''
    pydev_update_site = BASE_DEPLOY_DIR + '/updateSite/pydev'
    pydev_pro_update_site = BASE_DEPLOY_DIR + '/updateSite/pydevPro'
    
    pydev_remote_site = 'http://pydev.sourceforge.net/updates/'
    pydev_pro_remote_site = 'http://www.fabioz.com/pydev/updates/'
    
    open_parameters = (pydev_update_site, pydev_remote_site, GetOpenSiteContents(), PYDEV_OPEN_DEPLOY_DIR)
    pro_parameters = (pydev_pro_update_site, pydev_pro_remote_site, GetProSiteContents(), PYDEV_PRO_DEPLOY_DIR)
    
    for update_site, remote, contents, deploy_dir in (
        open_parameters,
        pro_parameters,
        ):
        try:
            print 'Making', update_site
            os.makedirs(update_site)
        except:
            pass
        
        file_to_contents = {}
        for f in ('site.xml', 'artifacts.jar', 'content.jar'):
            print 'Getting...', remote + f
            contents = ReadFromURL(remote + f)
            file_to_contents[f] = contents
            filename = update_site + '/' + f
            print 'Writing'
            WriteToFile(filename, contents)
                
        IS_PRO = update_site == pro_parameters[0]
        #At this point we should have:
        #site.xml
        #artifacts.jar
        #content.jar
        
        #We need to: update the site.xml to add the new release
        #Merge the contents of artifacts.jar and content.jar with the ones created for the build
        
        #Add after the description
        for f, contents in file_to_contents.iteritems():
            filename = update_site + '/' + f
            if f.endswith('.jar'):
                
                if IS_PRO:
                    #When building the pro version, we have an additional step, because we've just added the site for the
                    #pro, but it should contain both, the pro and the open-source plugins, so, we have to copy those files 
                    #to both deploy dirs (not only its current one 
                    target = open_parameters[3] + '/' + f
                    print 'Writing', f, 'to', target
                    open(target, 'wb').write(contents)
                else:
                    target = deploy_dir + '/' + f
                    print 'Writing', f, 'to', target
                    open(target, 'wb').write(contents)
                
            if f == 'site.xml':
                contents = open(filename).read()
                contents = contents.replace("<feature url", "%s<feature url" % (contents,), 1)
                stream = open(filename, 'w')
                stream.write(contents)
                stream.close()
        
        if IS_PRO:
            #We have to build it 1st in the open-source, copy them to the pydev pro and then build it again!
            BuildP2(open_parameters[3], open_parameters[0])
            shutil.copyfile(open_parameters[3] + '/content.jar', deploy_dir + '/content.jar') 
            shutil.copyfile(open_parameters[3] + '/artifacts.jar', deploy_dir + '/artifacts.jar') 
            
        BuildP2(deploy_dir, update_site)
        
        
        files_to_check = (
            ('content.jar', "<unit id='org.python.pydev'", "<unit id='com.python.pydev'"),
            ('artifacts.jar', "id='org.python.pydev'", "id='com.python.pydev'"))
        
        for file_to_check, open_occurrences, pro_occurrences in files_to_check:
            sys.stdout.write('Checking contents of resulting ' + file_to_check + ' ... ')
            for contents in Unzip(deploy_dir + '/' + file_to_check, only_to_memory=True).itervalues():
                #Check if it has all the versions we deployed!
                assert '1.4.4' in contents
                assert '1.4.5' in contents
                
                found = contents.count(open_occurrences)
                expected = 2
                assert found == expected, "Expected %s occurrences of %s. Found: %s" % (expected, open_occurrences, found)
                
                found = contents.count(pro_occurrences)
                if IS_PRO:
                    expected = 2
                else:
                    expected = 0
                assert found == expected, "Expected %s occurrences of %s. Found: %s" % (expected, pro_occurrences, found)
                sys.stdout.write(' OK\n')
                
            print 'Copying to', update_site + '/' + file_to_check
            shutil.copyfile(deploy_dir + '/' + file_to_check, update_site + '/' + file_to_check)
            
            

MAKE_OPEN = 1
MAKE_PRO = 2

REVERT_SVN = 1
REVERT_SVN_NO = 2
REVERT_SVN_ONLY = 3

#=======================================================================================================================
# Make
#=======================================================================================================================
def Make(make, revert_and_update_svn=REVERT_SVN):
    '''
    Actually builds the open source or the pro plugin.
    
    @param make: which one to build (according to constants)
    @param revert_and_update_svn: should we revert the svn before the build?
    '''
    initial_dir = os.getcwd()
    try:
        os.chdir(PYDEV_PRO_DIR)
        
        if make == MAKE_OPEN:
            d = PYDEV_OPEN_DIR
            os.chdir(d + r'\builders\org.python.pydev.build')
            build_dir = 'w:/temp_buildDir/pydev'
            deploy_dir = PYDEV_OPEN_DEPLOY_DIR
            
        elif make == MAKE_PRO:
            d = PYDEV_PRO_DIR
            os.chdir(d + r'\builders\com.python.pydev.build')
            build_dir = 'w:/temp_buildDir/pydev_pro'
            deploy_dir = PYDEV_PRO_DEPLOY_DIR
            
        else:
            raise AssertionError('Wrong target!')  
    
        if revert_and_update_svn in (REVERT_SVN, REVERT_SVN_ONLY):
            Execute(['svn', 'revert', '-R', d])
            remove_unversioned_files.RemoveFilesFrom(d)
            Execute(['svn', 'up', '--non-interactive', '--force', d])
            remove_unversioned_files.RemoveFilesFrom(d)


        if revert_and_update_svn == REVERT_SVN_ONLY:
            return
            
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
             '-Dpydev.p2.repo=file:%s/pydev' % (BASE_DEPLOY_DIR,), #Only really used when building the pro version
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
    
    revert_and_update_svn = REVERT_SVN
    
    if '--no-revert' in args:
        revert_and_update_svn = REVERT_SVN_NO
    
    elif '--only-revert' in args:
        revert_and_update_svn = REVERT_SVN_ONLY
    
    if '--make-open' in args:
        Make(MAKE_OPEN, revert_and_update_svn)
        
    if '--make-pro' in args:
        Make(MAKE_PRO, revert_and_update_svn)
        
    if '--up-docs' in args:
        UpdateDocs()
        
    if '--up-site' in args:
        UpdateSite()
        
    
