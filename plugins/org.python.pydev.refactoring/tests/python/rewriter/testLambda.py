def make_incrementer(n):
    return lambda x:x + n

a = make_incrementer(5)
print a(10)
def varprinter(a, b, *args, **kwargs):
    print a, b, args, kwargs

def make_lambda(a, b, *c, **d):
    return lambda a, f=b, *g, **h:varprinter(a)

def make_foo(a, ):
    print a

make_lambda("1", "2")
make_foo("foo")