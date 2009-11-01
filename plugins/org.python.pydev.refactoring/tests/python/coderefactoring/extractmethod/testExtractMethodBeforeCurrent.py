def m1(self):
    pass
def m2(self):
    print ##|'here'##|
    

##c
'''
<config>
  <offsetStrategy>8</offsetStrategy>
</config>
'''
##r
def m1(self):
    pass

def extracted_method():
    return 'here'

def m2(self):
    print extracted_method() 
