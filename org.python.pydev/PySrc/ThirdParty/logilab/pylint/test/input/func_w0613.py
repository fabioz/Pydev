# pylint: disable-msg=R0903
"""test unused argument
"""

__revision__ = 1

def function(arg=1):
    """ignore arg"""


class AAAA:
    """dummy class"""

    def method(self, arg):
        """dummy method"""

    def __init__(self):
        pass
