# pylint: disable-msg=W0622
# Copyright (c) 2003-2004 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr
#
# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
"""pylint packaging information"""

__revision__ = '$Id: __pkginfo__.py,v 1.1 2004-10-26 12:52:28 fabioz Exp $'


modname = 'pylint'

numversion = (0, 4, 2)
version = '.'.join([str(num) for num in numversion])

license = 'GPL'
copyright = '''Copyright (c) 2003-2004 LOGILAB S.A. (Paris, FRANCE).
http://www.logilab.fr/ -- mailto:contact@logilab.fr'''

short_desc = "Python code static checker"
long_desc = """Pylint is tool to analyze Python source code looking for bugs and signs
  of poor quality. It can check if a module satisfies a coding standard.
  Pylint can be seen as another PyChecker since nearly all tests you can
  do with PyChecker can also be done with Pylint. However, Pylint offers
  some more features, like checking line-code's length, checking if
  variable names are well-formed according to your coding standard, or
  checking if declared interfaces are truly implemented, and much more.
  .
  Additionnaly, it is possible to write plugins to add your own checks,
  using a plugin mechanism."""

author = "Sylvain Thenault"
author_email = "sylvain.thenault@logilab.fr"

web = "http://www.logilab.org/projects/%s/" % modname
ftp = "ftp://ftp.logilab.org/pub/%s/" % modname
mailinglist = "mailto://python-projects@lists.logilab.org"

from os.path import join
scripts = (join('bin','pylint'), join('bin','pylint-gui'))

subpackage_of = 'logilab'

include_dirs = [join('test', 'input'), join('test', 'messages')]

pyversions = ["2.2", "2.3"]

debian_maintainer = 'Alexandre Fayolle'
debian_maintainer_email = 'alexandre.fayolle@logilab.fr'
