

import sys
import os
import traceback
import StringIO


sys.path.insert(1, os.path.join(os.path.dirname(sys.argv[0]), 
    "ThirdParty", "brm"))
import ThirdParty.brm.bike as bike



class Refactoring(object):
    
    def __init__(self):
        self.progress = StringIO.StringIO()
        self.warning = StringIO.StringIO()
        self.init()

    def getLastProgressMsg(self):
        progress = self.progress.getvalue().split('\n')
        msg = ''
        i = -1
        while msg == '' and i > -5:
            try:
                msg = progress[i]
            except:
                msg = ''
            i -= 1
        return msg
    
    def getLastProgressMsgs(self, v):
        progress = self.progress.getvalue().split('\n')
        msg = ''
        i = -1
        while i > -v:
            try:
                msg += progress[i]+'\n'
            except:
                pass
            i -= 1
        return msg

    def init(self):
        """
        Private slot to handle the Reset action.
        """
        self.brmctx = bike.init()
        self.brmctx.setProgressLogger(self.progress)
        self.brmctx.setWarningLogger(self.warning)
    
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
    
def releaseRefactorerBuffers():
    GetRefactorer().warning.close()
    GetRefactorer().progress.close()

    GetRefactorer().warning.__init__()
    GetRefactorer().progress.__init__()

def restartRefactorer():
    global __Refactoring
    __Refactoring = Refactoring()
    
def HandleRefactorMessage(msg, keepAliveThread):
    '''
    The message received should have: the method of the class
    '''
    msgSplit = msg.split(' ')
    
    func = msgSplit.pop(0)
    
    refactorer = GetRefactorer()
    func = getattr(refactorer, func)
    
    keepAliveThread.processMsgFunc = refactorer.getLastProgressMsg
    
    try:
        f = func(*msgSplit)+'END@@'
        releaseRefactorerBuffers()
        return f
    except:
        import sys
        s = StringIO.StringIO()
        exc_info = sys.exc_info()
        
        print >> s, str(exc_info[1])
        
        print >> s, '\nWARNINGS:\n'
        print >> s, refactorer.warning.getvalue()

        print >> s, '\nPROGRESS:\n'
        print >> s, refactorer.getLastProgressMsgs(8)
        
        print >> s, '\nDETAILS:\n'
        traceback.print_exception(exc_info[0], exc_info[1], exc_info[2], limit=None, file = s)
        
        releaseRefactorerBuffers()
        restartRefactorer()
        return 'ERROR:%s\nEND@@'%(s.getvalue().replace('END@@',''))
        

