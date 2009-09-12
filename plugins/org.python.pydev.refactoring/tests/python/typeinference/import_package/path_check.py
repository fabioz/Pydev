# the dot here doesn't change anything really

from .os import Os
from import_package.foo import Foo

class PathCheck(object):
    def get_rel_os(self):
        return Os
    
    def get_abs_foo(self):
        return Foo
        