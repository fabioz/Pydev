package com.python.pydev;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.bundle.BundleUtils;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.PyEdit;

import com.aptana.ide.core.licensing.ClientKey;
import com.aptana.ide.core.ui.preferences.ApplicationPreferences;
import com.aptana.ide.core.ui.preferences.IPreferenceConstants;
import com.python.pydev.license.ClientEncryption;
import com.python.pydev.util.PydevExtensionNotifier;

/**
 * The main plugin class to be used in the desktop.
 */
public class PydevPlugin extends AbstractUIPlugin {
    
    /**
     * This is an exception that indicates that the license is invalid
     */
    @SuppressWarnings("serial")
    private static class InvalidLicenseException extends RuntimeException{

        /**
         * Indicates whether the license was correctly decrypted or not -- if it was,
         * the message indicates why it wasn't accepted.
         */
        public boolean wasLicenseCorrectlyDecrypted;

        public InvalidLicenseException(String msg, boolean didLicenseDecrypt) {
            super(msg);
            this.wasLicenseCorrectlyDecrypted = didLicenseDecrypt;
        }
        
    }

    public static final String version = "REPLACE_VERSION";

    //The shared instance.
    private static PydevPlugin plugin;
    private PydevExtensionNotifier notifier;
    private boolean validated;
    public static final String ANNOTATIONS_CACHE_KEY = "MarkOccurrencesJob Annotations";
    public static final String OCCURRENCE_ANNOTATION_TYPE = "com.python.pydev.occurrences";
    
    /**
     * The constructor.
     */
    public PydevPlugin() {
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        
        if(!version.equals(org.python.pydev.plugin.PydevPlugin.version)){
            String msg = StringUtils.format("Error: the pydev com plugin version (%s) differs from the org plugin version (%s)", version, org.python.pydev.plugin.PydevPlugin.version);
            org.python.pydev.plugin.PydevPlugin.log(msg);
        }
        checkValid();
    }

    public boolean isValidated(){
        return validated;
    }
    
    public String checkValidStr() {
        String result = loadLicense();
        if(notifier == null){
            notifier = new PydevExtensionNotifier();
            notifier.start();
        }
        return result;
        
    }
    
    private boolean checkedValidOnce = false;
    public boolean checkValid() {
        if(!checkedValidOnce){
            checkValidStr();
            checkedValidOnce = true;
        }
        return validated;
    }

    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     */
    public static PydevPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path.
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin("com.python.pydev", path);
    }
    
    
    /**
     * @param license the license key
     * @param emailOrUsername the e-mail or the username to decrypt the license
     * @param licenseProvider "Aptana" or "Pydev"
     */
    public void saveLicense( String license, String emailOrUsername, String licenseProvider ) {    
        if(isAptanaLicenseProvider(licenseProvider)){
            //if it's an Aptana license, let's save it in the Aptana location too.
            ApplicationPreferences.getInstance().setString(IPreferenceConstants.ACTIVATION_EMAIL_ADDRESS, emailOrUsername);
            ApplicationPreferences.getInstance().setString(IPreferenceConstants.ACTIVATION_KEY, license);
        }
        
        getPreferenceStore().setValue(PydevExtensionInitializer.USER_EMAIL, emailOrUsername);
        
        Bundle bundle = Platform.getBundle("com.python.pydev");
        IPath path = Platform.getStateLocation( bundle );        
        path = path.addTrailingSeparator();
        path = path.append("license");
        try {
            FileOutputStream file = new FileOutputStream(path.toFile());
            file.write( license.getBytes() );
            file.close();
        } catch (FileNotFoundException e) {        
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }               
    }
    
    
    /**
     * @return null if no contents are available or a tuple with the key and the e-mail to be used as the license.
     */
    public Tuple<String, String> getLicenseKeyAndEmail(){
        Tuple<String, String> keyAndEmail = getLicenseKeyAndEmailFromPluginLocation();
        if(hasContentsForKeyAndEmail(keyAndEmail)){
            return keyAndEmail;
        }
        
        keyAndEmail = getLicenseKeyAndEmailFromPreferences();
        if(hasContentsForKeyAndEmail(keyAndEmail)){
            return keyAndEmail;
        }
        
        keyAndEmail = getLicenseKeyAndEmailFromAptana();
        if(hasContentsForKeyAndEmail(keyAndEmail)){
            return keyAndEmail;
        }

        return null; //no contents available
    }
    
    
    /**
     * Tries to load the license and set is as valid if it was actually validated (otherwise will set that the license
     * is not validated)
     * 
     * @return the string to be shown to the user if it was not able to validate (or null if it was properly validated)
     */
    private String loadLicense() {
        String returnMsg = "Could not find a suitable license.";
        
        // Tries to validate the license from a file in the plugin location.
        try {
            if(tryToValidateKeyAndEmail(getLicenseKeyAndEmailFromPluginLocation())){
                return null; 
            }
        } catch (Exception e) {
            //that's OK
        }

        
        // Tries to validate the license from the Pydev preferences.
        try {
            if(tryToValidateKeyAndEmail(getLicenseKeyAndEmailFromPreferences())){
                return null; 
            }
        } catch (Exception e) {
            returnMsg = e.getMessage();
        }
        
        
        // Tries to validate the license from the Aptana preferences.
        try {
            if(tryToValidateKeyAndEmail(getLicenseKeyAndEmailFromAptana())){
                return null; 
            }
        } catch (Exception e) {
            returnMsg = e.getMessage();
        }
        
        
        //if it got here, we were not able to validate!
        validated = false;
        return returnMsg;
    }

    /**
     * Tries to validate the license given the key and the e-mail (or username) to be used 
     * @param keyAndEmail the key and the e-mail (or username) to be used.
     * 
     * @return true if it was validated correctly and false otherwise.
     */
    private boolean tryToValidateKeyAndEmail(Tuple<String, String> keyAndEmail) {
        if(keyAndEmail.o1 != null && keyAndEmail.o2 != null && isLicenseValid(keyAndEmail.o1, keyAndEmail.o2, false)) {
            validated = true;
            return true;
        }
        return false;
    }
    
    /**
     * @return a tuple with the key and the e-mail that should eb used to decrypt it from the pydev preferences.
     */
    private Tuple<String, String> getLicenseKeyAndEmailFromPreferences() {
        Bundle bundle = Platform.getBundle("com.python.pydev");
        IPath path = Platform.getStateLocation( bundle );        
        path = path.addTrailingSeparator();
        path = path.append("license");
        String encLicense = null;
        String email = null;
        try {
            File f = path.toFile();
            if(!f.exists()){
                throw new FileNotFoundException("File not found.");
            }
            
            encLicense = REF.getFileContents(f);
            email = getPreferenceStore().getString(PydevExtensionInitializer.USER_EMAIL);
            return new Tuple<String, String>(encLicense, email);
        } catch (FileNotFoundException e) {
            String ret = "The license file: "+path.toOSString()+" was not found.";
            throw new RuntimeException(ret);
        }
    }

    
    /**
     * @return a tuple with the key and the e-mail that should eb used to decrypt it from the pydev preferences.
     */
    private Tuple<String, String> getLicenseKeyAndEmailFromAptana() {
        String email = ApplicationPreferences.getInstance().getString(IPreferenceConstants.ACTIVATION_EMAIL_ADDRESS);
        String key = ApplicationPreferences.getInstance().getString(IPreferenceConstants.ACTIVATION_KEY);
        return new Tuple<String, String>(email, key);
    }
    
    /**
     * @return a tuple with the key and the e-mail that should be used to decrypt it from a file in the plugin location.
     */
    private Tuple<String, String> getLicenseKeyAndEmailFromPluginLocation() {
        try{
            Location configurationLocation = Platform.getInstallLocation();
            URL url = configurationLocation.getURL();
            String path = url.getPath();
            File file = new File(path);
            if(file.exists()){
                file = new File(file, "features");
                if(file.exists()){
                    file = new File(file, "com.python.pydev");
                }
            }
            
            if(!file.exists()){
                //Let's give it a 2nd chance for finding it inside of the com.python.pydev plugin.
                file = BundleUtils.getRelative(new Path("/"), getBundle());
                file = new File(file.getParentFile(), "com.python.pydev");
            }
            
            File fileLicense = new File(file, "pydev_ext");
            File fileEmail = new File(file, "pydev_ext_email");
            
            if(fileLicense != null && fileEmail != null && fileLicense.exists() && fileEmail.exists()){
                String encLicense = REF.getFileContents(fileLicense).replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
                String enteredEmail = REF.getFileContents(fileEmail).trim();
                
                return new Tuple<String, String>(encLicense, enteredEmail);
            }
        }catch(Throwable e){
        }
        return null;
    }
    


    /**
     * @return true if the given tuple has enough information to be considered valid as a key license and the
     * related e-mail to use it.
     */
    private boolean hasContentsForKeyAndEmail(Tuple<String, String> keyAndEmail) {
        return keyAndEmail != null && keyAndEmail.o1 != null && keyAndEmail.o2 != null && 
            keyAndEmail.o1.trim().length() > 0 && keyAndEmail.o2.trim().length() > 0;
    }
    

    /**
     * @param hideDetails if true, it'll just show 'install validated for xxx' in the dialog (otherwise, the proper
     * key will be shown)
     * 
     * @return true if the given license is valid for the passed e-mail and false otherwise. 
     */
    private boolean isLicenseValid(String encLicense, String enteredEmail, boolean hideDetails) {
        try{
            //already decrypted
            IPreferenceStore prefsStore = getPreferenceStore();
            
            prefsStore.setValue(PydevExtensionInitializer.USER_NAME, "");
            prefsStore.setValue(PydevExtensionInitializer.LIC_TIME, "");
            prefsStore.setValue(PydevExtensionInitializer.LIC_TYPE, "");
            
            
            String license = ClientEncryption.getInstance().decrypt(encLicense);
            
            try {
                Properties properties = new Properties();
                properties.load(new ByteArrayInputStream(license.getBytes()));
                
                String eMail = (String) properties.remove("e-mail");
                String name  = (String) properties.remove("name");
                String time = (String) properties.remove("time");
                String licenseType = (String) properties.remove("licenseType");
                String devs = (String) properties.remove("devs");
                
                if(eMail == null || name == null || time == null || licenseType == null || devs == null || enteredEmail == null){
                    throw new InvalidLicenseException("The license is not correct, please re-paste it. If this error persists, please request a new license.", true);
                }
                if(!enteredEmail.equalsIgnoreCase(eMail)){
                    throw new InvalidLicenseException("The e-mail specified is different from the e-mail this license was generated for.", true);
                }
                
                
                Calendar currentCalendar = Calendar.getInstance();
                Calendar licenseCalendar = getExpTime(time);
                if(currentCalendar.after(licenseCalendar)){
                    throw new InvalidLicenseException("The current license expired at: "+formatDate(licenseCalendar), true);
                }
                
                
                setPrefsToShow(name, time, licenseType, devs, hideDetails, true);
                
            } catch (IOException e) {
                throw new InvalidLicenseException(e.getMessage(), false);
            }
            
            
        }catch(RuntimeException e){
            if(e instanceof InvalidLicenseException){
                InvalidLicenseException invalidLicenseException = (InvalidLicenseException) e;
                if(invalidLicenseException.wasLicenseCorrectlyDecrypted){
                    throw e; //if it did already decrypt, there's no sense in trying to decrypt it in some other way!
                }
                
            }
            
            //something went wrong when trying to decrypt it with the pydev license... let's go on and try to 
            //decrypt it with the Aptana license
            try {
                ClientKey clientKey = com.aptana.ide.professional.licensing.LicensingUtilities.decrypt(encLicense, enteredEmail);
                if(clientKey.getEmail() == null){
                    //leave same exception... no email is available
                }else{
                    if(clientKey.isValid()){
                        //note: number of clients hardcoded
                        String licenseType = "Aptana";
                        
                        if(clientKey.isTrial()){
                            licenseType += " trial";
                        }else{
                            licenseType += " pro";
                        }
                        if(!clientKey.isExpired()){
                            setPrefsToShow(clientKey.getEmail(), clientKey.getExpiration().getTimeInMillis()+"", licenseType, "1", hideDetails, false);
                            return true;
                        }else{
                            //change the exception to be thrown
                            e = new InvalidLicenseException(StringUtils.format("The current %s license expired at: %s", licenseType, formatDate(clientKey.getExpiration())), true);
                        }
                    }else{
                        //change the exception to be thrown
                        e = new InvalidLicenseException("The current license information is not valid", true);
                    }
                }
                
            } catch (Exception e1) {
                //something unexpected happened... just go on and throw the 'e' exception: it could've been already changed if it was an Aptana license.
            }

            throw e;
        }
        
        //if it got here, everything is ok...
        return true;
    }

    
    /**
     * Sets in the preferences the strings that should be shown to the user in the preferences dialog.
     */
    private void setPrefsToShow(String name, String time, String licenseType, String devs, boolean hideDetails, boolean isPydevLicense) {
        IPreferenceStore prefsStore = getPreferenceStore();
        prefsStore.setValue(PydevExtensionInitializer.USER_NAME, name);
        prefsStore.setValue(PydevExtensionInitializer.LIC_TIME, time);
        prefsStore.setValue(PydevExtensionInitializer.LIC_TYPE, licenseType);
        prefsStore.setValue(PydevExtensionInitializer.LIC_DEVS, devs);

        if(isPydevLicense){
            prefsStore.setValue(PydevExtensionInitializer.LIC_PROVIDER, "Pydev");
        }else{
            prefsStore.setValue(PydevExtensionInitializer.LIC_PROVIDER, "Aptana");
        }
        
        if(hideDetails){
            prefsStore.setValue(PydevExtensionInitializer.USER_EMAIL, "Install validated for: "+name);
            prefsStore.setValue(PydevExtensionInitializer.LICENSE, "Install validated for: "+name);
        }
    }

    
    /**
     * @param licenseProvider "Aptana", "Pydev" or null/empty -- which means Pydev
     * @return true if the licenseProvider registered is Aptana (and not Pydev)
     */
    public static boolean isAptanaLicenseProvider(String licenseProvider) {
        return !isPydevLicenseProvider(licenseProvider);
    }

    /**
     * @param licenseProvider "Aptana", "Pydev" or null/empty -- which means Pydev
     * @return true if the licenseProvider registered is Pydev (and not Aptana)
     */
    public static boolean isPydevLicenseProvider(String licenseProvider) {
        return licenseProvider == null || licenseProvider.trim().length() == 0 || !licenseProvider.equals("Aptana");
    }

    /**
     * @param date the date to be formatted to be shown to the user.
     * @return a String to be shown to the user with the given date
     */
    public static String formatDate(Calendar date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = format.format(new Date(date.getTimeInMillis()));
        return formattedDate;
    }

    
    /**
     * @return the list of occurrence annotations in the pyedit
     */
    @SuppressWarnings("unchecked")
    public static final List<Annotation> getOccurrenceAnnotationsInPyEdit(final PyEdit pyEdit) {
        List<Annotation> toRemove = new ArrayList<Annotation>();
        final Map<String, Object> cache = pyEdit.cache;
        
        if(cache == null){
            return toRemove;
        }
        
        List<Annotation> inEdit = (List<Annotation>) cache.get(ANNOTATIONS_CACHE_KEY);
        if(inEdit != null){
            Iterator<Annotation> annotationIterator = inEdit.iterator();
            while(annotationIterator.hasNext()){
                Annotation annotation = annotationIterator.next();
                if(annotation.getType().equals(OCCURRENCE_ANNOTATION_TYPE)){
                    toRemove.add(annotation);
                }
            }
        }
        return toRemove;
    }

    public static Calendar getExpTime(String time) {
        return getExpTime(Long.parseLong(time));
    }

    public static Calendar getExpTime(long lTime) {
        Calendar licenseCalendar = Calendar.getInstance();
        licenseCalendar.setTimeInMillis(lTime);
        licenseCalendar.add(Calendar.YEAR, 1);
        return licenseCalendar;
    }

}
