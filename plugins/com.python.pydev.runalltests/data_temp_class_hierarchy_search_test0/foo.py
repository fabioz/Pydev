class Root(object):
    pass
class Mid1(Root):
    pass
class Mid2(Root):
    pass
class Leaf(Mid1, Mid2):
    pass

