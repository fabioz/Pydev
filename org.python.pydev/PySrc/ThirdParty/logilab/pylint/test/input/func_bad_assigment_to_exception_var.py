"""ho ho ho"""

__revision__ = 'toto'

import sys

e = 1
e2 = 'yo'
e3 = None
try:
    raise e, 'toto'
except Exception, ex:
    print ex
    _, _, tb = sys.exc_info()
    raise e2


def func():
    """bla bla bla"""
    raise e3


raise e3
