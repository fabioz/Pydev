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
  <offset>45</offset>
  <selectionLength>22</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
# Invalid selection:
# nitializing A", "test"