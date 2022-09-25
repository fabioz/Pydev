def m1():
    ##|import foo##|
    print(foo)

##r

def extracted_method():
    import foo
    return foo

def m1():
    foo = extracted_method()
    print(foo)
