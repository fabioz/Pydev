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
  <offset>37</offset>
  <selectionLength>2</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
print "Initializing A"