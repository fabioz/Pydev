# this file is both an indepentend test and part of the relative_import test. In both cases
# the outcome should be successful (of course)

class SameLevel(object):
    def __init__(self, arg):
        self.field = "x"
        
    def get_field(self):
        x = self.field
        return x


obj = SameLevel()

z = obj.get_field()

z # type str