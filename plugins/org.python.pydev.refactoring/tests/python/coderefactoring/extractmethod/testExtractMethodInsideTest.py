a = 5
b = 5
if (##|a##| == b):
    print "equal"

##r

def extracted_method(a):
    return a

a = 5
b = 5
if (extracted_method(a) == b):
    print "equal"