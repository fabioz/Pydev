
import java.util.*;

/**
 * Demonstrates Retroweaver's support for java.lang.Iterable.
 *
 * @author Toby Reyelts
 *
 */
public class ItTest {

  public static void main( String[] args ) {

    // The following commented code would fail.
    //
    // The problem is that the new JDK 1.5 Collection classes inherit from
    // Iterable, but they don't get retroweaved, so their implemented interface
    // never gets changed from Iterable to Iterable_. I'm not sure 
    // about the amount of work required to make this work. You'll see this
    // problem anywhere you make a conversion from a JDK1.5 core class that implements
    // Iterable, to the java.lang.Iterable type. 
    //
    /*
    Iterable<Integer> itConversion = Arrays.asList( new Integer[] { 1, 2, 3 } );

    for ( Integer i : itConversion ) {
      System.out.println( i );
    }*/


    // This, however, works just fine, because the anonymous class is weaved
    // by Retroweaver.
    //
    Iterable<Integer> it = new Iterable<Integer>() {
      public Iterator<Integer> iterator() {
        return Arrays.asList( new Integer[] { 1, 2, 3 } ).iterator();
      }
    };

    for ( Integer i : it ) {
      System.out.println( i );
    }
  }
}

