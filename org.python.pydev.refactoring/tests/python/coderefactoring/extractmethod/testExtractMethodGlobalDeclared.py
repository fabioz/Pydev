class A:
    def test(self):
        a = True
        a = True
    
a = A()
a.test()

##c
'''
<config>
  <offset>54</offset>
  <selectionLength>8</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
class A:

    def pepticMethod(self):
        a = True

    def test(self):
        a = True
        self.pepticMethod()
    
a = A()
a.test()