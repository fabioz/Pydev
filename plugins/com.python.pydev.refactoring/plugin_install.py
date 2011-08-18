class Action:
    
    def __init__(self, class_, key, desc, group):
        if not isinstance(key, (list, tuple)):
            self.key = [key]
        else:
            self.key = key
        self.desc = desc
        self.class_ = class_
        self.id = class_+'Command'
        self.group = group
        

ACTIONS_AND_BINDING = [
    #The actions below are now available through the Ctrl+1 quick fixes!
    #Action('com.python.pydev.refactoring.tdd.PyCreateClass', 'Alt+Shift+S C', 'CreateClass', 'tddGroup'),
    #Action('com.python.pydev.refactoring.tdd.PyCreateMethodOrField', 'Alt+Shift+S M', 'CreateMethod', 'tddGroup'),
    Action('org.python.pydev.refactoring.ui.actions.ExtractMethodAction', ('M2+M3+M', 'M3+M2+T E'), 'ExtractMethod', 'pepticRefactoringGroup'),
]