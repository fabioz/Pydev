'''
@author Fabio Zadrozny 
'''
import sys
import os

#make it as if we were executing from the directory above this one (so that we can use jycompletionserver
#without the need for it being in the pythonpath)
sys.argv[0] = os.path.dirname(sys.argv[0]) 
#twice the dirname to get the previous level from this file.
sys.path.insert(1, os.path.join(  os.path.dirname( sys.argv[0] )) )

import unittest
import jycompletionserver
import socket
import urllib


DEBUG = 0

def dbg(s):
    if DEBUG:
        print s

class Test(unittest.TestCase):

    def setUp(self):
        unittest.TestCase.setUp(self)

    def tearDown(self):
        unittest.TestCase.tearDown(self)
    
    def testIt(self):
        dbg( 'ok')
        
    def testMessage(self):
        t = jycompletionserver.T(0,0)
        
        l = []
        l.append(('Def','description'  , 'args'))
        l.append(('Def1','description1', 'args1'))
        l.append(('Def2','description2', 'args2'))
        
        msg = t.formatCompletionMessage(l)
        self.assertEquals('@@COMPLETIONS((Def,description,args),(Def1,description1,args1),(Def2,description2,args2))END@@', msg)
        
        l = []
        l.append(('Def','desc,,r,,i()ption',''  ))
        l.append(('Def(1','descriptio(n1',''))
        l.append(('De,f)2','de,s,c,ription2',''))
        msg = t.formatCompletionMessage(l)
        self.assertEquals('@@COMPLETIONS((Def,desc%2C%2Cr%2C%2Ci%28%29ption, ),(Def%281,descriptio%28n1, ),(De%2Cf%292,de%2Cs%2Cc%2Cription2, ))END@@', msg)






    def testCompletionSocketsAndMessages(self):
        dbg( 'testCompletionSocketsAndMessages')
        t, sToWrite, sToRead, self.connToRead, addr = self.createConnections()
        dbg( 'connections created')
        
        try:
            #now that we have the connections all set up, check the code completion messages.
            msg = urllib.quote_plus('math')

            toWrite = '@@IMPORTS:%sEND@@'%msg
            dbg( 'writing' + str(toWrite))
            sToWrite.send(toWrite) #math completions
            completions = self.readMsg()
            dbg( urllib.unquote_plus(completions))
            
            
            start = '@@COMPLETIONS(('
            self.assert_(completions.startswith(start), '%s DOESNT START WITH %s' % ( completions, start) )
    
            self.assert_(completions.find('@@COMPLETIONS') != -1)
            self.assert_(completions.find('END@@') != -1)

        
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




    def createConnections(self, p1 = 50002,p2 = 50003):
        '''
        Creates the connections needed for testing.
        '''
        t = jycompletionserver.T(p1,p2)
        
        t.start()

        sToWrite = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sToWrite.connect((jycompletionserver.HOST, p1))
        
        sToRead = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sToRead.bind((jycompletionserver.HOST, p2))
        sToRead.listen(1) #socket to receive messages.

        connToRead, addr = sToRead.accept()

        return t, sToWrite, sToRead, connToRead, addr
        

    def readMsg(self):
        msg = '@@PROCESSING_END@@'
        while msg.startswith('@@PROCESSING'):
            msg = self.connToRead.recv(1024*4)
            if msg.startswith('@@PROCESSING:'):
                dbg( 'Status msg:' + str(msg))

        return msg
        
    def sendKillMsg(self, socket):
        socket.send(pycompletionserver.MSG_KILL_SERVER)
        
    

    def testGettingInfoOnJython(self):
        from java.lang.reflect import Method
        from java.lang.reflect import Field
        from java.lang import Class
        from java.lang import System
        from java.lang import Object
        from java.lang.System import arraycopy
        from java.lang.System import out
        from java.io import OutputStream
        
        from org.python.core import PyReflectedFunction
        from org.python.core import PyReflectedField
        from org.python.core import PyReflectedConstructor
        
        a = 1
        
        from org.python import core
        from org.python.core import PyReflectedFunction
        
        def isclass(cls):
            return isinstance(cls, core.PyClass)
        
        def ismethod(func):
            if isinstance(func, core.PyFunction):
                print '    PyFunction'
                print dir(func.func_code.__class__)
                #
                return 1
                
            if isinstance(func, core.PyMethod):
                #things to play in func:
                #['__call__', '__class__', '__cmp__', '__delattr__', '__dir__', '__doc__', '__findattr__', '__name__', '_doget', 'im_class',
                #'im_func', 'im_self', 'toString']
                print '    PyMethod'
                #that's the PyReflectedFunction... keep going to get it
                func = func.im_func
        
            if isinstance(func, PyReflectedFunction):
                print '    PyReflectedFunction'
                print '    args'
                for i in range(len(func.argslist)):
                    #things to play in func.argslist[i]:
                        
                    #'PyArgsCall', 'PyArgsKeywordsCall', 'REPLACE', 'StandardCall', 'args', 'compare', 'compareTo', 'data', 'declaringClass'
                    #'flags', 'isStatic', 'matches', 'precedence']
                    
                    #print '        ', func.argslist[i].data.__class__
                    #func.argslist[i].data.__class__ == java.lang.reflect.Method
                    
                    params = ''
                    for param in func.argslist[i].data.getParameterTypes():
                        if len(params) != 0:
                            params = params + str(', ')
                        params = params + str(param)
                    print '        ', params
                return 1
            return 0
        
        def ismodule(mod):
            return isinstance(mod, core.PyModule)
        
        
        print '\n\n--------------------------- Method'
        assert not ismethod(Method)
        assert isclass(Method)
            
        print '\n\n--------------------------- System'
        assert not ismethod(System)
        assert isclass(System)
            
        print '\n\n--------------------------- arraycopy'
        assert ismethod(arraycopy)
        assert not isclass(arraycopy)
            
        print '\n\n--------------------------- out'
        assert not ismethod(out)
        assert not isclass(out)
            
        print '\n\n--------------------------- out.println'
        assert ismethod(out.println)
        assert not isclass(out.println)
        
        print '\n\n--------------------------- str'
        assert ismethod(str)
        assert not isclass(str)
        
        
        def met1():
            pass
        
        print '\n\n--------------------------- met1'
        assert ismethod(met1)
        assert not isclass(met1)
        









#"C:\Program Files\Java\jdk1.5.0_04\bin\java.exe" -Dpython.path="C:\bin\jython21\Lib";"C:\bin\jython21";"C:\Program Files\Java\jdk1.5.0_04\jre\lib\rt.jar" -classpath C:/bin/jython21/jython.jar org.python.util.jython D:\eclipse_workspace\org.python.pydev\PySrc\pycompletionserver.py 53795 58659
#
#"C:\Program Files\Java\jdk1.5.0_04\bin\java.exe" -Dpython.path="C:\bin\jython21\Lib";"C:\bin\jython21";"C:\Program Files\Java\jdk1.5.0_04\jre\lib\rt.jar" -classpath C:/bin/jython21/jython.jar org.python.util.jython D:\eclipse_workspace\org.python.pydev\PySrc\tests\test_jyserver.py
#
#"C:\Program Files\Java\jdk1.5.0_04\bin\java.exe" -Dpython.path="C:\bin\jython21\Lib";"C:\bin\jython21";"C:\Program Files\Java\jdk1.5.0_04\jre\lib\rt.jar" -classpath C:/bin/jython21/jython.jar org.python.util.jython d:\runtime-workbench-workspace\jython_test\src\test.py        
if __name__ == '__main__':
    unittest.main()

