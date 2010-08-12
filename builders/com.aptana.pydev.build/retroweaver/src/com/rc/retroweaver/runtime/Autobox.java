
package com.rc.retroweaver.runtime;

/**
 * A replacement for the new boxing functions which were added
 * for autoboxing in 1.5.
 *
 * @author Toby Reyelts
 *
 */
public class Autobox {

  private static final int boxCount = 256;

  private static Byte[] byteVals = new Byte[ boxCount ];
  private static Character[] charVals = new Character[ boxCount ];
  private static Short[] shortVals = new Short[ boxCount ];
  private static Integer[] intVals = new Integer[ boxCount ];

  // Small lookup table for boxed objects
  //
  // The spec says that the range should be from -127 to 128,
  // but a byte's range is from -128 to 127. Neal Gafter seems to imply
  // that this is a bug in the spec.
  //
  //
  static {
    for ( int i = 0; i < 256; ++i ) {
      byte val = ( byte ) ( i - 128 );
      byteVals[ i ] = new Byte( val );
      charVals[ i ] = new Character( ( char ) val );
      shortVals[ i ] = new Short( val );
      intVals[ i ] = new Integer( val );
    }
  }

  public static Boolean valueOf( boolean b ) {
    return b ? Boolean.TRUE : Boolean.FALSE;
  }

  public static Byte valueOf( byte val ) {
    return byteVals[ val + 128 ];
  }

  public static Character valueOf( char val ) {
    if ( val > -129 && val < 128 ) {
      return charVals[ val + 128 ];
    }
    else {
      return new Character( val );
    }
  }

  public static Short valueOf( short val ) {
    if ( val > -129 && val < 128 ) {
      return shortVals[ val + 128 ];
    }
    else {
      return new Short( val );
    }
  }

  public static Integer valueOf( int val ) {
    if ( val > -129 && val < 128 ) {
      return intVals[ val + 128 ];
    }
    else {
      return new Integer( val );
    }
  }

  public static Long valueOf( long l ) {
    return new Long( l );
  }

  public static Float valueOf( float f ) {
    return new Float( f );
  }

  public static Double valueOf( double d ) {
    return new Double( d );
  }
}

