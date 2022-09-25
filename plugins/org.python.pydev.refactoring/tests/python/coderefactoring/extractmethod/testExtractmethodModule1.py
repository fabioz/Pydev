hello = "foo"
##|print(hello)##|

##r extract method from module body

def extracted_method(hello):
    return print(hello)

hello = "foo"
extracted_method(hello)