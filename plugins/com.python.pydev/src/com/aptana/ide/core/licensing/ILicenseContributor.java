package com.aptana.ide.core.licensing;

import org.eclipse.swt.widgets.Composite;

/**
 * License contributor interface for any license scheme that is allowed to contribute a different way of license
 * validation.
 * 
 * @author Shalom G
 * @since Aptana Studio Andretti
 */
public interface ILicenseContributor
{
	/**
	 * Returns the license key that is stored in the preferences.
	 * 
	 * @return A license key.
	 */
	public ILicenseKey getLicenseKey();

	/**
	 * Creates a license key.
	 * 
	 * @param key
	 *            An encrypted license key string
	 * @param email
	 *            An email, or a user name
	 * @return ILicenseKey
	 */
	public ILicenseKey createLicenseKey(String key, String email);

	/**
	 * Creates the Key-area UI.
	 * 
	 * @param parent
	 *            A parent composite.
	 * @return The area in which the user will input the license key or code.
	 */
	//public LicenseKeyWidget createKeyComposite(Composite parent);

	/**
	 * Returns true if this contributor can generate trial licenses locally.
	 * 
	 * @return True, if the contributor can generate trials; false otherwise.
	 * @see #generateTrialKey(String)
	 */
	public boolean canGenerateTrials();

	/**
	 * Generates a trial key. The UUID passed to this method can be used for re-trial validation.
	 * 
	 * @param uuid
	 *            A unique ID that can be used to prevent re-trial.
	 * @return A trial key, or null, in case that the UUID validation blocked this process, or this contributor is not
	 *         designed to generate trial keys.
	 * @see #canGenerateTrials()
	 */
	public String generateTrialKey(String uuid);
}