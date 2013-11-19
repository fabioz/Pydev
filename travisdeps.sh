#!/bin/bash

# This script installs testing deps on travis (but should work on any Ubuntu 12.04 or similar setup)
# Each section installs the given tool with the corresponding entries in TestDependent.travis.properties
# The variables in TestDependent that don't appear in here are handled by Travis "automatically"
# Where possible we install packages with apt because on travis that is fast.

set -e
set -x

INSTALL="sudo apt-get install -qq"

# wxPython PYTHON_WXPYTHON_PACKAGES
$INSTALL python-wxgtk2.8

# numpy PYTHON_NUMPY_PACKAGES
$INSTALL python-numpy

# django PYTHON_DJANGO_PACKAGES
$INSTALL python-django

# Qt4 PYTHON_QT4_PACKAGES
$INSTALL python-qt4

# OpenGL PYTHON_OPENGL_PACKAGES
$INSTALL python-opengl

# MX DateTime PYTHON_MX_PACKAGES
$INSTALL python-egenix-mxdatetime

# PIL PYTHON_PIL_PACKAGES
$INSTALL python-imaging

# Python 3 PYTHON_30_LIB
$INSTALL python3

# IronPython IRONPYTHON_EXE, IRONPYTHON_LIB
# IronPython is not part of Ubuntu
# URL for IPython download from http://ironpython.codeplex.com/releases/view/90087 (IronPython 2.7.4 Binaries link)
# Unfortunately it now seems problematic to automate the download directly off codeplex, therefore
# download it off a mirror
# OLD COMMAND: wget -O /tmp/IronPython-2.7.4.zip 'http://download-codeplex.sec.s-msft.com/Download/Release?ProjectName=ironpython&DownloadId=723207&FileTime=130230841338900000&Build=20779'
wget -O /tmp/IronPython-2.7.4.zip https://s3.amazonaws.com/pydevbuilds2/IronPython-2.7.4.zip
# TODO Getting all of mono-complete may be overkill
$INSTALL mono-complete
(cd /tmp && unzip -q /tmp/IronPython-2.7.4.zip)
chmod +x /tmp/IronPython-2.7.4/ipy.exe
/tmp/IronPython-2.7.4/ipy.exe -V
/tmp/IronPython-2.7.4/ipy.exe -c "print 'from IronPython'"

# Jython JYTHON_JAR_LOCATION, JYTHON_LIB_LOCATION
# TODO Jython package in Ubuntu is not currently supported by PyDev
# This get from Maven can sometimes be very slow, instead get it from our mirror
# OLD COMMAND: wget -O /tmp/jython-installer-2.5.3.jar http://search.maven.org/remotecontent?filepath=org/python/jython-installer/2.5.3/jython-installer-2.5.3.jar
wget -O /tmp/jython-installer-2.5.3.jar https://s3.amazonaws.com/pydevbuilds2/jython-installer-2.5.3.jar
java -jar /tmp/jython-installer-2.5.3.jar -s -d /tmp/jython-2.5.3
java -jar /tmp/jython-2.5.3/jython.jar -V
java -jar /tmp/jython-2.5.3/jython.jar -c "print 'from Jython'"
