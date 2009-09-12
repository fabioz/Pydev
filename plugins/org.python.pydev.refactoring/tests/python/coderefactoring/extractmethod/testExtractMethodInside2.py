a = 5
b = 5
if (a == ##|b##|):
    print "equal"

##r

def extracted_method(b):
    return b

a = 5
b = 5
if (a == extracted_method(b)):
    print "equal"