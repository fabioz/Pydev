package com.python.pydev.ui;

import com.aptana.ide.core.ui.preferences.ApplicationPreferences;
import com.aptana.ide.core.ui.preferences.IPreferenceConstants;
import com.python.pydev.PydevPlugin;

import junit.framework.TestCase;

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
        
        
        //valid license
        String validAptana = "" +
        		"--begin-aptana-license--" +
        		"10465823113475051543783567" +
        		"91540800829455099504387745" +
        		"87823149652493795939048273" +
        		"42699848888392311428955832" +
        		"13258739566850730919698017" +
        		"96672194726139161693761588" +
        		"48837436903613544035374426" +
        		"31090631617826789465965549" +
        		"27256288329425218289335717" +
        		"58716786920917605249449235" +
        		"36750516426478224703496554" +
        		"17481608184332090735265" +
        		"--end-aptana-license--" +
        		"";
        
        plugin.saveLicense(validAptana, "asasaki", "Aptana");
        plugin.checkValidStr();
        assertTrue(plugin.checkValid());
        
        //let's see if it saved for Aptana...
        assertEquals("asasaki", ApplicationPreferences.getInstance().getString(IPreferenceConstants.ACTIVATION_EMAIL_ADDRESS));
        assertEquals(validAptana, ApplicationPreferences.getInstance().getString(IPreferenceConstants.ACTIVATION_KEY));
        
        
        //expired license
        String expiredAptana = "22983901328960548200186726172752019755391927862273541221817531927825394897692490551866553975351369337755911292721220479872921968921016134817557812147304787606042260500911113707788278292805637156469732836924511378606211568124640990571941177079604439414795160995140097715439323455782203014983181370468416294241";
        plugin.saveLicense(expiredAptana, "joellelam2", "Aptana");
        plugin.checkValidStr();
        assertTrue(!plugin.checkValid());
        assertEquals("joellelam2", ApplicationPreferences.getInstance().getString(IPreferenceConstants.ACTIVATION_EMAIL_ADDRESS));
        assertEquals(expiredAptana, ApplicationPreferences.getInstance().getString(IPreferenceConstants.ACTIVATION_KEY));
        
    }

}
