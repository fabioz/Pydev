
package com.rc.retroweaver.runtime;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * A version of the 1.5 java.lang.Enum class for the 1.4 VM.
 *
 * @author Toby Reyelts
 *
 */
public class Enum_<E extends Enum_<E>> implements Comparable<E>, Serializable {

  // Implementation notes:
  //
  // Due to access restrictions which are circumvented in the JDK, but can not
  // be circumvented here in user-land code, we have to pre-emptively get enum
  // classes to register themselves with Enum_, through a call to setEnumValues.
  //
  //

  private int ordinal;
  private String name;

  private static Map<Class,Object[]> enumValues = new HashMap<Class,Object[]>();

  protected Enum_( String name, int ordinal ) {
    this.name = name;
    this.ordinal = ordinal;
  }

  protected void setEnumValues( Class c, Object[] values ) {
    enumValues.put( c, values );
  }

  /**
   * Implement serialization so we can get the singleton behavior we're looking
   * for in enums.
   *
   */
  private Object readResolve() throws ObjectStreamException {
    Class c = getClass();
    return valueOf( c, name );
  }

  // This method is present in java.lang.Class in 1.5
  // but we're just duplicating it here for now
  // Perhaps we'll move it into a Class_ class in the future.
  //
  private static <T> boolean isEnum( Class<T> class_ ) {
    Class c = class_.getSuperclass();

    if ( c == null ) {
      return false;
    }

    return Enum_.class.isAssignableFrom( c );
  }

  // This method is present in java.lang.Class in 1.5
  // but we're just duplicating it here for now
  // Perhaps we'll move it into a Class_ class in the future.
  //
  private static <T> T[] getEnumConstants( Class<T> class_ ) {

    if ( ! isEnum( class_ ) ) {
      return null;
    }

    // Will uncomment this as soon as I add the static registration code to enum subclasses
    // in the bytecode enhancement.
    //
    // T[] values = ( T[] ) enumValues.get( class_ );

    T[] values = null;

    try {
      Method valuesMethod = class_.getMethod( "values" );
      values = ( T[] ) valuesMethod.invoke( null );
    }
    catch ( java.lang.reflect.InvocationTargetException e ) {
      return null;
    }
    catch ( NoSuchMethodException e ) {
      return null;
    }
    catch ( IllegalAccessException e ) {
      return null;
    }

    if ( values == null ) {
      return null;
    }

    return ( T[] ) values.clone();
  }

  public static <T extends Enum_<T>> T valueOf( Class<T> enumType, String name ) {

    if ( enumType == null ) {
      throw new NullPointerException( "enumType is null" );
    }

    if ( name == null ) {
      throw new NullPointerException( "name is null" );
    }

    T[] enums = getEnumConstants( enumType );

    if ( enums != null ) {
      for ( T enum_ : enums ) {
        if ( enum_.name.equals( name ) ) {
          return enum_;
        }
      }
    }

    throw new IllegalArgumentException( "No enum const " + enumType + "." + name );
  }

  public final boolean equals( Object other ) {
    return other == this;
  }

  public final int hashCode() {
    return System.identityHashCode( this );
  }

  public String toString() {
    return name;
  }

  // This method shows up with a final access specifier in the current specification,
  // but the current JDK 1.5 beta doesn't declare it as final. If I declare it final,
  // I get verify errors, because the JDK1.5 beta generates enums that override this method.
  //
  public int compareTo( E e ) {
    Class c1 = getDeclaringClass();
    Class c2 = e.getDeclaringClass();

    if ( c1 == c2 ) {
      return ordinal - e.ordinal;
    }

    throw new ClassCastException();
  }

  protected final Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  public final String name() {
    return name;
  }

  public final int ordinal() {
    return ordinal;
  }

  public final Class<E> getDeclaringClass() {
    Class superClass = getClass().getSuperclass();
    if ( superClass != Enum_.class ) {
      return superClass;
    }
    else {
      return ( Class ) Enum_.class;
    }
  }
}

