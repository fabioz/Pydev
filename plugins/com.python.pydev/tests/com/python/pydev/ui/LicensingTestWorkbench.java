package com.python.pydev.ui;

import junit.framework.TestCase;

import com.python.pydev.PydevPlugin;

public class LicensingTestWorkbench extends TestCase{
    
    public void testAptanaLicense() throws Exception {
        //valid license -- until 2019
        String validPydev = "72548747308550511547436577260069369375851257654872299745337321804983375038022009"+
                            "74257143888697080180753393467701400840631193626491307465771343000651052324052841"+
                            "48828689091367760603120068729224572385210462575110235806999557565341910142117103"+
                            "42574137683053701881252715257237500319875874655218188297999753796971@10855025638"+
                            "51343154793792448857883243450584923516525117384186377312686432957194743383107295"+
                            "48220028263196657242092657116337462818635364453948528241216591973779804981787642"+
                            "91772332918190611052811335118376310079121401946587381153514760944485656877764876"+
                            "6412913566149255681771107358352955153686631820417110617925@";
        
        PydevPlugin plugin = PydevPlugin.getDefault();
        plugin.saveLicense(validPydev, "valid_pydev", "Pydev");
        plugin.checkValidStr();
        assertTrue(plugin.checkValid());

        //invalid license
        String invalidPydev = "333333333333333";
        plugin.saveLicense(invalidPydev, "valid_pydev", "Pydev");
        plugin.checkValidStr();
        assertTrue(!plugin.checkValid());
        
    }

}
