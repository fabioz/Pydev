
if rand():
    from complex_pkg.modulea import Foo as Alias
    Alias() ## type Foo
else:
    from complex_pkg.moduleb import Bar as Alias
    Alias() ## type Bar

x = Alias()
x ## type Bar|Foo
x.method(5.5) # type int|str


if random():
    import complex_pkg.modulea as module
else:
    module = 1
    
module ## type int|modulea
