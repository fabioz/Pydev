# Copyright (c) 2002-2004 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr
#
# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
"""bug report 8086 in JPL"""

__revision__ = '$Id: test_indirectInterface.py,v 1.1 2005-01-21 17:46:21 fabioz Exp $'

import unittest
from utils import get_tests_info, fix_path, TestReporter

from logilab.pylint.lint import PyLinter
from logilab.pylint import checkers

test_reporter = TestReporter()
linter = PyLinter()
linter.set_reporter(test_reporter)
linter.config.persistent = 0
linter.quiet = 1
checkers.initialize(linter)

class IndirectInterface(unittest.TestCase):

    """shows a bug where pylint can't find interfaces when they are
    used indirectly. See input/indirect[123].py for details on the
    setup"""

    def test_indirect(self):
        linter.check('input/indirect3.py')
        for message in test_reporter.messages:
            if message.endswith('ConcreteToto: Unable to resolve TotoInterface'):
                self.fail(message)
        
        

if __name__ == '__main__':
    unittest.main()
