/* Copied by atotic from Eclipse's bugzilla. The bug talked about opening external files */

package org.python.pydev.plugin;

/*
 * (c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;

/**
 *
 * @see IStorage
 */
public class FileStorage extends PlatformObject implements IStorage {
   IPath path;
   InputStream in = null;

   /**
    * Two FileStorages are equal if their IPaths are equal.
    * @see java.lang.Object#equals(java.lang.Object)
    */
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (!(obj instanceof FileStorage))
         return false;
      FileStorage other = (FileStorage)obj;
      return path.equals(other.path);
   }

   /**
    * @see org.eclipse.core.resources.IStorage#getContents()
    */
   public InputStream getContents() throws CoreException {
      if (in == null) {
         try {
            return new FileInputStream(path.toFile());
         } catch (FileNotFoundException e) {
            throw new CoreException(new Status(IStatus.ERROR, "FileStorage unknown string",
                  IStatus.ERROR, e.toString(), e));
         }
      } else {
         return in;
      }
   }

   /**
    * @see IStorage#getFullPath
    */
   public IPath getFullPath() {
      return this.path;
   }

   /**
    * @see IStorage#getName
    */
   public String getName() {
      return this.path.lastSegment();
   }

   /**
    * @see IStorage#isReadOnly()
    */
   public boolean isReadOnly() {
      return true;
   }

   /**
    * Method FileStorage.
    * @param path
    */
   public FileStorage(IPath path){
      this.path = path;
   }

   /**
    * Method FileStorage.
    * @param in
    * @param path
    */
   public FileStorage(InputStream in, IPath path){
      this.path = path;
      this.in = in;
   }

   /**
    * @see IStorage#isReadOnly()
    */
   public String toString() {
      return path.toOSString();
   }
}
