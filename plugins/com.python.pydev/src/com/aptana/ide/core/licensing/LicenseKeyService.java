package com.aptana.ide.core.licensing;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

//import com.aptana.ide.core.AptanaCorePlugin;
//import com.aptana.ide.core.IdeLog;

/**
 * License key service.
 */
public class LicenseKeyService
{
	private final static String POINT_ID = "com.aptana.ide.core"; //$NON-NLS-1$
	private final static String EXT_ID = "licensing"; //$NON-NLS-1$
	private final static String CLASS = "class"; //$NON-NLS-1$
	public static final ILicenseKey EMPTY_KEY = new ClientKey(1, null, 0);
	private static ILicenseContributor provider = null;

	private static ILicenseKey clientKey;

	public final static ILicenseKey getLicenseKey()
	{
		/* No longer storing clientKey because it can be updated on the fly */

		final ILicenseContributor myProvider = getContributor();
		if (myProvider == null)
		{
			clientKey = EMPTY_KEY;
		}
		else
		{
			clientKey = myProvider.getLicenseKey();
		}

		return (clientKey != null) ? clientKey : EMPTY_KEY;

	}

	/**
	 * Creates a license key.
	 * 
	 * @param key
	 *            The license key string.
	 * @param email
	 *            The email/name to use.
	 * @return A new license key (in case a license provider cannot be loaded, an empty license is returned).
	 * @see #EMPTY_KEY
	 */
	public final static ILicenseKey createLicenseKey(String key, String email)
	{
		final ILicenseContributor myProvider = LicenseKeyService.getContributor();
		if (myProvider == null)
		{
			return EMPTY_KEY;
		}

		ILicenseKey clientKey = myProvider.createLicenseKey(key, email);
		return clientKey;
	}

	public final static ILicenseKey createTrialClientKey(String key, String email)
	{
		return new ClientKey(ILicenseKey.TRIAL, email, 1);
	}

	/**
	 * Returns true iff a license contributor can be loaded successfully.
	 * 
	 * @return True, if a license contributor can be loaded successfully.
	 */
	public final static boolean isLicenseContributorAvail()
	{
		ILicenseContributor cp = LicenseKeyService.getContributor();
		return (cp != null);
	}

	/**
	 * Returns the loaded {@link ILicenseContributor}.
	 * 
	 * @return An {@link ILicenseContributor} instance, null if failed or no such contributor was registered.
	 * @see #isLicenseContributorAvail()
	 */
	public final static ILicenseContributor getContributor()
	{
		if (provider != null)
		{
			return provider;
		}

		IConfigurationElement[] configurationElements = Platform.getExtensionRegistry().getConfigurationElementsFor(
				POINT_ID, EXT_ID);
		if (configurationElements != null && configurationElements.length < 1)
		{
			return null;
		}

		ILicenseContributor providerCandidate = null;
		for (IConfigurationElement element : configurationElements)
		{
			try
			{
				if (element.getNamespaceIdentifier().equals("com.aptana.ide.professional.licensing"))//$NON-NLS-1$
				{
					providerCandidate = (ILicenseContributor) element.createExecutableExtension(CLASS);
				}
				else
				{
					provider = (ILicenseContributor) element.createExecutableExtension(CLASS);
					break;
				}

			}
			catch (CoreException e)
			{
				org.python.pydev.core.log.Log.log( "Failed loading the license contributor"); //$NON-NLS-1$
			}
		}
		if (providerCandidate != null && provider == null)
		{
			provider = providerCandidate;
		}
		return provider;
	}
}
