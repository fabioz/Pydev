class A(B,
        C):
    def test(self):
        print "Initializing A"
        anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()

##c
'''
<config>
  <offset>51</offset>
  <selectionLength>22</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
class A(B,
        C):

    def pepticMethod(self):
        print "Initializing A"

    def test(self):
        self.pepticMethod()
        anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()