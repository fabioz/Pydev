
/**
 * Ensures that Retroweaver can handle compiler-generated
 * calls to StringBuilder that are new to 1.5.
 *
 * @author Toby Reyelts
 *
 */
public class StringTest {
  public static void main( String[] args ) {
    String s = "This is " + StringTest.class + " version " + 1;
    System.out.println( s );
  }
}

