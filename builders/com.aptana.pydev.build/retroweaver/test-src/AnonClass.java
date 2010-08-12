
import java.awt.event.*;

/**
 * Ensures that Retroweaver can handle anonymous classes ok.
 *
 * @author Toby Reyelts
 */
public class AnonClass {
  public static void main( String[] args ) throws Exception {
    new AnonClass().foo();
  }
  public void foo() {
    ActionListener a = new ActionListener() {
      public void actionPerformed( ActionEvent e ) {
        System.out.println( getClass() );
      }
    };
    a.actionPerformed( null );
  }
}

