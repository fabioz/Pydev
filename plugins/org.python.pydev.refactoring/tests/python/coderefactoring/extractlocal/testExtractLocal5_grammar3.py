def m1():
    a.Get(a=1).GetBar(something)
    ##|a.Get(a=1)##|.Foo()

##r

def m1():
    extracted_variable = a.Get(a=1)
    extracted_variable.GetBar(something)
    extracted_variable.Foo()
