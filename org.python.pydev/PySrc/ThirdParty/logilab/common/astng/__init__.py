# Copyright (c) 2003-2004 Sylvain Thenault (thenault@nerim.net)
# Copyright (c) 2003-2004 Logilab
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
"""astng enhances the compiler.ast module from the standard library.

Look at the astng submodule for more info.
"""

__author__ = u"Sylvain Thenault"
__revision__ = "$Id: __init__.py,v 1.5 2005-04-19 14:39:11 fabioz Exp $"

class ASTNGError(Exception):
    """base exception class for all astng related exceptions
    """

class ASTNGBuildingException(ASTNGError):
    """exception class when we are not able to build an astng representation"""

class ResolveError(ASTNGError):
    """raised when we are unabled to resolve a name"""

class NotFoundError(ASTNGError):
    """raised when we are unabled to resolve a name"""

from logilab.common.astng.manager import ASTNGManager, Project, Package
from logilab.common.astng.utils import ASTWalker, IgnoreChild
from logilab.common.astng.astng import *
