'''
@author Fabio Zadrozny 
'''
import sys
import os

#make it as if we were executing from the directory above this one (so that we can use pycompletionserver
#without the need for it being in the pythonpath)
sys.argv[0] = os.path.dirname(sys.argv[0]) 
#twice the dirname to get the previous level from this file.
sys.path.insert(1, os.path.join(  os.path.dirname( sys.argv[0] )) )

import unittest
import pycompletionserver
import socket
import urllib



class Test(unittest.TestCase):

    def setUp(self):
        unittest.TestCase.setUp(self)

    def tearDown(self):
        unittest.TestCase.tearDown(self)
    
    def testMessage(self):
        t = pycompletionserver.T(0,0)
        
        l = []
        l.append(('Def','description'  ))
        l.append(('Def1','description1'))
        l.append(('Def2','description2'))
        
        msg = t.formatCompletionMessage(l)
        self.assertEquals('@@COMPLETIONS((Def,description),(Def1,description1),(Def2,description2))END@@', msg)
        
        l = []
        l.append(('Def','desc,,r,,i()ption'  ))
        l.append(('Def(1','descriptio(n1'))
        l.append(('De,f)2','de,s,c,ription2'))
        msg = t.formatCompletionMessage(l)
        self.assertEquals('@@COMPLETIONS((Def,desc%2C%2Cr%2C%2Ci%28%29ption),(Def%281,descriptio%28n1),(De%2Cf%292,de%2Cs%2Cc%2Cription2))END@@', msg)

    def createConnections(self, p1 = 50002,p2 = 50003):
        '''
        Creates the connections needed for testing.
        '''
        t = pycompletionserver.T(p1,p2)
        
        t.start()

        sToWrite = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sToWrite.connect((pycompletionserver.HOST, p1))
        
        sToRead = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sToRead.bind((pycompletionserver.HOST, p2))
        sToRead.listen(1) #socket to receive messages.

        connToRead, addr = sToRead.accept()

        return t, sToWrite, sToRead, connToRead, addr
        

    def readMsg(self):
        msg = '@@PROCESSING_END@@'
        while msg.startswith('@@PROCESSING'):
            msg = self.connToRead.recv(1024*4)
            if msg.startswith('@@PROCESSING:'):
                print 'Status msg:', msg

        return msg

    def testCompletionSocketsAndMessages(self):
        t, sToWrite, sToRead, self.connToRead, addr = self.createConnections()
        
        try:
            #now that we have the connections all set up, check the code completion messages.
            msg = urllib.quote_plus('import math\n')
            sToWrite.send('@@GLOBALS:%sEND@@'%msg) #only 1 global should be returned: math itself.
            completions = self.readMsg()
            
            
            msg = urllib.quote_plus('This module is always available.  It provides access to the\n'\
                               'mathematical functions defined by the C standard.')
            start = '@@COMPLETIONS((math,%s)'%msg
            self.assert_(completions.startswith(start), '%s DOESNT START WITH %s' % ( completions, start) ) #it returns math and builtins...just check for math.
    
            
            msg1 = urllib.quote_plus('math')
            msg2 = urllib.quote_plus('import math\n')
            #check token msg.
            sToWrite.send('@@TOKEN_GLOBALS(%s):%sEND@@' % (msg1, msg2)) 
            completions = self.readMsg()
    
            self.assert_('@@COMPLETIONS' in completions)
            self.assert_('END@@' in completions)

            s = \
'''
class C(object):          
                           
    def __init__(self):           
                          
        print dir(self)       
                             
    def a(self):                
        pass                             
                                 
                                
    def b(self):                   
        self.c=1                    
                                
        pass            
'''     
            msg = urllib.quote_plus(s)
    
            sToWrite.send('@@TOKEN_GLOBALS(C):%s\nEND@@'%s) 
            completions = self.readMsg()
    
            sToWrite.send('@@CLASS_GLOBALS(C):%s\nEND@@'%s) 
            completions2 = self.readMsg()
            self.assert_(len(completions) != len(completions2))

        
        #reload modules test
#        sToWrite.send('@@RELOAD_MODULES_END@@')
#        ok = self.readMsg()
#        self.assertEquals('@@MSG_OK_END@@' , ok)
#        this test is not executed because it breaks our current enviroment.
        
        
        
            #change dir test
            curr = os.getcwd( ) 
            newDir = None
            
            if curr.find('/') != -1:
                newDir = curr[0:curr.rindex('/')]
            elif curr.find('\\') != -1:
                newDir = curr[0:curr.rindex('\\')]
            
            self.assert_(newDir != None)
            newDir = urllib.quote_plus(newDir)
            sToWrite.send('@@CHANGE_DIR:%sEND@@'%newDir)
            ok = self.readMsg()
            self.assertEquals('@@MSG_OK_END@@' , ok)
            
            msg1 = urllib.quote_plus('math.acos') #with point
            msg2 = urllib.quote_plus('import math\n')
            sToWrite.send('@@TOKEN_GLOBALS(%s):%sEND@@' %(msg1, msg2)) 
            completions = self.readMsg()
            self.assert_('@@COMPLETIONS' in completions)
            self.assert_('END@@' in completions)
    
            msg1 = urllib.quote_plus('math acos') #with space
            msg2 = urllib.quote_plus('import math\n')
            sToWrite.send('@@TOKEN_GLOBALS(%s):%sEND@@' %(msg1, msg2)) 
            completions2 = self.readMsg()
            self.assertEquals(completions, completions2)
        finally:
            try:
                self.sendKillMsg(sToWrite)
                
        
                while not hasattr(t, 'ended'):
                    pass #wait until it receives the message and quits.
        
                    
                sToRead.close()
                sToWrite.close()
                self.connToRead.close()
            except:
                pass
        
    def sendKillMsg(self, socket):
        socket.send(pycompletionserver.MSG_KILL_SERVER)
        
    
    def testRefactoringSocketsAndMessages(self):
        t, sToWrite, sToRead, self.connToRead, addr = self.createConnections(50002+2,50003+2)

        import refactoring
        from test_refactoring import delete, createFile, FILE, getInitialFile, getRenameRefactored
        createFile(FILE, getInitialFile())
        
        sToWrite.send('@@BIKEfindDefinition %s %s %sEND@@'%(FILE, 7+1, 4)) 
        result = self.readMsg()
        self.assert_('BIKE_OK:' in result)

        sToWrite.send('@@BIKErenameByCoordinates %s %s %s %sEND@@'%(FILE, 1+1, 6, 'G')) 
        result = self.readMsg()
        self.assert_('BIKE_OK:' in result)

        self.sendKillMsg(sToWrite)
        

        while not hasattr(t, 'ended'):
            pass #wait until it receives the message and quits.

            
        sToRead.close()
        sToWrite.close()
        self.connToRead.close()

        
if __name__ == '__main__':
    unittest.main()

