'''
@author Fabio Zadrozny 
'''
import threading
import time
import simpleTipper

HOST = '127.0.0.1'               # Symbolic name meaning the local host


MSG_KILL_SERVER     = '@@KILL_SERVER_END@@'
MSG_COMPLETIONS     = '@@COMPLETIONS'
MSG_END             = 'END@@'
MSG_GLOBALS         = '@@GLOBALS:'
MSG_TOKEN_GLOBALS   = '@@TOKEN_GLOBALS('
MSG_INVALID_REQUEST = '@@INVALID_REQUEST'

BUFFER_SIZE = 1024

class T(threading.Thread):

    def __init__(self, thisPort, serverPort):
        threading.Thread.__init__(self)
        self.thisPort   = thisPort
        self.serverPort = serverPort
        self.socket = None #socket to send messages.
        

    def connectToServer(self):
        import socket
        
        self.socket = s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((HOST, self.serverPort))

    def removeInvalidChars(self, msg):
        return msg.replace(',','').replace('(','').replace(')','')
    
    def formatCompletionMessage(self, completionsList):
        '''
        Format the completions suggestions in the following format:
        @@COMPLETIONS((token,description),(token,description),(token,description))END@@
        '''
        compMsg = ''
        for tup in completionsList:
            if compMsg != '':
                compMsg += ','
                
            compMsg += '(%s,%s)' % (self.removeInvalidChars(tup[0]),self.removeInvalidChars(tup[1]))
            
        return '%s(%s)%s'%(MSG_COMPLETIONS, compMsg, MSG_END)
    
    def sendCompletionsMessage(self, completionsList):
        '''
        Send message with completions.
        '''
        self.socket.send(self.formatCompletionMessage(completionsList))
    
    def sendReceivedInvalidMessage(self):
        self.socket.send(MSG_INVALID_REQUEST)
    
    def getTokenAndData(self, data):
        '''
        When we receive this, we have 'token):data'
        '''
        token = ''
        for c in data:
            if c != ')':
                token += c
            else:
                break;
        
        return token, data.lstrip(token+'):')

    
    def run(self):
        # Echo server program
        import socket
        
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind((HOST, self.thisPort))
        s.listen(1) #socket to receive messages.
        

        #we stay here until we are connected.
        #we only accept 1 client. 
        #the exit message for the server is @@KILL_SERVER_END@@
        conn, addr = s.accept()

        #after being connected, create a socket as a client.
        self.connectToServer()
        
#        print 'pycompletionserver Connected by', addr
        
        
        while 1:
            data = ''
            while not data.endswith(MSG_END):
                data += conn.recv(BUFFER_SIZE)
            
            if MSG_KILL_SERVER in data:
                #break if we received kill message.
                break;
            else:
                data = data.rstrip(MSG_END)
            
            if MSG_GLOBALS in data:
                data = data.replace(MSG_GLOBALS, '')
                comps = simpleTipper.GenerateTip(data, None)
                self.sendCompletionsMessage(comps)
            
            elif MSG_TOKEN_GLOBALS in data:
                data = data.replace(MSG_TOKEN_GLOBALS, '')
                token, data = self.getTokenAndData(data)                
                comps = simpleTipper.GenerateTip(data, token)
                self.sendCompletionsMessage(comps)

            else:
                self.sendReceivedInvalidMessage()
            
            
            conn.send(data)
            
        conn.close()
        self.ended = True

if __name__ == '__main__':
    
    import sys
    thisPort = int(sys.argv[1])  #this is from where we want to receive messages.
    serverPort = int(sys.argv[2])#this is where we want to write messages.
    
    t = T(thisPort, serverPort)
    t.start()

