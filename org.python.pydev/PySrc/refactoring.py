

import sys
import os



sys.path.insert(1, os.path.join(os.path.dirname(sys.argv[0]), 
    "ThirdParty", "brm"))
import ThirdParty.brm.bike as bike



class Refactoring(object):
    
    def __init__(self):
        self.init()
    
    def init(self):
        """
        Private slot to handle the Reset action.
        """
        self.brmctx = bike.init()
        self.logging = Preferences.getRefactoring("Logging")
        if self.logging:
            self.brmctx.setProgressLogger(self.ui.stdout)
        else:
            self.brmctx.setProgressLogger(SilentLogger())
        self.brmctx.setWarningLogger(self.ui.stderr)
    
    def handleReset(self):
        """
        Private slot to handle the Reset action.
        """
        self.init()
        
    
    

