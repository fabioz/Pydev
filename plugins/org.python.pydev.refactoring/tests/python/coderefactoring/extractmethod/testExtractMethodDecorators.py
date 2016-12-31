class Foo:

    @decorator
    def foo(self, bar):
        if bar.zoo():
##|            bar.foo()##|
            bar.bar()


##r

class Foo:

    def extracted_method(self, bar):
        return bar.foo()


    @decorator
    def foo(self, bar):
        if bar.zoo():
            self.extracted_method(bar)
            bar.bar()