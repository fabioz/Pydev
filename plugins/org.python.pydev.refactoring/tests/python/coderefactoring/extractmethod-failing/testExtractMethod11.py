def method1():
 ##|   if idx > 2:
        print ''
    else:
        print ''
    
    if idx == 5:
        print "nothing!"
##|
##c

<config>
  <offsetStrategy>0</offsetStrategy>
</config>


##r
def extracted_method():
    if idx > 2:
        print ''
    else:
        print ''
    
    if idx == 5:
        print "nothing!"

def method1():
    extracted_method()
