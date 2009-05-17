

import sys
import os
import traceback
import StringIO
import urllib

#kind of hack to get the bicicle repair man without having it in the pythonpath.
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
                msg += progress[i] + '\n'
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


    def inlineLocalVariable(self, filename, line, column):
        '''
        Receives all as a string and changes to correct type.
        '''
        self.brmctx.inlineLocalVariable(filename, int(line), int(column))
        savedfiles = self.brmctx.save()
        return str(savedfiles)
        

    def extractLocalVariable(self, filename, begin_line, begin_col,
                             end_line, end_col, newname):
        '''
        Receives all as a string and changes to correct type.
        '''
        self.brmctx.extractLocalVariable(filename, int(begin_line), int(begin_col),
                             int(end_line), int(end_col), newname)
        savedfiles = self.brmctx.save()
        return str(savedfiles)

    
    def findDefinition(self, filename, line, column):
        '''
        Receives all as a string and changes to correct type.
        '''
        defns = self.brmctx.findDefinitionByCoordinates(filename, int(line), int(column))
        
        ret = ''
        
        for ref in defns:
            ret += '(%s)' % str(ref)
        
        return '[%s]' % ret


__Refactoring = None

def GetRefactorer():
    global __Refactoring
    if __Refactoring is None:
        __Refactoring = Refactoring()
    
    return __Refactoring
    
def restartRefactorerBuffer():
    r = GetRefactorer()
    r.warning.close()
    r.progress.close()

    r.warning.__init__()
    r.progress.__init__()

def restartRefactorer():
    global __Refactoring
    __Refactoring = Refactoring()
    
def HandleRefactorMessage(msg, keepAliveThread):
    '''
    The message received should have: the method of the class and its parameters.
    '''
    msgSplit = msg.split('|')
    
    func = msgSplit.pop(0)
    
    refactorer = GetRefactorer()
    func = getattr(refactorer, func)
    
    keepAliveThread.processMsgFunc = refactorer.getLastProgressMsg
    
    try:
        f = func(*msgSplit)
        restartRefactorerBuffer()
        s = urllib.quote_plus(f)
        return 'BIKE_OK:%s\nEND@@' % (s)
    except:
        import sys
        s = StringIO.StringIO()
        exc_info = sys.exc_info()
        
        s.write(str(exc_info[1]))
        
        s.write('\nWARNINGS:\n\n')
        s.write('%s\n' % (refactorer.warning.getvalue(),))

        s.write('\nPROGRESS:\n\n')
        s.write('%s\n' % (refactorer.getLastProgressMsgs(8),))
        
        s.write('\nDETAILS:\n\n')
        traceback.print_exception(exc_info[0], exc_info[1], exc_info[2], limit=None, file=s)
        
        restartRefactorerBuffer()
        restartRefactorer()
        s = urllib.quote_plus(s.getvalue())
        return 'ERROR:%s\nEND@@' % (s)
        

