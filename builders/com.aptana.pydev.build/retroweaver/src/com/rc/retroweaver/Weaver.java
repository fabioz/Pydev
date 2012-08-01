
package com.rc.retroweaver;

import java.io.*;
import java.util.*;

/**
 * Applies the RetroWeaver against a set of classes.
 *
 * @author Toby Reyelts
 *
 */
public class Weaver {

  // Read the new class file format spec for how the version is computed.
  public static final int VERSION_1_4 = 48;
  public static final int VERSION_1_3 = 47;
  public static final int VERSION_1_2 = 46;

  private static final String nl = System.getProperty( "line.separator" );

  public static void main( String[] args ) {

    String source = null;
    int version = VERSION_1_4;
    int currentArg = 0;
	  boolean lazy = false;
    String verifyPath = null;

    while ( currentArg < args.length ) {
      String command = args[ currentArg ];
      ++currentArg;

      if ( command.equals( "-source" ) ) {
        source = args[ currentArg++ ];
      }
      else if ( command.equals( "-version" ) ) {
        String verStr = args[ currentArg++ ];
        if ( verStr.equals( "1.4" ) ) {
          version = VERSION_1_4;
        }
        else if ( verStr.equals( "1.3" ) ) {
          version = VERSION_1_3;
        }
        else if ( verStr.equals( "1.2" ) ) {
          version = VERSION_1_2;
        }
        else {
          System.out.println( "Invalid target version: " + verStr );
          System.out.println();
          System.out.println( getUsage() );
          return;
        }
      }
      else if ( command.equals( "-lazy" ) ) {
        lazy = true;
      }
      else if ( command.equals( "-verifyrefs" ) ) {
        verifyPath = args[ currentArg++ ];
      }
      else {
        System.out.println( "I don't understand the command: " + command );
        System.out.println();
        System.out.println( getUsage() );
        return;
      }
    }

    if ( source == null ) {
      System.out.println( "Option \"-source\" is required." );
      System.out.println();
      System.out.println( getUsage() );
      return;
    }

    File sourcePath = new File( source );

    RetroWeaver weaver = new RetroWeaver( version );
    weaver.setLazy( lazy );

    try {
      invokeWeaver( weaver, sourcePath );
    }
    catch ( Exception e ) {
      throw new RuntimeException( "Weaving failed", e );
    }

    if ( verifyPath != null ) {
      List<String> paths = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer( verifyPath, File.pathSeparator );
      while ( st.hasMoreTokens() ) {
        paths.add( st.nextToken() );
      }
      RefVerifier rv = new RefVerifier( paths, new RefVerifier.Listener() {
        public void verifyStarted( String msg ) {
          System.out.println( "[RefVerifier] " + msg );
        }
        public void acceptWarning( String msg ) {
          System.out.println( "[RefVerifier] " + msg );
        }
      } );

      try {
        invokeVerifier( rv, sourcePath );
      }
      catch ( IOException e ) {
        throw new RuntimeException( "Verification failed", e );
      }
    }
  }

  private static String getUsage() {
    return "Usage: Weaver <options>" + nl +
           "  Options: " + nl +
           " -source <source dir> (required)" + nl +
           " -version <target VM version> (one of {1.4, 1.3, 1.2}, default is 1.4)" + nl +
           " -verifyrefs <classpath>";
  }

  public static void invokeWeaver( RetroWeaver w, File path ) throws Exception {

    FileFilter filter = new FileFilter() {
      public boolean accept( File f ) {
        return f.isDirectory() || f.getName().endsWith( ".class" );
      }
    };

    File[] files = path.listFiles( filter );

    if ( files == null ) {
      files = new File[] {};
    }

    for ( int i = 0; i < files.length; ++i ) {
      File f = files[ i ];
      if ( f.isDirectory() ) {
        invokeWeaver( w, f );
        continue;
      }
      String pathStr = f.getCanonicalPath();
      w.weave( pathStr, null );
    }
  }

  public static void invokeVerifier( RefVerifier rv, File path ) throws IOException {

    FileFilter filter = new FileFilter() {
      public boolean accept( File f ) {
        return f.isDirectory() || f.getName().endsWith( ".class" );
      }
    };

    File[] files = path.listFiles( filter );

    if ( files == null ) {
      files = new File[] {};
    }

    for ( int i = 0; i < files.length; ++i ) {
      File f = files[ i ];
      if ( f.isDirectory() ) {
        invokeVerifier( rv, f );
        continue;
      }
      String pathStr = f.getCanonicalPath();
      rv.verify( pathStr );
    }
  }

}

