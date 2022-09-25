class Knight(object):
    def __init__(self, name):
        self.name = name

def create(klass, name):
    return klass(name)

robin = create(Knight, "Sir Robin")
print(robin.name)##|

##r

class Knight(object):
    def __init__(self, name):
        self.p = name

def create(klass, name):
    return klass(name)

robin = create(Knight, "Sir Robin")
print(robin.p)
