# pylint: disable-msg=R0903
"""test Invalid name"""

__revision__ = 1

def Run():
    """method without any good name"""
    class B:
        """nested class should not be tested has a variable"""
        def __init__(self):
            pass
    bBb = 1
    return A(bBb)

def run():
    """anothrer method without only good name"""
    class Aaa:
        """nested class should not be tested has a variable"""
        def __init__(self):
            pass
    bbb = 1
    return Aaa(bbb)

A = None

def HOHOHOHO():
    """yo"""
    HIHIHI = 1
    print HIHIHI

class xyz: 
    """yo"""
    def __init__(self):
        pass

    def Youplapoum(self):
        """bad method name"""


def nested_args(arg1, (arg21, arg22)):
    """function with nested arguments"""
    print arg1, arg21, arg22
