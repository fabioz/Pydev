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

##c this test will extract a method and rename the variables
'''
<config>
  <offset>48</offset>
  <selectionLength>36</selectionLength>
  <offsetStrategy>0</offsetStrategy>
  <renameMap>
    <entry>
      <string>a</string>
      <string>b</string>
    </entry>
  </renameMap>  
</config>
'''

##r
class A:

    def pepticMethod(self, b):
        while (b < 2):
            b += 1
        
        return b

    def test(self):
        a = 1
        a = self.pepticMethod(a)
        var = a * a
        print var
            
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()
