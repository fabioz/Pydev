"""docstring"""

__revision__ = ''

class Interface:
    """base class for interfaces"""

class IMachin(Interface):
    """docstring"""
    def truc(self):
        """docstring"""
        
    def troc(self, argument):
        """docstring"""

class Correct1:
    """docstring"""
    __implements__ = IMachin

    def __init__(self):
        pass

    def truc(self):
        """docstring"""
        pass
    
    def troc(self, argument):
        """docstring"""
        pass
    
class Correct2:
    """docstring"""
    __implements__ = (IMachin,)

    def __init__(self):
        pass

    def truc(self):
        """docstring"""
        pass
    
    def troc(self, argument):
        """docstring"""
        print argument

class MissingMethod:
    """docstring"""
    __implements__ = IMachin,

    def __init__(self):
        pass

    def troc(self, argument):
        """docstring"""
        print argument
   
    def other(self):
        """docstring"""
     
class BadArgument:
    """docstring"""
    __implements__ = (IMachin,)

    def __init__(self):
        pass
 
    def truc(self):
        """docstring"""
        pass
    
    def troc(self):
        """docstring"""
        pass
    
