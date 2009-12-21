
package com.rc.retroweaver.ant;

import com.rc.retroweaver.*;
import com.rc.retroweaver.event.*;
import java.io.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

import java.util.*;

/**
 * An Ant task for running RetroWeaver on a set of class files.
 *
 * @author Gunnar Grim
 * @author Toby Reyelts
 *
 */
public class RetroWeaverTask extends Task {

	////////////////////////////////////////////////////////////////////////////////
	//	Constants and variables.
	
 	/**
	 * The destination directory for processd classes, or <code>null</code> for in place
	 * processing.
	 */
	private File itsDestDir;
	
	/**
	 * Indicates if an error should cause the script to fail. Default to <code>true</code>.
	 */
	private boolean itsFailOnError = true;
	
	/**
	* The set of files to be weaved.
	*/
	private List<FileSet> itsFileSets = new ArrayList<FileSet>();
 
	/**
	 * Indicates if classes should only be processed if their current version differ from the target version. Initially <code>true</code>.
	 */
	private boolean itsLazy = true;
	
	/**
	 * Indicates if each processed class should be logged. Initially set to <code>false</code>.
	 */
	private boolean itsVerbose = false;

  /**
   * The directory to the JDK being targetted.
   *
   */
  private String refClassPath;

	/**
	 * The class file version number.
	 */
	private int itsVersion = 48;
	
	/**
	 * The class file version number.
	 */
	private static final Map<String, Integer> itsVersionMap = new HashMap<String, Integer>();
	
	/**
	 * Initialize the version map.
	 */
	static {
		itsVersionMap.put( "1.2", 46 );
		itsVersionMap.put( "1.3", 47 );
		itsVersionMap.put( "1.4", 48 );
		itsVersionMap.put( "1.5", 49 );
	}
	
	////////////////////////////////////////////////////////////////////////////////
	//	Constructors.
	
	/**
	 * Construct a new RetroWeaver task.
	 * @since 4.0.1
	 * @changed 4.0.1
	 */
	public RetroWeaverTask()
	{
	}
	
	////////////////////////////////////////////////////////////////////////////////
	//	Property accessors and mutators.
	
 	/**
	 * Set the destination directory for processed classes. Unless specified the classes
	 * are processed in place.
	 * @param pDir The destination directory. 
	 */
	public void setDestDir( File pDir ) {
		if (!pDir.isDirectory())
			throw new BuildException("The destination directory doesn't exist: "+pDir, getLocation());
		
		itsDestDir = pDir;
	}

  /**
   * Specify if an error should cause the script to fail. Default to <code>true</code>.
   *
   * @param pFailOnError <code>true</code> to fail, <code>false</code> to keep going.
   */
   public void setFailOnError(boolean pFailOnError) {
     itsFailOnError = pFailOnError;
   }

	/**
   * Add a set of files to be weaved.
   * @param pSet The fileset.
   */
	public void addFileSet(FileSet pFileSet) {
		itsFileSets.add(pFileSet);
	}

 	/**
	 * Specify if classes should only be processed if their current version differ from the target version. Initially <code>true</code>.
	 * @param pLazy <code>true</code> for lazy processing.
	 */
	public void setLazy(boolean pLazy) {
		itsLazy = pLazy;
	}

 	/**
	 * Set the source directory containing classes to process. This is a shortcut to
	 * using an embedded fileset with the specified base directory and which includes
	 * all class files.
	 * @param pDir The directory. 
	 */
	public void setSrcDir(File pDir) {
		FileSet fileSet = new FileSet();
		fileSet.setDir(pDir);
		fileSet.setIncludes("**/*.class");
		
		addFileSet(fileSet);
	}

 	/**
	 * Specify if each processed class should be logged. Initially set to <code>false</code>.
	 * @param pVerbose <code>true</code> for verbose processing.
	 */
	public void setVerbose(boolean pVerbose) {
		itsVerbose = pVerbose;
	}

 	/**
	 * Set the target class file version. Initially set to "1.4".
	 * @param pVersion The JDK version, e&nbsp;g "1.3". 
	 */
	public void setVersion(String pVersion) {
		Integer v = itsVersionMap.get(pVersion);
		if (v == null)
			throw new BuildException("Unknown version: "+pVersion, getLocation());
		itsVersion = v;
	}

  /**
   * Turns on reference verification using the specified classpath.
   * Retroweaver will report any references to fields/methods/classes which don't appear
   * on refClassPath.
   *
   */
  public void setVerifyRefs( String refClassPath ) {
    this.refClassPath = refClassPath;
  }

	////////////////////////////////////////////////////////////////////////////////
	//	Operations.
	
	/**
	 * Run the RetroWeaver task.
	 * @throws BuildException If a build exception occurs.
	 */
	public void execute() throws BuildException {

		//	Check arguments.
		
		if ( itsFileSets.size() == 0 )
			throw new BuildException( "Either attribute 'srcdir' must be used or atleast one fileset must be embedded.", getLocation() );

		//	Create and configure the weaver.
		
		RetroWeaver weaver = new RetroWeaver( itsVersion );
		weaver.setLazy(itsLazy);
	
		//	Set up a listener if the verbose option is true.
		if ( itsVerbose ) {
			weaver.setListener(new WeaveListener() {
        public void weavingPath( String pPath ) {
          getProject().log( RetroWeaverTask.this, "Weaving "+ pPath, Project.MSG_INFO );
        }
      });
    } 
    else {
			weaver.setListener(null);
		}
			
		//	Weave the files in the filesets.
		Set<String> weaved = new HashSet<String>();
		
		try {
			//	Process each fileset.
			for ( FileSet fileSet : itsFileSets ) {
				//	Create a directory scanner for the fileset.
				File baseDir = fileSet.getDir(getProject());
				DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
				//	Process each file.
				for (String fileName : scanner.getIncludedFiles()) {
					//	Set up the source and output paths.
					File file = new File(baseDir, fileName);
					String sourcePath = file.getCanonicalPath();
					String outputPath = null;
					if (itsDestDir != null)
						outputPath = new File(itsDestDir, fileName).getCanonicalPath();
					//	Weave it unless already weaved.
					if (!weaved.contains(sourcePath)) {
						weaver.weave(sourcePath, outputPath);
						weaved.add(sourcePath);
					}
				}
			}
		} 
    catch (Throwable ex) {
			if ( itsFailOnError )
				throw new BuildException( ex, getLocation() );
			else
				getProject().log( this, ex.toString(), Project.MSG_WARN );
		}

    // Put in the code to call the verifier from here
    if ( refClassPath != null ) {

      List<String> refPath = new ArrayList<String>();

      StringTokenizer st = new StringTokenizer( refClassPath, File.pathSeparator );
      while ( st.hasMoreTokens() ) {
        refPath.add( st.nextToken() );
      }

      RefVerifier rv = new RefVerifier( refPath, new RefVerifier.Listener() {
        public void verifyStarted( String msg ) {
          // getProject().log( RetroWeaverTask.this, msg, Project.MSG_INFO );
        }
        public void acceptWarning( String msg ) {
          getProject().log( RetroWeaverTask.this, msg, Project.MSG_WARN );
        }
      } );

      //	Process each fileset.
      for ( FileSet fileSet : itsFileSets ) {
        //	Create a directory scanner for the fileset.
        File baseDir = fileSet.getDir(getProject());
        DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
        //	Process each file.
        for (String fileName : scanner.getIncludedFiles()) {
          //	Set up the source and output paths.
          File file = new File(baseDir, fileName);
          try {
            String sourcePath = file.getCanonicalPath();
            rv.verify( sourcePath );
          }
          catch (Throwable ex) {
    				throw new BuildException( ex, getLocation() );
          }
        }
      }
    }
  }
}

