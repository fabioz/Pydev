

import sys
import os
import traceback
import StringIO


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
        self.brmctx.setProgressLogger(sys.stdout)
        self.brmctx.setWarningLogger(sys.stderr)
    
    def handleReset(self):
        """
        Private slot to handle the Reset action.
        """
        self.init()
        

    def extractMethod(self, filename, startline, startcolumn, 
                            endline, endcolumn, newname):
        '''
        Receives all as a string and changes to correct type.
        '''
        self.brmctx.extractMethod(filename, int(startline), int(startcolumn), 
                                            int(endline), int(endcolumn), 
                                            newname)
        savedfiles = self.brmctx.save()
        return str(savedfiles)

    def renameByCoordinates(self, filename, line, column, newname):
        '''
        Receives all as a string and changes to correct type.
        '''
        self.brmctx.renameByCoordinates(filename, int(line), int(column), newname)
        savedfiles = self.brmctx.save()
        return str(savedfiles)

__Refactoring = None

def GetRefactorer():
    global __Refactoring
    if __Refactoring is None:
        __Refactoring = Refactoring()
    
    return __Refactoring
    
def HandleRefactorMessage(msg):
    '''
    The message received should have: the method of the class
    '''
    msgSplit = msg.split(' ')
    
    func = msgSplit.pop(0)
    
    func = getattr(GetRefactorer(), func)
    
    try:
        return func(*msgSplit)+'END@@'
    except:
        import sys
        s = StringIO.StringIO()
        exc_info = sys.exc_info()
        print >> s, str(exc_info[1])
        traceback.print_exception(exc_info[0], exc_info[1], exc_info[2], limit=None, file = s)
        return 'ERROR: %s\nEND@@'%(s.getvalue())
        

