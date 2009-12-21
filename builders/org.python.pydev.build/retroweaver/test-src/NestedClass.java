
/**
 * Ensures Retroweaver can handle nested classes ok.
 *
 * @author Toby Reyelts
 *
 */
public class NestedClass {
  static class A {
    public A() {
      System.out.println( getClass() );
    }
  }
  public static void main( String[] args ) {
    new NestedClass();
  }
  A a = new A();
}
