# pylint: disable-msg=R0903,R0922
"""test overriding of abstract method
"""

__revision__ = '$Id: func_w0223.py,v 1.3 2005-02-24 18:28:49 fabioz Exp $'

class Abstract:
    """abstract class
    """
    def aaaa(self):
        """should be overriden in concrete class"""
        raise NotImplementedError()


    def bbbb(self):
        """should be overriden in concrete class"""
        raise NotImplementedError()

    def __init__(self):
        pass

class Concret(Abstract):
    """concret class"""

    def aaaa(self):
        """overidden form Abstract"""
        print self
