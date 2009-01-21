class A:
  
    def test(self):
        anAttribute = "hello" # peptic rocks
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()

##c
'''
<config>
  <offset>54</offset>
  <selectionLength>22</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
class A:

    def pepticMethod(self):
        return "hello" # peptic rocks

  
    def test(self):
        anAttribute = self.pepticMethod()
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()