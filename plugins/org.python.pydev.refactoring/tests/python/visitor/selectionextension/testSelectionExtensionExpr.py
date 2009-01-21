class A:
    def test(self):
        print "Initializing A", "test"
        anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()

##c Should expand to Full String "Initializing A"
'''
<config>
  <offset>43</offset>
  <selectionLength>24</selectionLength>
</config>
'''

##r
"Initializing A", "test"