def m1():
    ##|from bar import foo as b##|
    print(b)

##r

def extracted_method():
    from bar import foo as b
    return b

def m1():
    b = extracted_method()
    print(b)
