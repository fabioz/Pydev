# pylint: disable-msg=R0903
"""test for call to __init__ from a non ancestor class
"""

__revision__ = '$Id: func_w0233.py,v 1.1 2005-01-21 17:46:14 fabioz Exp $'

class AAAA:
    """ancestor 1"""

    def __init__(self):
        print 'init', self
        BBBB.__init__(self)

class BBBB:
    """ancestor 2"""

    def __init__(self):
        print 'init', self
        
