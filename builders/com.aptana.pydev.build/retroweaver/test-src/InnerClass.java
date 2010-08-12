
/**
 * Ensure Retroweaver can handle inner classes ok.
 *
 * @author Toby Reyelts
 *
 */
public class InnerClass {
  public class A {
    public A() {
      System.out.println( getClass() );
    }
  }

  public static void main( String[] args ) {
    new InnerClass();
  }

  A a = new A();
}

