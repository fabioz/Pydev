class A:
    def __init__(self):
        print "A"
        
class B:
    def __init__(self):
        print "B" 
        
    def simpleMeth(self):
        print "simpleMeth B"
        
class C(A, B):
    def __init__(self):
        print "C"

class D(B):
    def __init__(self, a):
        self.a = 2
        print "D"
    
    def simpleMeth(self):
        print "simpleMeth D"
    
        
class E(D, B):
    def __init__(self):  
        D.__init__(self, 2)
        print "E"
        
e = E()
d = D()
d.simpleMeth()
        
##r
# A
# B
# C A B
## C Base: A
## C Base: B
# D B
## D Base: B
# E D B
## E Base: B
## E Base: D 