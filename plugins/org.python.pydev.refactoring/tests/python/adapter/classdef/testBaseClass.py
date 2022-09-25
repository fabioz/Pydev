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

class D(B):
    def __init__(self, a):
        self.a = 2
        print("D")
    
    def simple_meth(self):
        print("simple_meth D")
    
        
class E(D, B):
    def __init__(self):  
        D.__init__(self, 2)
        print("E")
        
e = E()
d = D()
d.simple_meth()
        
##r
# A
# B
# C A B
## C Base: A
## C Base: B
## C Base: object
# D B
## D Base: B
## D Base: object
# E D B
## E Base: B
## E Base: D
## E Base: object