class Process:

    def Foo(self):
        if ##|bar.GetLocation()##| == 1:
            pass
        elif bar.GetLocation() == 1:
            pass

##r
class Process:

    def Foo(self):
        extracted_variable = bar.GetLocation()
        if extracted_variable == 1:
            pass
        elif extracted_variable == 1:
            pass

