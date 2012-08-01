
/**
 * Tests support for the new 1.5 enum language feature.
 *
 * @author Toby Reyelts
 *
 */
public enum EnumTest {

  A, B, C;

  public static void main( String[] args ) {

    for ( EnumTest t : EnumTest.values() ) {
      System.out.println( t );
    }

    EnumTest et = Enum.valueOf( EnumTest.class, "A" );
    System.out.println( et );

    boolean failure = false;

    try {
      // Should fail with IllegalArgumentException, because
      // D isn't part of the enum.
      EnumTest et2 = Enum.valueOf( EnumTest.class, "D" );
    }
    catch ( IllegalArgumentException e ) {
      failure = true;
    }

    if ( ! failure ) {
      throw new RuntimeException( "Didn't fail on bad lookup!" );
    }
  }
}

