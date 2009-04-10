'''
The options available are:

--no-revert: The svn is not reverted to its initial state

--make-pro: To build the pro version

--make-open To build the open source version

E.g.:

To build both without reverting the svn:
d:\bin\python261\python.exe make_release.py --make-open --make-pro --no-revert

To build the docs:
d:\bin\python261\python.exe --up-docs

To update the site:
d:\bin\python261\python.exe --up-site


'''

#We have to update the versions on each release!
VERSION = "1.4.5"
VERSION_WITH_SVN = "1.4.5.2725"

ALL_VERSIONS_TO_CHECK = [
    VERSION_WITH_SVN,
    "1.4.4.2636"
]


from zipfile import ZipFile
import shutil
import urllib

import subprocess
import remove_unversioned_files
import sys
import os

BASE_DIR = r'W:\temp_buildDir'
BASE_DEPLOY_DIR = 'w:/temp_deployDir'
BASE_LOCAL_UPDATE_SITE_DIR = 'w:/temp_deployDir/updateSite'

PYDEV_PRO_DIR = BASE_DIR + r'\pydev_pro'
PYDEV_OPEN_DIR = BASE_DIR + r'\pydev'

PYDEV_OPEN_DEPLOY_DIR = BASE_DEPLOY_DIR + '/pydev'
PYDEV_PRO_DEPLOY_DIR = BASE_DEPLOY_DIR + '/pydev_pro'

PYDEV_LOCAL_UPDATE_SITE = BASE_LOCAL_UPDATE_SITE_DIR + '/pydev'
PYDEV_PRO_LOCAL_UPDATE_SITE = BASE_LOCAL_UPDATE_SITE_DIR + '/pydevPro'

PYDEV_ZIPS = BASE_LOCAL_UPDATE_SITE_DIR + '/zipsPydev'
PYDEV_PRO_ZIPS = BASE_LOCAL_UPDATE_SITE_DIR + '/zipsPydevPro'


PYDEV_REMOTE_UPDATE_SITE = 'http://pydev.sourceforge.net/updates/'
PYDEV_PRO_REMOTE_UPDATE_SITE = 'http://www.fabioz.com/pydev/updates/'



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
    ExecutePython([PYDEV_PRO_DIR + '/plugins/com.python.pydev.docs/build_both.py', '--version=' + VERSION])
    
    
    
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
    return TEMPLATE_FOR_PRO % {'version_with_svn':VERSION_WITH_SVN}


#=======================================================================================================================
# GetOpenSiteContents
#=======================================================================================================================
def GetOpenSiteContents():
    TEMPLATE_FOR_PRO = '''<feature url="features/org.python.pydev.feature_%(version_with_svn)s.jar" id="org.python.pydev.feature" version="%(version_with_svn)s">
      <category name="PyDev"/>
   </feature>

   '''
    return TEMPLATE_FOR_PRO % {'version_with_svn':VERSION_WITH_SVN}


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
# DownloadTo
#=======================================================================================================================
def DownloadTo(url, filename):
    print 'Getting...', url
    contents = ReadFromURL(url)
    print 'Writing'
    WriteToFile(filename, contents)
    return contents

#=======================================================================================================================
# UpdateSiteCreateP2
#=======================================================================================================================
def UpdateSiteCreateP2():
    '''
    Ok, this will update the update site. For that, it'll download the contents from both update sites,
    add them to
    deployDir/updateSite/pydev
    deployDir/updateSite/pydev_pro
    
    and will update the correspondent site.xml, artifacts.jar and content.jar
    '''
    open_parameters = (PYDEV_LOCAL_UPDATE_SITE, PYDEV_REMOTE_UPDATE_SITE, GetOpenSiteContents(), PYDEV_OPEN_DEPLOY_DIR)
    pro_parameters = (PYDEV_PRO_LOCAL_UPDATE_SITE, PYDEV_PRO_REMOTE_UPDATE_SITE, GetProSiteContents(), PYDEV_PRO_DEPLOY_DIR)
    
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
            filename = update_site + '/' + f
            file_to_contents[f] = DownloadTo(remote + f, filename)
            
                
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
                for v in ALL_VERSIONS_TO_CHECK:
                    assert v in contents
                
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
            
    
    
#=======================================================================================================================
# UpdateDescriptIon
#=======================================================================================================================
def UpdateDescriptIon():
    target = PYDEV_PRO_ZIPS + '/descript.ion'
    contents = DownloadTo("http://fabioz.com/pydev/zips/descript.ion", target).strip()
    
    #Try to keep delimiter...
    if '\r\n' in contents:
        delim = '\r\n'
        
    elif '\r' in contents:
        delim = '\r'
        
    elif '\n' in contents:
        delim = '\n'
    
    else:
        delim = '\r\n' #default
        
    NEW_CONTENTS = delim + "com.python.pydev.extensions-%(version_with_svn)s.zip    Pydev Extensions %(version_with_svn)s" + delim + \
    "org.python.pydev.feature-%(version_with_svn)s.zip    Pydev 'Open Source' %(version_with_svn)s" + delim

    NEW_CONTENTS = NEW_CONTENTS % {'version_with_svn':VERSION_WITH_SVN}

    contents += NEW_CONTENTS
    print 'Updating descript.ion'
    open(target, 'wb').write(contents)
    
    
#=======================================================================================================================
# UpdateSiteCopyZips
#=======================================================================================================================
def UpdateSiteCopyZips():
    create_dirs = [PYDEV_ZIPS, PYDEV_PRO_ZIPS]

    for dir in create_dirs:
        try:
            print 'Making', dir
            os.makedirs(dir)
        except:
            pass
    
    #Copy the sources zip to the pydev zips
    for f in os.listdir(PYDEV_OPEN_DEPLOY_DIR):
        if f.endswith('-sources.zip'):
            print 'Copying', f
            shutil.copyfile(PYDEV_OPEN_DEPLOY_DIR + '/' + f, PYDEV_ZIPS + '/' + f)
    
    #Copy things to the update site
    def CopyDirContents(src, dst):
        try:
            os.makedirs(dst)
        except:
            pass
        for f in os.listdir(src):
            shutil.copy(src + '/' + f, dst + '/' + f)
    
    print 'Copying features and plugins to open source update site'
    CopyDirContents(PYDEV_OPEN_DEPLOY_DIR + '/features', PYDEV_LOCAL_UPDATE_SITE + '/features')
    CopyDirContents(PYDEV_OPEN_DEPLOY_DIR + '/plugins', PYDEV_LOCAL_UPDATE_SITE + '/plugins')
    
    print 'Copying features and plugins to pydev extensions update site'
    CopyDirContents(PYDEV_OPEN_DEPLOY_DIR + '/features', PYDEV_PRO_LOCAL_UPDATE_SITE + '/features')
    CopyDirContents(PYDEV_OPEN_DEPLOY_DIR + '/plugins', PYDEV_PRO_LOCAL_UPDATE_SITE + '/plugins')
    CopyDirContents(PYDEV_PRO_DEPLOY_DIR + '/features', PYDEV_PRO_LOCAL_UPDATE_SITE + '/features')
    CopyDirContents(PYDEV_PRO_DEPLOY_DIR + '/plugins', PYDEV_PRO_LOCAL_UPDATE_SITE + '/plugins')
    
    
    #Pass all the .jars in the features and plugins, uncompress all but the templates jar and recompress them 
    #(that's needed because that's what's expected to just extract in the dropins)
    dirs = (
        PYDEV_OPEN_DEPLOY_DIR + '/features',
        PYDEV_OPEN_DEPLOY_DIR + '/plugins',
        PYDEV_PRO_DEPLOY_DIR + '/features',
        PYDEV_PRO_DEPLOY_DIR + '/plugins',
    )
    
    
    file_to_zip_contents = {}
    for d in dirs:
        
        is_feature = d.endswith('features')
        
        for f in os.listdir(d):
            
            if f.endswith('.jar'):
                filename = d + '/' + f
                if f.startswith('org.python.pydev.templates'):
                    file_to_zip_contents[f] = open(filename, 'rb').read(), is_feature
                else:
                    features = Unzip(filename, only_to_memory=True)
                    file_to_zip_contents[f] = features, is_feature
    
    zip_file_open = ZipFile(PYDEV_ZIPS + '/org.python.pydev.feature-%s.zip' % VERSION_WITH_SVN, mode='w', compression=zipfile.ZIP_DEFLATED)            
    zip_file_open_in_pro = ZipFile(PYDEV_PRO_ZIPS + '/org.python.pydev.feature-%s.zip' % VERSION_WITH_SVN, mode='w', compression=zipfile.ZIP_DEFLATED)            
    zip_file_pro = ZipFile(PYDEV_PRO_ZIPS + '/com.python.pydev.extensions-%s.zip' % VERSION_WITH_SVN, mode='w', compression=zipfile.ZIP_DEFLATED)            
    
    for filename, path_to_contents in file_to_zip_contents.iteritems():
        path_to_contents, is_feature = path_to_contents
        
        print 'Writing to zip:', filename
        def Write(arcname, contents):
            if arcname.startswith('com.'):
                if is_feature:
                    arcname = 'features/' + arcname
                else:
                    arcname = 'plugins/' + arcname
                zip_file_pro.writestr(arcname, contents)
            else:
                if is_feature:
                    arcname = 'features/' + arcname
                else:
                    arcname = 'plugins/' + arcname
                zip_file_open.writestr(arcname, contents)
                zip_file_open_in_pro.writestr(arcname, contents)
                
        if isinstance(path_to_contents, str):
            #Handle the ones we should not zip. i.e. templates
            Write(filename, path_to_contents)
        else:
            for path, contents in path_to_contents.iteritems():
                Write(filename[:-4] + '/' + path, contents)
                
            
    zip_file_open.close()
    zip_file_pro.close()
    
    
        
#=======================================================================================================================
# UploadToFtp
#=======================================================================================================================
def UploadToFtp():
    '''
    Depends on paramiko: http://pypi.python.org/pypi/paramiko/1.7.2
    and PyCrypto: http://www.voidspace.org.uk/python/modules.shtml#pycrypto
    
    Examples from http://commandline.org.uk/python/sftp-python/
    '''
    import paramiko
    paramiko.util.log_to_file('/tmp/paramiko.log')
    host = "example.com"
    port = 22
    transport = paramiko.Transport((host, port))
    password = "example101"
    username = "warrior"
    transport.connect(username=username, password=password)
    sftp = paramiko.SFTPClient.from_transport(transport)
    try:
        filepath = '/home/zeth/lenna.jpg'
        localpath = '/home/zeth/lenna.jpg'
        sftp.get(filepath, localpath)
        filepath = '/home/zeth/lenna.jpg'
        localpath = '/home/zeth/lenna.jpg'
        sftp.put(filepath, localpath)
    finally:
        sftp.close()
        transport.close()



#=======================================================================================================================
# UpdateSite
#=======================================================================================================================
def UpdateSite():
    UpdateSiteCreateP2()
    UpdateSiteCopyZips()
    UpdateDescriptIon() #the directory structure must already be created for this function.
            

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
        
    
