class A:
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
  <offset>43</offset>
  <selectionLength>17</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
"Initializing A"