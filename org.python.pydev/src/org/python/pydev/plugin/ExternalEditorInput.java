/* Copied by atotic from Eclipse's bugzilla. The bug talked about opening external files */

package org.python.pydev.plugin;

/*
 * (c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 */
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.core.resources.IStorage;

import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PlatformUI;


/**
 * An EditorInput for an external file.
 */
public class ExternalEditorInput implements IStorageEditorInput {

   IStorage externalFile;

   /**
    * Two ExternalEditorInputs are equal if their IStorage's are equal.
    * @see java.lang.Object#equals(java.lang.Object)
    */
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (!(obj instanceof ExternalEditorInput))
         return false;
      ExternalEditorInput other = (ExternalEditorInput)obj;
      return externalFile.equals(other.externalFile);
   }

   /*
   * @see IEditorInput#exists()
   */
   public boolean exists() {
      // External file ca not be deleted
      return true;
   }

   /*
   * @see IAdaptable#getAdapter(Class)
   */
   public Object getAdapter(Class adapter) {
      return null;
   }

   /*
   * @see IEditorInput#getContentType()
   */
   public String getContentType() {
      return externalFile.getFullPath().getFileExtension();
   }

   /*
   * @see IEditorInput#getFullPath()
   */
   public String getFullPath() {
      return externalFile.getFullPath().toString();
   }

   /*
   * @see IEditorInput#getImageDescriptor()
   */
   public ImageDescriptor getImageDescriptor() {
      IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
      return registry.getImageDescriptor(externalFile.getFullPath().getFileExtension());
   }

   /*
   * @see IEditorInput#getName()
   */
   public String getName() {
      return externalFile.getName();
   }

   /*
   * @see IEditorInput#getPersistable()
   */
   public IPersistableElement getPersistable() {
      return null;
   }

   /*
   * see IStorageEditorInput#getStorage()
   */
   public IStorage getStorage() {
      return externalFile;
   }

   /*
   * @see IEditorInput#getToolTipText()
   */
   public String getToolTipText() {
      return externalFile.getFullPath().toString();
   }

   public ExternalEditorInput(IStorage exFile) {
      externalFile = exFile;
   }
}
