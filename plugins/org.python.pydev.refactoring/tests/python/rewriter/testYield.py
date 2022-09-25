ble = "abc"
def simple_yield(foo, bar=5.0, mar=ble, *args, **kwargs):
    # this is a yield
    yield ("yield") # foo
    yield "yield"

print("foo", simple_yield(1, 3.0))