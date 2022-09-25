class Test(object):
    def setUp(self):
        class Stub(object):
            def __init__(self):
                self.a = b['c']
            def GetFoo(self):
                for a in ##|10##|:
                    print(a)

##r

class Test(object):
    def setUp(self):
        class Stub(object):
            def __init__(self):
                self.a = b['c']
            def GetFoo(self):
                extracted_variable = 10
                for a in extracted_variable:
                    print(a)
