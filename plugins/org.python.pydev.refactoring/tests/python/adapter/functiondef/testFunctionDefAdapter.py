class A:
    def __init__(self):
        print("A")
        
class B:
    def __init__(self):
        print("B") 
        
    def simple_meth(self):
        print("simple_meth B")
        
class C(A, B):
    def __init__(self):
        print("C")
        
    def kwarg(self, **kwarg):
        print(kwarg)

class D(B):
    def __init__(self, a):
        self.a = 2
        print("D")
    
    def simple_meth(self):
        print("simple_meth D")
        
    def noarg():
        print("foo")
    
    def vararg(self, *vararg):
        print(vararg)
        
class E(D, B):
    def __init__(self):
        D.__init__(self, 2)
        print("E")
        
e = E()
d = D()
d.simple_meth()
        
##r
# ClassName FunctionName hasArg hasVarArg hasKwArg ArgumentsOnly
# A
# __init__ true false false [self]

# B
# __init__ true false false [self]
# simple_meth true false false [self]

# C
# __init__ true false false [self]
# kwarg true false true [self]

# D
# __init__ true false false [self, a]
# simple_meth true false false [self]
# noarg false false false []
# vararg true true false [self]

# E
# __init__ true false false [self]