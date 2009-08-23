import renameselfclass
class RenameSelfClass2(renameselfclass.RenameSelfClass):
    def __init__(self):
        self.instance1 = 1
        self.instance2 = 1

        
RenameSelfClass2().instance1 = 2
#instance1 comment
'instance1 string'
