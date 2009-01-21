class A:
    def test(self):
        if (1 == 2):
            print "foo"
        else:
            print "bar"
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
if (1 == 2):
    print "foo"
else:
    print "bar"