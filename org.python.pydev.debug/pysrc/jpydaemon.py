#! /usr/bin/env python

""" this daemon may be used by 'external' python sollicitors interfaces"""

import sys
import bdb 
import socket
import string
import traceback
import os
import inspect
import types

from inspector import *

HOST = '' 
PORT = 29000 # default listening port
OK   = "OK"

COMMAND = 0
SET_BP  = 2
DEBUG   = 31
STEP    = 4
NEXT    = 5
RUN     = 6
FREEZE  = 7 # remain on current line 
CLEAR_BP  = 8
STACK   = 9
QUIT    = 10
LOCALS  = 11
GLOBALS = 12
SETARGS = 13
READSRC = 14
UNKNOWN = -1

# instanciate a jpyutil object
utils = jpyutils() 

class JPyDbg(bdb.Bdb) :
    
    
    def __init__(self):
      bdb.Bdb.__init__(self)
      # store debugger script name to avoid debugging it's own frame 
      self.debuggerFName = os.path.normcase(sys.argv[0])
      print self.debuggerFName
      # client debugger connection
      self.connection = None
      # frame debuggee contexts
      self.globalContext = None
      self.localContext = None 
      self.verbose = 0
      # hide is used to prevent debug inside debugger
      self.hide = 0
      # debugger active information
      self.debuggee = None
      self.cmd = UNKNOWN
      # net buffer content
      self.lastBuffer = ""
      # EXCEPTION raised flag
      self.exceptionRaised = 0
      # debuggee current 'command line arguments'
      self.debuggeeArgs = None
      # last executed line exception or None
      self.lastLineException = None
      
    def populateToClient( self , bufferList ) :
      buffer = '<JPY>'   
      for element in bufferList:
        buffer = buffer + ' ' + str(element)
      buffer = buffer + '</JPY>\n'
      #print buffer
      self.connection.send( buffer )
    
    # bdb overwitten to capture call debug event  
    def user_call(self, frame, args):
      name = frame.f_code.co_name
      if not name: name = '???'
      fn = self.canonic(frame.f_code.co_filename)
      if not fn: fn = '???'
      # discard debugger frame 
      if fn == self.debuggerFName or self.hide:
          self.hide = self.hide + 1
      if self.hide:     
          return None
      self.populateToClient( [ '<CALL',
                               'cmd="'+str(self.cmd)+'"' , 
                               'fn="'+ utils.removeForXml(fn) +'"' ,
                               'name="'+name+'"',
                               'args="'+str(args)+'"' ,
                               '/>' ]
                           )
      
      
    def checkDbgAction( self , frame ):
      if ( self.cmd == DEBUG )  or ( self.cmd == STEP ) or ( self.cmd == NEXT ) or ( self.cmd == RUN ):
        # DEBUG STARTING event  
        # Debuggin starts stop on first line wait for NEXT , STEP , RUN , STOP ....  
        while ( self.parseSubCommand( self.receiveCommand() , frame ) == FREEZE ):
          pass
        
        
    # bdb overwitten to capture line debug event  
    def user_line(self, frame):
      if self.hide:
        return None 
      import linecache
      name = frame.f_code.co_name
      if not name: name = '???'
      fn = self.canonic(frame.f_code.co_filename)
      if not fn: fn = '???'
      # populate info to client side
      line = linecache.getline(fn, frame.f_lineno)
      self.populateToClient( [ '<LINE',
                               'cmd="'+str(self.cmd)+'"' , 
                               'fn="'+ utils.removeForXml(fn)+'"' ,
                               'lineno="'+str(frame.f_lineno)+'"' ,
                               'name="' + name + '"' ,
                               'line="' + utils.removeForXml(line.strip())+'"',
                               '/>'] )
      # what's on next
      self.checkDbgAction( frame ) 
        
    # bdb overwitten to capture return debug event  
    def user_return(self, frame, retval):
        fn = self.canonic(frame.f_code.co_filename)
        if not fn: fn = '???'
        if self.hide:
          self.hide = self.hide - 1
          return None  
        self.populateToClient( [  '<RETURN',
                                  'cmd="'+str(self.cmd)+'"' , 
                                  'fn="'+utils.removeForXml(fn)+'"' ,
                                  'retval="'+str(retval)+'"' ,
                                  '/>'] )
    
    def send_client_exception( self , cmd , content ):
      self.populateToClient( ['<EXCEPTION',
                               'cmd="'+cmd+'"' , 
                               'content="'+content+'"' ,
                              '/>'] ) 
      
                                  
    def populate_exception( self , exc_stuff):
        if ( self.exceptionRaised == 0 ): # exception not yet processed
          extype  = exc_stuff[0]
          details = exc_stuff[1]
          ex = exc_stuff
          # Deal With SystemExit in specific way to reflect debuggee's return
          if issubclass( extype , SystemExit):
            content = 'System Exit REQUESTED BY DEBUGGEE with code =' + ex.code
          elif issubclass(extype, SyntaxError):  
            content = str(details)
            error = details[0]
            compd = details[1]
            content = 'SOURCE:SYNTAXERROR:"'+str(compd[0])+'":('+str(compd[1])+','+str(compd[2])+')'+':'+error
          elif issubclass(extype,NameError):
            content = 'SOURCE:NAMEERROR:'+str(details)
          elif issubclass(extype,ImportError):
            content = 'SOURCE::IMPORTERROR:'+str(details)
          else:
            content = str(details)
          # keep track of received exception
          self.lastLineException = ['<EXCEPTION',
                                  'cmd="'+str(self.cmd)+'"' , 
                                  'content="'+utils.removeForXml(content)+'"' ,
                                  '/>']
          self.send_client_exception( str(self.cmd) , utils.removeForXml(content) )
          self.exceptionRaised = 1 # set ExceptionFlag On 
            
        
    # bdb overwitten to capture Exception events  
    def user_exception(self, frame, exc_stuff):
      # capture next / step go ahead when exception is around 
      # current steatement while steping
      if self.cmd==NEXT or self.cmd==STEP:
        # self.populate_exception( exc_stuff )
        self.set_step()
        sys.settrace(self.trace_dispatch)
      else:   
        self.populate_exception( exc_stuff )
        self.set_continue()
  
    def parsedReturned( self , command = 'COMMAND' , argument = None , message = None , details = None ):
      parsedCommand = []
      parsedCommand.append(command)
      parsedCommand.append(argument)
      parsedCommand.append(message)
      parsedCommand.append(details)
      return parsedCommand

    # acting as stdio => redirect to client side 
    def write( self , toPrint ):
      # transform eol pattern   
      if ( toPrint == "\n" ):
        toPrint = "/EOL/"
      self.populateToClient( ['<STDOUT' , 'content="'+utils.removeForXml(toPrint)+'"' , '/>' ] )

    def buildEvalArguments( self , arg ):
      posEqual = arg.find('=')
      if posEqual == -1:
        return None,None # Syntax error on provided expession couple
      return arg[:posEqual].strip() , arg[posEqual+1:].strip()

    #
    # parse & execute buffer command 
    #
    def dealWithCmd( self , 
                     verb , 
                     arg , 
                     myGlobals = globals() , 
                     myLocals = locals() 
                   ):
      cmd = COMMAND
      msgOK = OK
      cmdType = "single"
      silent , silentarg = self.commandSyntax( arg )
      if silent == 'silent':
        arg = silentarg # consume
        # "exec" is the magic way which makes 
        # used debuggees dictionaries updatable while 
        # stopped in debugging hooks
        cmdType = "exec"  
        msgOK = silent
      # we use ';' as a python multiline syntaxic separator 
      arg = string.replace(arg,';','\n')
      # execute requested dynamic command on this side
      try:
        oldstd = sys.stdout
        sys.stdout=self
        code = compile( arg ,"<string>" , cmdType)  
        exec code in myGlobals , myLocals
        sys.stdout=oldstd
        return utils.parsedReturned( argument = arg , message = msgOK ) 
      except:
        try: 
          return utils.populateCMDException(arg,oldstd)
        except:
          tb , exctype , value = sys.exc_info()
          excTrace = traceback.format_exception( tb , exctype , value )
          print excTrace
          
    #
    # build an xml CDATA structure
    # usage of plus is for jpysource.py xml CDATA encapsulation of itself
    #
    def CDATAForXml( self , data ):
      return '<'+'![CDATA['+ data + ']'+']>'
      
    #
    # parse & execute buffer command
    #
    def dealWithRead( self , verb , arg ):
      cmd = READSRC
      # check python code and send back any found syntax error
      if arg == None:
        return utils.parsedReturned( message = "JPyDaemon ReadSrc Argument missing")
      try:
        arg , lineno = self.nextArg(arg)  
        candidate = file(arg) 
        myBuffer = utils.parsedReturned( argument = arg , message=OK )
        # 
        # append the python source in <FILEREAD> TAG
        myBuffer.append( ['<FILEREAD' ,
                          'fn="'+arg+'"' ,
                          'lineno="'+str(lineno)+'">' +
                          self.CDATAForXml(candidate.read()) +
                          '</FILEREAD>' ] )
        return myBuffer
      except IOError, e:
        return utils.parsedReturned( argument = arg , message = e.strerror )
    #
    # parse & execute buffer command
    #
    def dealWithSetArgs( self , arg ):
      cmd = SETARGS
      # populate given command line argument before debugging start
      if arg == None:
        self.debuggeeArgs = [] # nor args provided
      else:  
        self.debuggeeArgs = string.split(arg)
      self.debuggeeArgs.insert(0,'') # keep program name empty for the moment
      sys.argv = self.debuggeeArgs # store new argument list ins sys argv
      return utils.parsedReturned( argument = arg , message = OK ) 

    # load the candidate source to debug
    # Run under debugger control 
    def dealWithDebug( self , verb , arg ):
      self.cmd = DEBUG
      if self.debuggee == None:
        result = "source not found : " + arg
        for dirname in sys.path:
          fullname = os.path.join(dirname,arg)
          if os.path.exists(fullname):
            # Insert script directory in front of module search path
            # and make it current path (#sourceforge REQID 88108 fix)
            debugPath = os.path.dirname(fullname)
            sys.path.insert(0, debugPath)
            os.chdir(debugPath)
            oldstd = sys.stdout
            sys.stdout=self
            self.debuggee = fullname
            sys.argv[0] = fullname # keep sys.argv in sync
            try:
              self.run('execfile(' + `fullname` + ')')
            # send a dedicated message for syntax error in order for the
            # frontend debugger to handle a specific message and display the involved line
            # inside the frontend editor
            except:
              tb , exctype , value = sys.exc_info()
              excTrace = traceback.format_exception( tb , exctype , value )
              # self.populateException(excTrace)
              self.send_client_exception(str(self.cmd) , utils.removeForXml(str(excTrace)))
              #print excTrace
              pass
              
            sys.stdout=oldstd
            result ="OK"
            self.debuggee = None 
            break 
      else:
        result = "debug already in progress on : " + self.debuggee   
      return utils.parsedReturned( command = 'DEBUG' , argument = arg , message = result ) 
    
    def formatStackElement( self , element ):
        curCode = element[0].f_code
        fName = curCode.co_filename
        line  =  element[1]
        if ( fName == '<string>' ):
          return ("program entry point")
        return utils.removeForXml(fName + ' (' + str(line) + ') ')
    
    # populate current stack info to client side 
    def dealWithStack( self , frame ):
      stackList , size = self.get_stack ( frame , None )
      stackList.reverse() 
      xmlStack = ['<STACKLIST>' ] 
      for stackElement in stackList:
        xmlStack.append('<STACK')
        xmlStack.append('content="'+ self.formatStackElement(stackElement) +'"')
        xmlStack.append( '/>')
      xmlStack.append('</STACKLIST>') 
      self.populateToClient( xmlStack )
      
    # populate requested disctionary to client side
    def dealWithVariables( self , frame , type , stackIndex  ):
      # get the stack frame first   
      stackList , size = self.get_stack ( frame , None )
      stackList.reverse() 
      stackElement = stackList[int(stackIndex)]
      if ( type == 'GLOBALS' ):
        variables = stackElement[0].f_globals
      else:
        variables = stackElement[0].f_locals
      xmlVariables = ['<VARIABLES type="'+type+'">' ]
      for mapElement in variables.iteritems():
        xmlVariables.append('<VARIABLE ')
        xmlVariables.append('name="'+utils.removeForXml(mapElement[0])+'" ')
        xmlVariables.append('content="'+utils.removeForXml(str(mapElement[1]))+'" ')
        xmlVariables.append( '/>')
      xmlVariables.append('</VARIABLES>') 
      self.populateToClient( xmlVariables )
    
    def variablesSubCommand( self , frame , verb , arg , cmd ):
      self.cmd = cmd
      if ( arg == None ):
        arg = "0"  
      else:    
        arg , optarg = self.nextArg(arg) # split BP arguments  
      self.dealWithVariables( frame , verb , arg )
      self.cmd = FREEZE 
    
    def nextArg( self , toParse ):
      nextSpace = string.strip(toParse).find(" ")           
      if ( nextSpace == -1 ):
        return string.strip(toParse) , None
      else:
        return string.strip(toParse[:nextSpace]) , string.strip(toParse[nextSpace+1:])
    
    # rough command/subcommand syntax analyzer    
    def commandSyntax( self , command ):
      self.cmd  = UNKNOWN
      verb , arg  = self.nextArg(command)
      return verb , arg  
    
    
    def quiting( self ):
      self.populateToClient( ['<TERMINATE/>'] )
      self.set_quit()

    def parseSingleCommand( self , command ):
      verb , arg = self.commandSyntax( command )
      if ( string.upper(verb) == "CMD" ):
        return self.dealWithCmd( verb , arg )
      if ( string.upper(verb) == "READSRC" ):
        return self.dealWithRead( verb , arg )
      if ( string.upper(verb) == "SETARGS" ):
        return self.dealWithSetArgs( arg )
      elif ( string.upper(verb) == "DBG" ):
        return self.dealWithDebug( verb, arg )
      elif ( string.upper(verb) == "STOP" ):
        return None
      else:
        return utils.parsedReturned( message = "JPyDaemon SYNTAX ERROR : " + command ) 
        
    # receive a command when in debugging state using debuggee's frame local and global
    # contexts
    def parseSubCommand( self , command , frame ):
      if ( command == None ): # in case of IP socket Failures
        return UNKNOWN
      verb , arg = self.commandSyntax( command )
      if ( string.upper(verb) == "CMD" ):
        self.populateCommandToClient( command ,
                                      self.dealWithCmd( verb ,
                                                        arg ,
                                                        myGlobals= frame.f_globals ,
                                                        myLocals = frame.f_locals
                                                      )
                                      )
        self.cmd = FREEZE

      elif ( string.upper(verb) == "READSRC" ):
        self.populateCommandToClient( command ,
                                      self.dealWithRead( verb , arg )
                                    )
        self.cmd = FREEZE
        
      elif ( string.upper(verb) == "NEXT" ):
        self.cmd = NEXT
        self.set_next(frame)
      elif ( string.upper(verb) == "STEP" ):
        self.cmd = STEP
        self.set_step()
      elif ( string.upper(verb) == "RUN" ):
        self.cmd = RUN
        self.set_continue()
      elif ( string.upper(verb) == "STOP"):
        self.cmd = QUIT  
        self.quiting()
      elif ( string.upper(verb) == "BP+"):
        self.cmd = SET_BP
        arg , optarg = self.nextArg(arg) # split BP arguments  
        self.set_break( arg , int(optarg) )
        self.cmd = FREEZE 
      elif ( string.upper(verb) == "STACK"):
        self.cmd = STACK
        self.dealWithStack(frame)
        self.cmd = FREEZE 
      elif ( string.upper(verb) == "LOCALS"):
        self.variablesSubCommand( frame , verb , arg , LOCALS )
      elif ( string.upper(verb) == "GLOBALS"):
        self.variablesSubCommand( frame , verb , arg , GLOBALS )
      elif ( string.upper(verb) == "BP-"):
        self.cmd = CLEAR_BP
        arg , optarg = self.nextArg(arg) # split BP arguments  
        self.clear_break( arg , int(optarg) )
        self.cmd = FREEZE 
      return self.cmd       
      
    # send command result back 
    def populateCommandToClient( self , command , result ):
      self.populateToClient( [ '<' + result[0] , 
                               'cmd="' +command+'"' ,
                               'operation="' +utils.removeForXml(str(result[1]))+'"' ,
                               'result="' +str(result[2])+'"' ,
                               '/>' ] )
      if ( result[3] != None ):
        for element in result[3]:
#         print strElement
          self.populateToClient( [ '<COMMANDDETAIL ' ,
                                   'content="'+utils.removeForXml(element)+'"',
                                   ' />'
                                  ]
                                )
      # complementary TAG may be provided starting at position 4
      if len(result) > 4 and (result[4]!=None):
        self.populateToClient( result[4] )
      
    # check and execute a received command
    def parseCommand( self , command ):
      # IP exception populating None object  
      if ( command == None ):
        return 0 # => stop daemon
    
      if ( self.verbose ):   
        print command
      result = self.parseSingleCommand(command)
      if ( result == None ):
        self.populateToClient( ['<TERMINATE/>'] )
        return 0 # stop requested
      self.populateCommandToClient( command , result )
      return 1
    
    # reading on network 
    def readNetBuffer( self ):
      try:
        if ( self.lastBuffer.find('\n') != -1 ):
          return self.lastBuffer ; # buffer stills contains commands
        networkData = self.connection.recv(1024)
        if not networkData:  # capture network interuptions if any
          return None
        data = self.lastBuffer + networkData
        return data
      except socket.error, (errno,strerror):
        print "recv interupted errno(%s) : %s" % ( errno , strerror )
        return None
          
    
    # receive a command from the net 
    def receiveCommand( self ):
      data = self.readNetBuffer() ;
      # data reception from Ip
      while ( data != None and data):
        eocPos = data.find('\n')
        nextPos = eocPos ;
        while (  nextPos < len(data) and \
                 ( data[nextPos] == '\n' or data[nextPos] == '\r') ): # ignore consecutive \n\r
          nextPos = nextPos+1     
        if ( eocPos != -1 ): # full command received in buffer
          self.lastBuffer = data[nextPos:] # cleanup received command from buffer
          returned = data[:eocPos]
          if (returned[-1] == '\r'):
            return returned[:-1]
          return returned  
        data = self.readNetBuffer() ; 
      # returning None on Ip Exception
      return None 

    # start the deamon 
    def start( self , port = PORT , host = None , debuggee = None ,debuggeeArgs = None ):
        if ( host == None ):
          # start in listen mode waiting for incoming sollicitors   
          print "JPyDbg listening on " , port 
          s = socket.socket( socket.AF_INET , socket.SOCK_STREAM )
          s.bind( (HOST , port) )
          s.listen(1)
          self.connection , addr = s.accept()
          print "connected by " , addr
        else:
          # connect back provided listening host
          print "JPyDbg connecting " , host , " on port " , port 
          try:   
            self.connection = socket.socket( socket.AF_INET , socket.SOCK_STREAM )
            self.connection.connect( (host , port) )
            print "JPyDbgI0001 : connected to " , host
          except socket.error, (errno,strerror):
            print "ERROR:JPyDbg connection failed errno(%s) : %s" % ( errno , strerror )
            return None
        welcome = [ '<WELCOME/>' ]
        # populate debuggee's name for remote debugging bootstrap
        if debuggee != None:
          welcome = [ '<WELCOME' ,  
                      'debuggee="'+utils.removeForXml(debuggee)]
          if debuggeeArgs != None:
            welcome.append(string.join(debuggeeArgs))
            # populate arguments after program Name
          # finally append XML closure  
          welcome.append('" />')
          
        self.populateToClient( welcome )
        while ( self.parseCommand( self.receiveCommand() ) ):
          pass    
        print "'+++ JPy/sessionended/"
        self.connection.close()
        

# start a listening instance when invoked as main program
# without arguments
# when [host [port]] are provided as argv jpydamon will try to
# connect back host port instead of listening
if __name__ == "__main__":
    instance = JPyDbg()
    port = PORT
    host = None
    localDebuggee = None
    args = None
    print "args = " , sys.argv
    
    if ( len(sys.argv) > 1 ): host=sys.argv[1]
    if ( len(sys.argv) > 2 ): port=int(sys.argv[2])
    if ( len(sys.argv) > 3 ): localDebuggee=sys.argv[3]
    if ( len(sys.argv) > 4 ): args=sys.argv[4:]
    instance.start( host=host , 
                    port=port , 
                    debuggee=localDebuggee ,
                    debuggeeArgs=args
                  )
    print "deamon ended\n"