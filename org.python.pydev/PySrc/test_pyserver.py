'''
@author Fabio Zadrozny 
'''

import unittest
import pycompletionserver
import socket

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
        self.assertEquals('@@COMPLETIONS((Def,description),(Def1,description1),(Def2,description2))END@@', msg)

    def testSocketsAndMessages(self):
        t = pycompletionserver.T(50002,50003)
        
        t.start()

        sToWrite = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sToWrite.connect((pycompletionserver.HOST, 50002))
        
        sToRead = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sToRead.bind((pycompletionserver.HOST, 50003))
        sToRead.listen(1) #socket to receive messages.

        connToRead, addr = sToRead.accept()
        
        
        #now that we have the connections all set up, check the code completion messages.
        sToWrite.send('@@GLOBALS:import math\nEND@@') #only 1 global should be returned: math itself.
        completions = connToRead.recv(1024)
        self.assertEquals('@@COMPLETIONS((math,This module is always available.  It provides access to the\n'\
                          'mathematical functions defined by the C standard.))END@@',
                          completions)

        
        #check token msg.
        sToWrite.send('@@TOKEN_GLOBALS(math):import math\nEND@@') 
        completions = connToRead.recv(4086)

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
        self.a                    
                                
        pass            
'''     

        sToWrite.send('@@TOKEN_GLOBALS(C):%s\nEND@@'%s) 
        completions = connToRead.recv(4086)

        self.sendKillMsg(sToWrite)
        

        while not hasattr(t, 'ended'):
            pass #wait until it receives the message and quits.

            
        sToRead.close()
        sToWrite.close()
        
    def sendKillMsg(self, socket):
        socket.send(pycompletionserver.MSG_KILL_SERVER)
        
    
if __name__ == '__main__':
    unittest.main()
