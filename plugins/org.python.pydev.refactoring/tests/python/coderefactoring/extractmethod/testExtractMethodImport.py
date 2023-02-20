##|import foo
a = 10##|
print(foo)
print(a)

##r

def extracted_method():
    import foo
    a = 10
    return foo, a

foo, a = extracted_method()
print(foo)
print(a)