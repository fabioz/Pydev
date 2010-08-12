
import java.util.*;
import java.util.concurrent.*;

class TestVerifyRef {

  public static void main( String[] args ) {

    // Shouldn't generate warning, because Retroweaver handles Iterables
    class It implements Iterable { 
      public Iterator iterator() {
        return Arrays.asList( new int[] { 1, 2, 3 } ).iterator();
      }
    }

    // Shouldn't generate a warning, because Retroweaver handles StringBuilder
    String s1 = "a" + args.length;

    // Shouldn't generate warnings, because Retroweaver handles autoboxing and autounboxing
    Integer i = 3;
    int i_ = i;

    // Should generate a warning, because ConcurrentHashMap is new to 1.5
    ConcurrentHashMap map = new ConcurrentHashMap();

    // Should generate a warning, because trimToSize is new in 1.5
    StringBuffer buf = new StringBuffer();
    buf.trimToSize();

    // Should generate a warning - implementing interface from 1.5
    class Ap implements Appendable {
      public Appendable append( char c ) { return null; }
      public Appendable append( CharSequence csq ) { return null; }
      public Appendable append( CharSequence csq, int start, int end ) { return null; }
    }
  }
}
