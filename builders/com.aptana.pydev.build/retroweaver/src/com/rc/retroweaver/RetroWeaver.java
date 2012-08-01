
package com.rc.retroweaver;

import com.rc.retroweaver.event.*;

import jace.parser.*;
import jace.parser.constant.*;
import jace.parser.method.*;
import jace.parser.field.*;
import jace.parser.attribute.*;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.*;

import java.io.*;
import java.util.*;

/**
 * A bytecode enhancer that translates Java 1.5 class files into Java 1.4 class
 * files. The enhancer performs primarily two tasks:
 *
 * 1) Reverses changes made to the class file format in 1.5 to the 
 * former 1.4 format.
 *
 * 2) Replaces compiler generated calls into the new 1.5 runtime with calls
 * into RetroWeaver's replacement runtime.
 *
 * @author Toby Reyelts
 *
 */
public class RetroWeaver {

  private ClassParser parser;
  private JavaClass javaClass;
  private ClassGen generator;
  private String className;
  private ConstantPoolGen constantPool;

  private String sourcePath;
  private String outputPath;

  private int newVersion;

  private boolean lazy;

  private WeaveListener listener = new WeaveListener() {
    public void weavingPath( String sourcePath ) {
      System.out.println( "[RetroWeaver] Weaving " + sourcePath );
    }
  };

  private static final String newLine = System.getProperty( "line.separator" );
  private static Map<Type,String> boxTypes = new HashMap<Type,String>();

  private static final String AUTOBOX_CLASS = "com.rc.retroweaver.runtime.Autobox";
  private static final String VALUE_OF_METHOD = "valueOf";
  private static final String CLASS_LITERAL_CLASS = "com.rc.retroweaver.runtime.ClassLiteral";
  private static final String GET_CLASS_METHOD = "getClass";
  private static final String GET_CLASS_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/Class;";

  static {
    boxTypes.put( Type.getType( Boolean.class ), "(Z)Ljava/lang/Boolean;" );
    boxTypes.put( Type.getType( Byte.class ), "(B)Ljava/lang/Byte;" );
    boxTypes.put( Type.getType( Character.class ), "(C)Ljava/lang/Character;" );
    boxTypes.put( Type.getType( Short.class ), "(S)Ljava/lang/Short;" );
    boxTypes.put( Type.getType( Integer.class ), "(I)Ljava/lang/Integer;" );
    boxTypes.put( Type.getType( Long.class ), "(J)Ljava/lang/Long;" );
    boxTypes.put( Type.getType( Float.class ), "(F)Ljava/lang/Float;" );
    boxTypes.put( Type.getType( Double.class ), "(D)Ljava/lang/Double;" );
  }

  public RetroWeaver( int version ) {
    newVersion = version;
  }

  public void weave( String sourcePath, String outputPath ) throws IOException {

    this.sourcePath = sourcePath;

    if ( outputPath == null ) {
      outputPath = sourcePath;
    }

    this.outputPath = outputPath;

    if ( ! fixupFormat() ) {
 
		  // We're lazy and the class already has the target version.
  		File sf = new File( sourcePath );
  		File of = new File( outputPath );

  		if ( ! of.isFile() || ! of.getCanonicalPath().equals( sf.getCanonicalPath() ) ) {
			  // Target doesn't exist or is different from source so copy the file and transfer utime.
  			FileInputStream fis = new FileInputStream( sf );
  			byte[] bytes = new byte[ ( int ) sf.length() ];
  			fis.read( bytes );
  			fis.close();
  			FileOutputStream fos = new FileOutputStream( of );
  			fos.write( bytes );
  			fos.close();
  			of.setLastModified( sf.lastModified() );
  		}

    	return;
	  }

    if ( listener != null ) {
      listener.weavingPath( sourcePath );
    }

    // System.out.println( "BCEL Parsing file" );

    // We read from the outputPath, because that is where the new fixed up class file lives
    parser = new ClassParser( this.outputPath );
    javaClass = parser.parse();

    generator = new ClassGen( javaClass );
    className = generator.getClassName();
    constantPool = generator.getConstantPool();

    if ( alreadyAspected() ) {
      return;
    }

    // System.out.println( "Fixing up runtime" );
    fixupRuntimeCalls();

    // System.out.println( "Dumping class" );
    generator.getJavaClass().dump( this.outputPath );
  }

  /**
   * Replaces '+' characters with '$' characters in the identifier string.
   * (See http://forum.java.sun.com/thread.jsp?forum=316&thread=499645 for more info on "+"s
   * and identifiers).
   *
   * Replaces references to StringBuilder with StringBuffer - StringBuilder is 1.5 only.
   * I believe the compiler doesn't use StringBuilder in a way that is incompatible with
   * StringBuffer - this has yet to be confirmed by Neal, though. The ultra-safe way of
   * doing this is to create a compatible clone of StringBuilder for 1.4.
   *
   * @param index - a constant pool index to a UTF8Constant.
   */
  private int updateId( jace.parser.ConstantPool pool, int index ) {
    UTF8Constant constant = ( UTF8Constant ) pool.getConstantAt( index );
    String name = constant.toString();
    String newName = name;

    // System.out.println( "considering: " + name );

    if ( name.indexOf( '+' ) != -1 ) {
      // System.out.println( "Replacing '+'" );
      newName = name.replace( '+', '$' );
    }

    if ( newName.indexOf( "java/lang/StringBuilder" ) != -1 ) {
      // System.out.println( "Replacing StringBuilder." );
      newName = newName.replace( "java/lang/StringBuilder", "java/lang/StringBuffer" );
    }

    // Change all references from java.lang.Iterable to our own Iterable_ class
    // which is signature compatible with Iterable.
    //
    if ( newName.indexOf( "java/lang/Iterable" ) != -1 ) {
      // System.out.println( "Replacing Iterable." );
      newName = newName.replace( "java/lang/Iterable", "com/rc/retroweaver/runtime/Iterable_" );
    }

    // Change all references from java.lang.Enum to our own Enum_ class
    // which is signature compatible with Enum.
    //
    if ( newName.indexOf( "java/lang/Enum" ) != -1 ) {
      // System.out.println( "Replacing Enum." );
      newName = newName.replace( "java/lang/Enum", "com/rc/retroweaver/runtime/Enum_" );
    }

    if ( newName != name ) {
      int value = pool.addUTF8( newName );
      // System.out.println( "Replacing " + name + " with " + newName );
      return value;
    }

    return -1;
  }

  /**
   * Changes the format of the class file from 1.5 to 1.4.
   *
   * 1) Changes the version flag to 1.4
   * 2) Rewrites synthetic access specifiers as synthetic attributes
   *
   * I have to use Jace here, because I don't see how I can make this
   * work with BCEL (easily anyway).
   *
   */
  private boolean fixupFormat() throws IOException {

    ClassFile classFile = new ClassFile( sourcePath );

    if ( lazy && classFile.getMajorVersion() == newVersion ) {
    	return false;
    }

    classFile.setVersion( newVersion, 0 );

    // Fix up identifiers in the constant pool.

    // Change the class name and class file if necessary
    //
    if ( classFile.getClassName().indexOf( '+' ) != -1 ) {

      String newClassName = classFile.getClassName().replace( '+', '$' );
      classFile.setClassName( newClassName );
      String simpleName = "";

      // System.out.println( "Setting new class name: " + newClassName );

      StringTokenizer st = new StringTokenizer( newClassName, "/" );
      while ( st.hasMoreTokens() ) {
        simpleName = st.nextToken();
      }

      File f = new File( outputPath );

      // Delete the old class file, if we're weaving to the same place.
      if ( sourcePath.equals( outputPath ) ) {
        // System.out.println( "deleting " + sourcePath );
        f.delete();
      }

      File dir = f.getParentFile();
      String name = f.getName();

      outputPath = dir.getCanonicalPath() + File.separator + simpleName + ".class";

      // System.out.println( "Updating output path: " + outputPath );
    }

    // Change the super class name if necessary
    //
    String newClassName = classFile.getSuperClassName().replace( '+', '$' );
    newClassName = newClassName.replace( "java/lang/Iterable", "com/rc/retroweaver/runtime/Iterable_" );
    newClassName = newClassName.replace( "java/lang/Enum", "com/rc/retroweaver/runtime/Enum_" );
    classFile.setSuperClassName( newClassName );

    // Run through all of the CONSTANT_Class, CONSTANT_Field, and CONSTANT_Method 
    // pool entries, and update their identifers. This catches the interfaces 
    // implemented by the class too.
    //
    jace.parser.ConstantPool pool = classFile.getConstantPool();

    for ( int i = 0; i < pool.getSize(); ++i ) {

      jace.parser.constant.Constant c = pool.getConstant( i );

      if ( c instanceof ClassConstant ) {
        ClassConstant constant = ( ClassConstant ) c;
        update( pool, constant );
      }
      else if ( c instanceof NameAndTypeConstant ) {
        // catches fieldref, methodref, and interfacemethodref constants
        NameAndTypeConstant constant = ( NameAndTypeConstant ) c;
        update( pool, constant );
      }
      else {
        continue;
      }
    }

    // Now run through all of the methods, fields, and local variables
    // updating their names and signatures.
    //

    for ( ClassMethod m : classFile.getMethods() ) {

      update( pool, m );

      CodeAttribute code = m.getCode();

      if ( code != null ) {
        LocalVariableTableAttribute lvt = code.getLocalVariableTable();
        if ( lvt != null ) {
          for ( LocalVariableTableAttribute.Variable v : lvt.getVariables() ) {
            update( pool, v );
          }
        }
      }
    }

    for ( ClassField f : classFile.getFields() ) {
      update( pool, f );
    }

    classFile.writeClass( outputPath );

    return true;
  }

  private void update( jace.parser.ConstantPool pool, ClassConstant constant ) {
    int nameIndex = updateId( pool, constant.getNameIndex() );
    if ( nameIndex != -1 ) {
      constant.setNameIndex( nameIndex );
    }
  }

  private void update( jace.parser.ConstantPool pool, NameAndTypeConstant constant ) {
    int nameIndex = updateId( pool, constant.getNameIndex() );
    if ( nameIndex != -1 ) {
      constant.setNameIndex( nameIndex );
    }
    int descriptorIndex = updateId( pool, constant.getDescriptorIndex() );
    if ( descriptorIndex != -1 ) {
      constant.setDescriptorIndex( descriptorIndex );
    }
  }

  private void update( jace.parser.ConstantPool pool, LocalVariableTableAttribute.Variable var ) {
    int nameIndex = updateId( pool, var.nameIndex() );
    if ( nameIndex != -1 ) {
      var.setNameIndex( nameIndex );
    }
    int descriptorIndex = updateId( pool, var.descriptorIndex() );
    if ( descriptorIndex != -1 ) {
      var.setDescriptorIndex( descriptorIndex );
    }
  }

  private void update( jace.parser.ConstantPool pool, ClassField field ) {
    int nameIndex = updateId( pool, field.getNameIndex() );
    if ( nameIndex != -1 ) {
      field.setNameIndex( nameIndex );
    }
    int descriptorIndex = updateId( pool, field.getDescriptorIndex() );
    if ( descriptorIndex != -1 ) {
      field.setDescriptorIndex( descriptorIndex );
    }
  }

  private void update( jace.parser.ConstantPool pool, ClassMethod method ) {
    int nameIndex = updateId( pool, method.getNameIndex() );
    if ( nameIndex != -1 ) {
      method.setNameIndex( nameIndex );
    }
    int descriptorIndex = updateId( pool, method.getDescriptorIndex() );
    if ( descriptorIndex != -1 ) {
      method.setDescriptorIndex( descriptorIndex );
    }
  }

  /**
   * Replaces calls to the 1.5 runtime with calls to the Retroweaver runtime.
   * Fixes autoboxing, class literals, (_future_ enums too?)
   */
  private void fixupRuntimeCalls() throws IOException {

    // Fix up code in all of the methods
    //
    Method[] methods = generator.getMethods();
    for ( int i = 0; i < methods.length; ++i ) {
      Method m = methods[ i ];

      MethodGen methodGen = new MethodGen( m, className, constantPool );
      InstructionList list = methodGen.getInstructionList();

      // It's possible that this is a native or abstract method
      // and has no instructions associated with it.
      // In that case, it's not going to be a match.
      if ( list == null ) {
        continue;
      }

      fixupAutoboxing( list );
      fixupClassLiterals( list );

      methodGen.setMaxLocals();
      methodGen.setMaxStack();

      generator.removeMethod( m );
      generator.addMethod( methodGen.getMethod() );
      list.dispose();
    }
  }

  /**
   * Fix autoboxing.
   *
   * Search for calls to invokestatic on any of the <Primitive>.valueOf functions
   * and replace them with calls to our own runtime.
   */
  private void fixupAutoboxing( InstructionList list ) {

    InstructionFinder finder = new InstructionFinder( list );
    InstructionFinder.CodeConstraint constraint = new InstructionFinder.CodeConstraint() {
      public boolean checkCode( InstructionHandle[] match ) {
        INVOKESTATIC instruction = ( INVOKESTATIC ) match[ 0 ].getInstruction();
        String methodName = instruction.getMethodName( constantPool );
        Type t = instruction.getType( constantPool );
        Type[] argTypes = instruction.getArgumentTypes( constantPool );
        // System.out.println( "type = " + t );
        boolean matches = 
          methodName.equals( VALUE_OF_METHOD ) && 
          boxTypes.containsKey( t ) &&
          argTypes.length == 1 &&
          argTypes[ 0 ] instanceof BasicType;

        // System.out.println( "match = " + matches );
        return matches;
      }
    };

    Iterator it = finder.search( "INVOKESTATIC", constraint );

    if ( ! it.hasNext() ) {
      return;
    }

    while ( it.hasNext() ) {
      InstructionHandle[] instructions = ( InstructionHandle[] ) it.next();
      InstructionHandle match = instructions[ 0 ];
      INVOKESTATIC instruction = ( INVOKESTATIC ) match.getInstruction();
      Type t = instruction.getType( constantPool );
      constantPool.addClass( AUTOBOX_CLASS );
      int methodIndex = constantPool.addMethodref( AUTOBOX_CLASS, VALUE_OF_METHOD, ( String ) boxTypes.get( t ) );
      INVOKESTATIC replacementInstruction = new INVOKESTATIC( methodIndex );
      InstructionHandle handle = list.insert( match, replacementInstruction );

      try {
        list.delete( match );
      }
      catch( TargetLostException e ) {
        InstructionHandle[] targets = e.getTargets();
        for ( int i = 0; i < targets.length; i++ ) {
          InstructionTargeter[] targeters = targets[ i ].getTargeters();
   
          for ( int j = 0; j < targeters.length; j++ ) {
            targeters[ j ].updateTarget( targets[ i ], handle );
          }
        }
      }
    }
  }

  /**
   * Fix class literals.
   *
   * The 1.5 VM has had its ldc* instructions updated so that it knows how to deal with 
   * CONSTANT_Class in addition to the other types. So, we have to search for uses of 
   * ldc* that point to a CONSTANT_Class and replace them with calls to our runtime.
   *
   */
  private void fixupClassLiterals( InstructionList list ) {

    final String searchTerm = "(LDC | LDC_W)";

    InstructionFinder finder = new InstructionFinder( list );
    InstructionFinder.CodeConstraint constraint = new InstructionFinder.CodeConstraint() {
      public boolean checkCode( InstructionHandle[] match ) {

        if ( match.length != 1 ) {
          // System.out.println( "Ignoring a match of length: " + match.length );
          return false;
        }

        Instruction instruction = match[ 0 ].getInstruction();

        if ( ! ( instruction instanceof LDC ) ) { // catches both LDC and LDC_W
          return false;
        }

        LDC ldc = ( LDC ) instruction;
        org.apache.bcel.classfile.Constant c = constantPool.getConstant( ldc.getIndex() );

        return c instanceof ConstantClass;
      }
    };

    Iterator it = finder.search( searchTerm, constraint );

    if ( ! it.hasNext() ) {
      return;
    }

    while ( it.hasNext() ) {
      InstructionHandle[] instructions = ( InstructionHandle[] ) it.next();
      InstructionHandle match = instructions[ 0 ];
      LDC instruction = ( LDC ) match.getInstruction();
      ConstantClass c = ( ConstantClass ) constantPool.getConstant( instruction.getIndex() );
      ConstantUtf8 cUtf8 = ( ConstantUtf8 ) constantPool.getConstant( c.getNameIndex() );
      String className = cUtf8.getBytes();
      int classNameIndex = constantPool.addString( className );

      // System.out.println( "Replacing an LDC for CONSTANT_Class " + className );
      constantPool.addClass( CLASS_LITERAL_CLASS );
      int methodIndex = constantPool.addMethodref( CLASS_LITERAL_CLASS, GET_CLASS_METHOD, GET_CLASS_SIGNATURE );

      // TODO: Need to dispose newInstructions, but when is it safe??!!
      InstructionList newInstructions = new InstructionList();
      newInstructions.append( new LDC_W( classNameIndex ) );
      newInstructions.append( new INVOKESTATIC( methodIndex ) );
      InstructionHandle handle = list.insert( match, newInstructions );

      try {
        list.delete( match );
      }
      catch( TargetLostException e ) {
        InstructionHandle[] targets = e.getTargets();
        for ( int i = 0; i < targets.length; i++ ) {
          InstructionTargeter[] targeters = targets[ i ].getTargeters();
   
          for ( int j = 0; j < targeters.length; j++ ) {
            targeters[ j ].updateTarget( targets[ i ], handle );
          }
        }
      }

      finder.reread();
      it = finder.search( searchTerm, constraint );
    }
  }

  private boolean alreadyAspected() {

    // do nothing for now
    // I could add in an attribute to look for here

    return false; 
  }

  public void setListener( WeaveListener listener ) {
    this.listener = listener;
  }

  public void setLazy( boolean lazy ) {
    this.lazy = lazy;
  }

  public static String getUsage() {

    /*
    StringBuffer buf = new StringBuffer();

    buf.append( "Usage: RetroWeaver " );
    buf.append( newLine );
    buf.append( "  <source path>" );
    buf.append( newLine );
    buf.append( "  <output path>" );

    return buf.toString();
    */

    return "Usage: RetroWeaver " + newLine +
           "  <source path>" + newLine +
           "  [<output path>]";
  }

  public static void main( String[] args ) {

    if ( args.length < 1 ) {
      System.out.println( getUsage() );
      return;
    }

    String sourcePath = args[ 0 ];
    String outputPath = null;

    if ( args.length > 1 ) {
      outputPath = args[ 1 ];
    }

    try {
      RetroWeaver weaver = new RetroWeaver( Weaver.VERSION_1_4 );
      weaver.weave( sourcePath, outputPath );
    }
    catch ( Throwable t ) {
      t.printStackTrace();
    }
  }
}

