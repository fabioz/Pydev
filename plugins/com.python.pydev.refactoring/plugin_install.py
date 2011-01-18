class Action:
    
    def __init__(self, class_, key, desc):
        if not isinstance(key, (list, tuple)):
            self.key = [key]
        else:
            self.key = key
        self.desc = desc
        self.class_ = class_
        self.id = class_+'Command'
        

ACTIONS_AND_BINDING = [
    Action('com.python.pydev.refactoring.tdd.PyCreateClass', 'Alt+Shift+S C', 'CreateClass'),
    Action('com.python.pydev.refactoring.tdd.PyCreateMethod', 'Alt+Shift+S M', 'CreateMethod'),
    Action('org.python.pydev.refactoring.ui.actions.ExtractMethodAction', ('M2+M3+M', 'M3+M2+T E'), 'ExtractMethod'),
]