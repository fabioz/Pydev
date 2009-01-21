class A:
    def test(self):
        a = 5
        var = a * a
        print var
            
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()

##c selection starts at the first char
'''
<config>
  <offset>37</offset>
  <selectionLength>25</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
class A:

    def pepticMethod(self):
        a = 5
        var = a * a
        return var

    def test(self):
        var = self.pepticMethod()
        print var
            
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()