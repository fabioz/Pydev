
package com.rc.retroweaver.runtime;

import java.util.*;

/**
 * A replacement for the new 1.5 ldc* class literal support.
 *
 * In 1.4, class literals were compiled into the following:
 *
 * 1) A field:
 *   static java.lang.Class class$<ClassName>;
 *
 * 2) A check and method call:
 *   0:	getstatic	#7; //Field class$<ClassName>:Ljava/lang/Class;
 *   3:	ifnonnull	18
 *   6:	ldc	        #8; //String <ClassName>
 *   8:	invokestatic	#9; //Method class$:(Ljava/lang/String;)Ljava/lang/Class;
 *   11:	dup
 *   12:	putstatic	#7; //Field class$<ClassName>:Ljava/lang/Class;
 *   15:	goto	21
 *   18:	getstatic	#7; //Field class$<ClassName>:Ljava/lang/Class;
 *   21:	astore_1
 *
 * 3) A method:
 *  static java.lang.Class class$(java.lang.String);

 *    Signature: (Ljava/lang/String;)Ljava/lang/Class;

 *    Code:

 *     0:	aload_0

 *     1:	invokestatic	#1; //Method java/lang/Class.forName:(Ljava/lang/String;)Ljava/lang/Class;

 *     4:	areturn

 *     5:	astore_1

 *     6:	new	#3; //class NoClassDefFoundError

 *     9:	dup

 *     10:	aload_1

 *     11:	invokevirtual	#4; //Method java/lang/ClassNotFoundException.getMessage:()Ljava/lang/String;

 *     14:	invokespecial	#5; //Method java/lang/NoClassDefFoundError."<init>":(Ljava/lang/String;)V

 *     17:	athrow

 *    Exception table:
 *     from   to  target type
 *       0     4     5   Class java/lang/ClassNotFoundException

 *
 * In 1.5, ldc was updated to additionally support CONSTANT_Class. All of the 1.4 generated code is 
 * replaced by ldc. In our code, we're going to mimic the 1.4 behavior, but do it without
 * all of the corresponding code bloat. We're just going to replace the ldc with a call to this
 * class, which will store the java.lang.Class's in a Map<String,Class>. This does have some drawbacks,
 * though:
 *
 *   1) The map is shared across all threads, requiring synchronization. If this becomes an issue,
 * we can take care of it by creating a Map per thread.
 *
 *   2) Classes had quick access to the class literal, because it was stored in a class static field.
 * Now, there's the overhead of a O(1) map lookup involved.
 *
 * @author Toby Reyelts
 *
 */
public class ClassLiteral {

  private static Map/*<String,Class>*/ classes = new HashMap/*<String,Class>*/();

  public static Class getClass( String className ) {

    synchronized ( classes ) {
      Class c = ( Class ) classes.get( className );
      if ( c != null ) {
        return c;
      }
    }

    try {
      Class c = Class.forName( className.replace( '/', '.' ) );
      synchronized ( classes ) {
        classes.put( className, c );
      }
      return c;
    }
    catch ( ClassNotFoundException e ) {
      throw new NoClassDefFoundError( e.getMessage() );
    }
  }
}

