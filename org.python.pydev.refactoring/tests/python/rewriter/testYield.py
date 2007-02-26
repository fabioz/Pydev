ble = "abc"
def simpleYield(foo, bar=5.0, mar=ble, *arg, **args):
    # this is a yield
    yield ("yield") # foo
    yield "yield"

print "foo", simpleYield(1, 3.0)