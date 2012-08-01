
package com.rc.retroweaver;

import com.rc.retroweaver.runtime.*;

import java.io.*;
import java.util.*;
import jace.parser.*;
import jace.parser.method.*;
import jace.parser.field.*;
import jace.parser.constant.*;
import jace.metaclass.*;
import jace.autoproxy.*;

/**
 * Reads through a class file searching for references to classes, methods, or fields,
 * which don't exist on the specified classpath. This is primarily useful when trying to 
 * target one JDK while using the compiler for another.
 *
 * @author Toby Reyelts
 */
public class RefVerifier {

  List<String> classpath;
  String classfile;
  ClassFile file;
  ConstantPool pool;
  ClassSource classSource;
  Set<String> failedClasses;
  Listener listener;

  public static interface Listener {
    public void verifyStarted( String msg );
    public void acceptWarning( String msg );
  }

  private static final String nl = System.getProperty( "line.separator" );

  private static Set<String> primitiveTypes = new HashSet<String>();
  static {
    primitiveTypes.add( "java/lang/Boolean" );
    primitiveTypes.add( "java/lang/Byte" );
    primitiveTypes.add( "java/lang/Character" );
    primitiveTypes.add( "java/lang/Short" );
    primitiveTypes.add( "java/lang/Integer" );
    primitiveTypes.add( "java/lang/Float" );
    primitiveTypes.add( "java/lang/Long" );
    primitiveTypes.add( "java/lang/Double" );
  }

  public RefVerifier( List<String> classpath, Listener listener ) {
    this.classpath = classpath;
    classSource = new ClassSource( classpath );
    this.listener = listener;
  }

  public void verify( String classfile ) throws IOException {

    failedClasses = new HashSet<String>();
    this.classfile = classfile;

    file = new ClassFile( classfile );
    pool = file.getConstantPool();

    listener.verifyStarted( "Verifying " + classfile );

    MetaClassFactory mcf = new MetaClassFactory();

    // Search the constant pool looking for Class, MethodRef, InterfaceMethodRef, and FieldRef constants
    // This handles any class (including superclasses and interfaces), method, or field that has been referred to.

    nextConstant: for ( int i = 0; i < pool.getSize(); ++i ) {
      Constant c = pool.getConstant( i );

      if ( c instanceof ClassConstant ) {
        String className = c.toString();

        // System.out.println( "class name: " + className );
        // Don't bother with primitives or arrays of primitives
        MetaClass metaClass = mcf.getMetaClass( className, false, false );
        // System.out.println( "MetaClass: " + metaClass );
        if ( metaClass instanceof ArrayMetaClass ) {
          metaClass = ( ( ArrayMetaClass ) metaClass ).getBaseClass();
          // System.out.println( "Base MetaClass: " + metaClass );
          className = metaClass.getFullyQualifiedName( "/" );

          if ( metaClass.isPrimitive() ) {
            continue;
          }

          metaClass = mcf.getMetaClass( className, true, false );
          // System.out.println( "Base MetaClass 2: " + metaClass );
          className = metaClass.getFullyQualifiedName( "/" );
        }

        boolean couldBeFound = true;
        InputStream input = null;

        try {
          classSource.openClass( className );
        }
        catch ( NoClassDefFoundError e ) {
          couldBeFound = false;
        }
        finally {
          try {
            if ( input != null ) {
              input.close();
            }
          }
          catch ( IOException e ) {
          }
        }

        if ( ! couldBeFound ) {
          failedClasses.add( className );
          report( c );
          continue;
        }
      }
      else if ( c instanceof MethodRefConstant || c instanceof InterfaceMethodRefConstant ) {
        TypeInfo t = getTypeInfo( ( TypedConstant ) c );
        InputStream input = null;

        // Don't report a method error, about a class for which we've already shown an error
        if ( failedClasses.contains( t.ownerClass ) ) {
          continue;
        }

        String className = t.ownerClass;

        if ( mcf.getMetaClass( className, false, false ) instanceof ArrayMetaClass ) {
          // We just ignore methods called on arrays, because we know they must exist
          continue;
        }
       
        try {
          input = classSource.openClass( className );
          ClassFile cf = new ClassFile( input );
          Collection<ClassMethod> methods = cf.getMethods();
          for ( ClassMethod m : methods ) {
            if ( m.getName().equals( t.name ) && m.getDescriptor().equals( t.type ) ) {
              continue nextConstant;
            }
          }
        }
        catch ( NoClassDefFoundError e ) {
          report( c, "The parent class, " + className + ", could not be located." );
          continue;
        }
        finally {
          try {
            if ( input != null ) {
              input.close();
            }
          }
          catch ( IOException e ) {
          }
        }

        // For some reason, the compiler is specifying the method call to name() directly on the enum
        // subclass instead of on Enum. Frankly, I don't know whether this illegal or not. It's, at the least,
        // out of line with all of the rest of the compiler calls I've seen which all go directly to the subclass that
        // actually defines the method. For now, we're just going to glaze over this, but once we get an official
        // answer about what is allowed, I may have to add a lot more code to search through subclasses
        // as well as the current class.
        //
        if ( file.getSuperClassName().equals( Enum_.class.getName().replace( '.', '/' ) ) && t.name.equals( "name" ) ) {
          continue;
        }

        // When Retroweaver fixes up autoboxing calls, it doesn't remove the MethodRef constants that 
        // point to the 1.5 autoboxing from the constant pool. It doesn't hurt anything, but it does trigger
        // a false alarm here. It'd probably be nice to fix Retroweaver to actually remove/change those MethodRef
        // constants, but it's easier to just ignore the autoboxing methods here, for now.
        //
        if ( t.name.equals( "valueOf" ) && primitiveTypes.contains( t.ownerClass ) ) {
          continue;
        }

        report( c, "Method not found in " + className );
      }
      else if ( c instanceof FieldRefConstant ) {
        TypeInfo t = getTypeInfo( ( TypedConstant ) c );
        InputStream input = null;

        // Don't report a field error, about a class for which we've already shown an error
        if ( failedClasses.contains( t.ownerClass ) ) {
          continue;
        }

        try {
          input = classSource.openClass( t.ownerClass );
          ClassFile cf = new ClassFile( input );
          Collection<ClassField> fields = cf.getFields();
          for ( ClassField f : fields ) {
            if ( f.getName().equals( t.name ) && f.getDescriptor().equals( t.type ) ) {
              continue nextConstant;
            }
          }
        }
        catch ( NoClassDefFoundError e ) {
          report( c, "The parent class, " + t.ownerClass + ", could not be located." );
          continue;
        }
        finally {
          try {
            if ( input != null ) {
              input.close();
            }
          }
          catch ( IOException e ) {
          }
        }

        report( c, "Field not found in " + t.ownerClass );
      }
    }
  }

  private static class TypeInfo {
    String ownerClass;
    String name;
    String type;
  }

  public TypeInfo getTypeInfo( TypedConstant c ) {
    TypeInfo t = new TypeInfo();
    t.ownerClass = pool.getConstantAt( c.getClassIndex() ).toString();
    int index = c.getNameAndTypeIndex();
    NameAndTypeConstant ntc = ( NameAndTypeConstant ) pool.getConstantAt( index );
    t.name = pool.getConstantAt( ntc.getNameIndex() ).toString();
    t.type = pool.getConstantAt( ntc.getDescriptorIndex() ).toString();

    return t;
  }

  private void report( Constant c ) {
    report( c, null );
  }

  private void report( Constant c, String msg ) {

    if ( c instanceof ClassConstant ) {
      report( ( ClassConstant ) c, msg );
    }
    else if ( c instanceof TypedConstant ) {
      report( ( TypedConstant ) c, msg );
    }
    else {
      throw new RuntimeException( "Unexpected constant type" );
    }
  }

  private void report( ClassConstant c, String msg ) {
    String report = "While processing, " + classfile + ". Failed to find the class, " + c + ", on the class path";

    if ( msg != null ) {
      report += ": " + msg;
    }

    listener.acceptWarning( report );
  }

  private void report( TypedConstant c, String msg ) {

    TypeInfo t = getTypeInfo( c );
    String type = c instanceof FieldRefConstant ? "field" : "method";
    String report = "While processing, " + classfile + ". Failed to find the " + type + ", " + t.name + "/" + t.type;

    if ( msg != null ) {
      report += ", " + msg;
    }

    listener.acceptWarning( report );
  }

  public static String getUsage() {
    return "Usage: RefVerifier <options>" + nl +
           "  Options: " + nl +
           " -class <path to class to verify> (required) " + nl +
           " -cp <classpath containing valid classes> (required)";
  }

  public static void main( String[] args ) throws Exception {

    List<String> classpath = new ArrayList<String>();
    String classfile = null;

    for ( int i = 0; i < args.length; ++i ) {
      String command = args[ i ];
      ++i;

      if ( command.equals( "-class" ) ) {
        classfile = args[ i ];
      }
      else if ( command.equals( "-cp" ) ) {
        String path = args[ i ];
        StringTokenizer st = new StringTokenizer( path, File.pathSeparator );
        while ( st.hasMoreTokens() ) {
          classpath.add( st.nextToken() );
        }
      }
      else {
        System.out.println( "I don't understand the command: " + command );
        System.out.println();
        System.out.println( getUsage() );
        return;
      }
    }

    if ( classfile == null ) {
      System.out.println( "Option \"-class\" is required." );
      System.out.println();
      System.out.println( getUsage() );
      return;
    }

    RefVerifier vr = new RefVerifier( classpath, new Listener() {
      public void verifyStarted( String msg ) {
        System.out.println( "[RefVerifier] " + msg );
      }
      public void acceptWarning( String msg ) {
        System.out.println( "[RefVerifier] " + msg );
      }
    } );
    vr.verify( classfile );
  }
}


