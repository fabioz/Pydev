# Copyright (c) 2000-2004 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr

# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.

# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
"""
logilab.common packaging information
"""

__revision__ = "$Id: __pkginfo__.py,v 1.1 2004-10-26 12:52:29 fabioz Exp $"

modname = 'common'
numversion = (0, 6, 0)
version = '.'.join([str(num) for num in numversion])

license = 'GPL'
copyright = '''Copyright (c) 2003-2004 LOGILAB S.A. (Paris, FRANCE).
http://www.logilab.fr/ -- mailto:contact@logilab.fr'''

author = "Logilab"
author_email = "contact@logilab.fr"

short_desc = "useful miscellaneous modules used by Logilab projects"

long_desc = """logilab-common is a collection of low-level Python packages and modules,
 designed to ease:
  * handling command line options and configuration files
  * writing interactive command line tools
  * manipulation files and character strings
  * interfacing to OmniORB
  * generating of SQL queries
  * running unit tests
  * manipulating tree structures
  * accessing RDBMS (currently postgreSQL and mysql)
  * generating text and HTML reports
  * logging"""


web = "http://www.logilab.org/projects/%s" % modname
ftp = "ftp://ftp.logilab.org/pub/%s" % modname
mailinglist = "mailto://python-projects@lists.logilab.org"

subpackage_of = 'logilab'

from os.path import join
include_dirs = [join('test', 'data')]

debian_name = 'logilab-common'
debian_maintainer = 'Alexandre Fayolle'
debian_maintainer_email = 'alexandre.fayolle@logilab.fr'
debian_handler = 'python-library'
