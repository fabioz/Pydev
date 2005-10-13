/*
 * Created on 12/10/2005
 */
package org.python.pydev.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class DeltaSaverTest extends TestCase {

    private DeltaSaver saver;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DeltaSaverTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        createDeltaSaver().clearAll(); //leave no traces
    }

    private DeltaSaver createDeltaSaver() {
        return new DeltaSaver(new File("."), "deltatest");
    }
    
    public static class DeltaProcessor implements IDeltaProcessor{

        public List<String> state = new ArrayList<String>();
        
        public int processed;

        public void processUpdate(Object data) {
            throw new RuntimeException("should not be called");
        }

        public void processDelete(Object data) {
            processed+=1;
            state.remove(data);
        }

        public void processInsert(Object data) {
            processed+=1;
            state.add((String) data);
        }

        public void endProcessing() {
        }
        
    }

    public void testSaveRestore() throws Exception {
        saver = createDeltaSaver();
        saver.addCommand(new DeltaSaver.DeltaInsertCommand("ins1"));
        saver.addCommand(new DeltaSaver.DeltaInsertCommand("ins2"));
        saver.addCommand(new DeltaSaver.DeltaDeleteCommand("ins1"));
        
        DeltaSaver restorer = createDeltaSaver();
        assertEquals(3, restorer.availableDeltas());
        DeltaProcessor deltaProcessor = new DeltaProcessor();
        restorer.processDeltas(deltaProcessor);
        assertEquals(3, deltaProcessor.processed);
        assertEquals(1, deltaProcessor.state.size());
        assertEquals("ins2", deltaProcessor.state.get(0));

        restorer = createDeltaSaver();
        assertEquals(0, restorer.availableDeltas());
        
    }
    
    
    public void testSaveRestore2() throws Exception {
        saver = createDeltaSaver();
        saver.addInsertCommand("ins1");
        saver.addInsertCommand("ins2");
        saver.addDeleteCommand("ins1");
        
        DeltaSaver restorer = createDeltaSaver();
        assertEquals(3, restorer.availableDeltas());
        restorer.clearAll();
        
        restorer = createDeltaSaver();
        assertEquals(0, restorer.availableDeltas());
    }

    
    
    public static class InsertDeltaProcessor implements IDeltaProcessor{

        public List<String> state = new ArrayList<String>();
        
        public int processed;

        public void processUpdate(Object data) {
            throw new RuntimeException("should not be called");
        }

        public void processDelete(Object data) {
            throw new RuntimeException("should not be called");
        }

        public void processInsert(Object data) {
            assertEquals(processed, data);
            processed+=1;
        }

        public void endProcessing() {
        }
        
    }

    public void testSaveRestore3() throws Exception {
        //check if the order is correct
        saver = createDeltaSaver();
        for (int i = 0; i < 50; i++) {
            saver.addInsertCommand(i);
        }
        DeltaSaver restorer = createDeltaSaver();
        assertEquals(50, restorer.availableDeltas());
        restorer.processDeltas(new InsertDeltaProcessor());
    }
    
}
