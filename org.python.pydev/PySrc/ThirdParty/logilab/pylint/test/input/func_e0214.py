"""mcs test"""

__revision__ = 1

class MetaClass(type):
    """a very intersting metaclass"""
    def __new__(mcs, name, bases, cdict):
        print mcs, name, bases, cdict
        return type.__new__(mcs, name, bases, cdict)

    def whatever(self):
        """should have mcs has first arg"""
