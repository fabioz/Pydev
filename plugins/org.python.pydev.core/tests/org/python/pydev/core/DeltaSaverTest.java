/*
 * Created on 12/10/2005
 */
package org.python.pydev.core;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.callbacks.ICallback;

import junit.framework.TestCase;

public class DeltaSaverTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DeltaSaverTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }
    

    protected void tearDown() throws Exception {
        super.tearDown();
        new DeltaSaver<Object>(new File("."), "deltatest", getCallBack()).clearAll(); //leave no traces
    }

    private ICallback<Object, ObjectInputStream> getCallBack() {
        return new ICallback<Object, ObjectInputStream>(){

            public Object call(ObjectInputStream arg) {
                try {
                    return arg.readObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }};
    }

    public static class DeltaProcessor implements IDeltaProcessor<String>{

        public List<String> state = new ArrayList<String>();
        
        public int processed;

        public void processUpdate(String data) {
            throw new RuntimeException("should not be called");
        }

        public void processDelete(String data) {
            processed+=1;
            state.remove(data);
        }

        public void processInsert(String data) {
            processed+=1;
            state.add((String) data);
        }

        public void endProcessing() {
        }
        
    }

    public void testSaveRestore() throws Exception {
        DeltaSaver<String> saver = new DeltaSaver<String>(new File("."), "deltatest", getCallBack());
        saver.addInsertCommand("ins1");
        saver.addInsertCommand("ins2");
        saver.addDeleteCommand("ins1");
        
        DeltaSaver<String> restorer = new DeltaSaver<String>(new File("."), "deltatest", getCallBack());
        assertEquals(3, restorer.availableDeltas());
        DeltaProcessor deltaProcessor = new DeltaProcessor();
        restorer.processDeltas(deltaProcessor);
        assertEquals(3, deltaProcessor.processed);
        assertEquals(1, deltaProcessor.state.size());
        assertEquals("ins2", deltaProcessor.state.get(0));

        restorer = new DeltaSaver<String>(new File("."), "deltatest", getCallBack());
        assertEquals(0, restorer.availableDeltas());
        
    }
    

    
    
    public static class InsertDeltaProcessor implements IDeltaProcessor<Integer>{

        public List<String> state = new ArrayList<String>();
        
        public int processed;

        public void processUpdate(Integer data) {
            throw new RuntimeException("should not be called");
        }

        public void processDelete(Integer data) {
            throw new RuntimeException("should not be called");
        }

        public void processInsert(Integer data) {
            assertEquals((Object)processed, (Object)data);
            processed+=1;
        }

        public void endProcessing() {
        }
        
    }

    public void testSaveRestore3() throws Exception {
        //check if the order is correct
        DeltaSaver<Integer> saver = new DeltaSaver<Integer>(new File("."), "deltatest", getCallBack());
        for (int i = 0; i < 50; i++) {
            saver.addInsertCommand(i);
        }
        DeltaSaver<Integer> restorer = new DeltaSaver<Integer>(new File("."), "deltatest", getCallBack());
        assertEquals(50, restorer.availableDeltas());
        restorer.processDeltas(new InsertDeltaProcessor());
    }
    
}
