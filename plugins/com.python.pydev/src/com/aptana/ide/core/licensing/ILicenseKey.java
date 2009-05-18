package com.aptana.ide.core.licensing;

import java.util.Calendar;

public interface ILicenseKey
{

	public static final int PRO = 0;
	public static final int TRIAL = 1;

	/**
	 * True if this key is close to expiring
	 * 
	 * @return true if close, false otherwise
	 */
	public boolean isCloseToExpiring();

	/**
	 * True if valid key
	 * 
	 * @return true if valid, false otherwise
	 */
	public boolean isValid();

	/**
	 * True if key has valid fields but email did not match
	 * 
	 * @return - true if close, false otherwise
	 */
	public boolean isCloseToMatching();

	/**
	 * True if this key is expired
	 * 
	 * @return true if expired, false otherwise
	 */
	public boolean isExpired();

	/**
	 * Gets the email for this key
	 * 
	 * @return - email
	 */
	public String getEmail();

	/**
	 * Gets the expiration of this key
	 * 
	 * @return - calendar
	 */
	public Calendar getExpiration();

	/**
	 * True if trial key
	 * 
	 * @return true if trial, false otherwise
	 */
	public boolean isTrial();

	/**
	 * True if pro key
	 * 
	 * @return true if pro, false otherwise
	 */
	public boolean isPro();

	/**
	 * True if professional plugins should run
	 * 
	 * @return true to run pros, false otherwise
	 */
	public boolean shouldProPluginsRun();

}