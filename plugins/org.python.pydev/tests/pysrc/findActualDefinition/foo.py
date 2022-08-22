from .bar import Bar

class Foo(Bar):
    def foo(self):
        self._endpoint.notify()