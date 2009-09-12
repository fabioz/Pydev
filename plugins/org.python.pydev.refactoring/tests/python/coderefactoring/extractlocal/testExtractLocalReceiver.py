class ClassA(object):
    def do_something(self):
        pass

def test():
    ##|ClassA()##|.do_something()

##r

class ClassA(object):
    def do_something(self):
        pass

def test():
    extracted_variable = ClassA()
    extracted_variable.do_something()