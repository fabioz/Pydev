class A:
    def test(self):
        a = 5
        var = a * a
        print var
            
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()

##c
'''
<config>
  <offset>51</offset>
  <selectionLength>11</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
class A:

    def pepticMethod(self, a):
        var = a * a
        return var

    def test(self):
        a = 5
        var = self.pepticMethod(a)
        print var
            
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()