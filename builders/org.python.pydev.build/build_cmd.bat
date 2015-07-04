@echo Execute:

@echo X:\pydev\builders\org.python.pydev.build\build_cmd.bat

@echo If needed to update version:
@echo x:
@echo cd x:\pydev
@echo python update_version.py 3.6.0

@echo Note: instructions for properly updating the variables are in the end of the file
@echo The contents here may just be copied into cmd.exe or some other shell (just note that
@echo in some cases a call to git may stop executing, so, you may need to copy the commands in chunks).

set BRANCH=development

set DRIVE=x:
set BASE_LOCAL_PYDEV_GIT=x:\pydev
set BUILD_DIR=X:\pydev_build\build_dir
set DEPLOY_DIR=X:\pydev_build\deploy_dir
set JAVA_HOME=C:\bin\jdk1.7.0_55
set MAVEN_BIN=C:\bin\maven-3.2.1\bin
set GIT_EXECUTABLE="C:\Program Files (x86)\Git\bin\git.exe"
set ECLIPSE_CLEAN=C:\bin\eclipse45final
set LAUNCHER_PLUGIN=org.eclipse.equinox.launcher_1.3.100.v20150511-1540.jar
set BUILDER_PLUGIN=org.eclipse.pde.build_3.9.100.v20150521-1524
set KEYSTORE=X:\release_tools\pydevkeystore
set KEYSTORE_ALIAS=pydev
set SIGN_KEYSTORE=X:\release_tools\pydevkeystore
set SIGN_ALIAS=pydev
SET MAVEN_OPTS=-Xmx1024m


set BASEOS=win32
set BASEWS=win32
set BASEARCH=x86

set PATH=
set PATH=C:\bin\Python27
set PATH=C:\bin\FastCopy211;%PATH%
set PATH=C:\Windows\system32;%PATH%
set PATH=%MAVEN_BIN%;%PATH%
set PATH=%JAVA_HOME%\bin;%PATH%
set PATH=C:\Program Files (x86)\Git\bin;%PATH%
set PATH=%ECLIPSE_CLEAN%\plugins\org.apache.ant_1.9.2.v201404171502\bin;%PATH%


@echo actual build command
mkdir %BUILD_DIR%
mkdir %DEPLOY_DIR%
%DRIVE%
cd %BUILD_DIR%
git clone %BASE_LOCAL_PYDEV_GIT%
@echo git clone git://github.com/fabioz/Pydev.git -- this could be used when building against the base git instead of a local git
cd Pydev
git reset --hard
git clean -f -d -x
git checkout -f
git remote update
git checkout %BRANCH%
git pull origin %BRANCH%
@echo If copied/pasted into cmd.exe, it will break here

@echo to clean after the build: -DcleanAfter.set=true
mvn -o install





@echo Notes on customizing parameters / making the build:
@echo If signing is needed, then the keystore needs to be created before (see commands below)
@echo and also a variable named SIGN_STOREPASS must be set with the same password used when the keystore is created
@echo i.e.: set SIGN_STOREPASS=my store pass
@echo
@echo BRANCH: the branch to be used to do the build (e.g.: master/development/etc) -- it's recommended that you create your own branch from a base branch in pydev and use it
@echo
@echo BASE_LOCAL_PYDEV_GIT: The directory where you checked out Pydev from git (the folder containing the 'plugins, features, builders, .git')
@echo
@echo BUILD_DIR: The directory that should be created for the build (the contents of the BASE_LOCAL_PYDEV_GIT will be checked out to this directory for the build)
@echo
@echo DEPLOY_DIR: The directory where the final artifacts of the build will be put
@echo
@echo KEYSTORE: A keystore needs to be created and available at X:\release_tools\pydevkeystore
@echo
@echo 	%JAVA_HOME%\bin\keytool -genkey -dname "CN=Brainwy Software, OU=PyDev, O=Brainwy, L=Florianopolis, ST=SC, C=Brazil" -keystore X:\release_tools\pydevkeystore -alias pydev -validity 3650
@echo 	%JAVA_HOME%\bin\keytool -selfcert -alias pydev -keystore X:\release_tools\pydevkeystore -validity 3650
@echo 	%JAVA_HOME%\bin\keytool -export -keystore X:\release_tools\pydevkeystore -alias pydev -file pydev_certificate.cer
@echo
@echo 	To sign
@echo 	%JAVA_HOME%\bin\jarsigner -keystore X:\release_tools\pydevkeystore -storepass PASSUSED JAR_TO_SIGN pydev
@echo
@echo
@echo KEYSTORE_ALIAS: The alias used during the keystore creation
@echo
@echo JAVA_HOME: Where is your java located? (The directory containing 'bin, jre, COPYRIGHT, LICENSE')
@echo
@echo GIT_EXECUTABLE: The place where the git executable is located (in Windows your git.exe)
@echo
@echo ECLIPSE_CLEAN: The DIRECTORY containing your eclipse executable (note that it should be a clean install of the Eclipse SDK) -- it will be copied during the build
@echo
@echo LAUNCHER_PLUGIN: The plugin for the equinox launcher at your eclipse version (i.e.: org.eclipse.equinox.launcher_1.1.0.v20100507)
@echo
@echo BUILDER_PLUGIN: The plugin for the pde builder at your eclipse version (i.e.: org.eclipse.pde.build_3.6.0.v20100603)
@echo
@echo BASEOS: The platform (e.g.: win32, linux, macosx, solaris, etc)
@echo
@echo BASEWS: The windows system (e.g.: win32, gtk, motif, carbon)
@echo
@echo BASEARCH: The architechure (e.g.: x86, ppc, sparc)
@echo
@echo PATH: python, java, git, ant must be in the path
@echo     Additionally for Windows, FastCopy (http://ipmsg.org/tools/fastcopy.html.en) is also required.
@echo
