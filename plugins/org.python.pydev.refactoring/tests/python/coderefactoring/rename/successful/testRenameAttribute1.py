class Person(object):
    def __init__(self, name):
        self.na##|me = name
        
    def __str__(self):
        return "Person " + self.name

Person("Lancelot").name

robin = Person("Sir Robin")
print(robin)
print(robin.name)

##r

class Person(object):
    def __init__(self, name):
        self.p = name
        
    def __str__(self):
        return "Person " + self.p

Person("Lancelot").p

robin = Person("Sir Robin")
print(robin)
print(robin.p)
