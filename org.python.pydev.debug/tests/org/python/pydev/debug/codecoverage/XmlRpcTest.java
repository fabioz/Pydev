package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.SocketUtil;
import org.python.pydev.runners.SimplePythonRunner;

public class XmlRpcTest extends TestCase{

    String[] EXPECTED = new String[]{
            "false",
            "false",
            "10",
            "false",
            "false",
            "false",
            "false",
            "true",
            "false",
            "true",
            "false",
            "true",
            "false",
            "20",
            "30",
            "false",
            "false",
            "false",
            "false",
            "start get completions",
            "foo",
            "3|4",
            "end get completions",
            "start raw_input",
            "false",
            "true",
            "finish raw_input",
            "'foo'",
            "false",
            "false",
    };
    
    private int next = -1;
    
    public static void main(String[] args) throws MalformedURLException, XmlRpcException {
        junit.textui.TestRunner.run(XmlRpcTest.class);
    }
    
    private Process startServer(int port, boolean python) throws IOException {
        File f = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"pysrc/pydevconsole.py");
        
        String cmdLine;
        if(python){
            cmdLine = SimplePythonRunner.getCommandLineAsString(
                    new String[]{TestDependent.PYTHON_EXE, "-u", REF.getFileAbsolutePath(f), ""+port});
        }else{
            cmdLine = SimplePythonRunner.getCommandLineAsString(
                    new String[]{TestDependent.JAVA_LOCATION, "-classpath",
                            TestDependent.JYTHON_JAR_LOCATION, "org.python.util.jython",
                            REF.getFileAbsolutePath(f), ""+port});
        }
        
        Process process = Runtime.getRuntime().exec(cmdLine);
        process.getErrorStream().close();
        return process;
    }
    
    public void testXmlRpcServerPython() throws XmlRpcException, IOException, InterruptedException {
        checkServer(true);
    }
    
    public void testXmlRpcServerJython() throws XmlRpcException, IOException, InterruptedException {
        checkServer(false);
    }
    
    public void checkServer(boolean python) throws XmlRpcException, IOException, InterruptedException {
        int port = SocketUtil.findUnusedLocalPort();
        Process process = startServer(port, python);
        
        //give some time for the process to start
        if(!python){
            synchronized (this) {
                this.wait(2000);
            }
        }else{
            synchronized (this) {
                this.wait(1000);
            }
        }
        
        
        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL("http://127.0.0.1:"+port));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            
            print(client.execute("addExec", new Object[]{"abc = 10"}));
            print(client.execute("addExec", new Object[]{"abc"}));
            print(client.execute("addExec", new Object[]{"import sys"}));
            print(client.execute("addExec", new Object[]{"class Foo:"}));
            print(client.execute("addExec", new Object[]{"    print 20"}));
            print(client.execute("addExec", new Object[]{"    print >> sys.stderr, 30"}));
            print(client.execute("addExec", new Object[]{""}));
            print(client.execute("addExec", new Object[]{"foo=Foo()"}));
//            print(client.execute("addExec", new Object[]{"foo.__doc__=None"}));
            print("start get completions");
            print(client.execute("getCompletions", new Object[]{"fo"}));
            print("end get completions");
            
            print("start raw_input");
            print(client.execute("addExec", new Object[]{"raw_input()"}));
            print("finish raw_input");
            print(client.execute("addExec", new Object[]{"foo"}));
        } finally {
            process.destroy();
        }
        assertEquals(next, EXPECTED.length-1);
    }

    private void print(Object execute) {
        if(execute instanceof Object[]){
            Object[] objects = (Object[]) execute;
            for(Object o:objects){
                print(o);
            }
        }else{
//            System.out.println(execute);
            String s = ""+execute;
            if(s.length() > 0){
                String expected = EXPECTED[nextExpected()].trim();
                String found = s.trim();
                if(!expected.equals(found)){
                    if(expected.equals("false")){
                        expected = "0";
                    }
                    if(expected.equals("true")){
                        expected = "1";
                    }
                    if(expected.equals("3|4")){
                        if(found.equals("3") || found.equals("4")){
                            return;
                        }
                    }
                    String errorMessage = StringUtils.format("Expected: >>%s<< and not: >>%s<< (position:%s)", 
                            expected, found, next);
                    assertEquals(errorMessage, found, expected);
                }
            }
        }
    }

    private int nextExpected() {
        next += 1;
        return next;
    }
    
}
