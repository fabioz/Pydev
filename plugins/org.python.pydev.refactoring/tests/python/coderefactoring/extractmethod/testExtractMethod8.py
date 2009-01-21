class A:
    def test(self):
        a = 1
        while (a < 2):
            a += 1
        var = a * a
        print var
            
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()

##c selection starts somewhere before the "while"-node, still have to normalize selected code in order to parse it
'''
<config>
  <offset>48</offset>
  <selectionLength>36</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
class A:

    def pepticMethod(self, a):
        while (a < 2):
            a += 1
        
        return a

    def test(self):
        a = 1
        a = self.pepticMethod(a)
        var = a * a
        print var
            
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()