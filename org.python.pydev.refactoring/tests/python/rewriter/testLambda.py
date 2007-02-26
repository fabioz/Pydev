def make_incrementor(n):
    return lambda x: x + n

a = make_incrementor(5)
print a(10)
def varprinter(a, b, *arg, **args):
    print a, b, arg, args

def make_lambda(a, b, *c, **d):
    return lambda a, f=b, *g, **h: varprinter(a)

def make_foo(a, ):
    print a

make_lambda("1", "2")
make_foo("foo")