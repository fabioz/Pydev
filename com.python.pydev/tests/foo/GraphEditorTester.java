package foo;

import java.io.File;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import edu.umd.cs.piccolox.swt.PSWTCanvas;
import edu.umd.cs.piccolox.swt.PSWTImage;
import edu.umd.cs.piccolox.swt.PSWTText;

public class GraphEditorTester {

	public GraphEditorTester() {
		super();
	}

	public static void main(String[] args) {
		Display display = new Display ();
		Shell shell = open(display);
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
		display.dispose();
	}
	
	public static Shell open(Display display) {
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		PSWTCanvas canvas = new PSWTCanvas(shell,0);
		
		PSWTText text = new PSWTText("Hello World");
		canvas.getLayer().addChild(text);
		
		PSWTImage im = new PSWTImage(canvas, "E:\\eclipse_workspace\\com.python.pydev\\src\\class_obj.gif");
		canvas.getLayer().addChild(im);
		
		shell.open();
		return shell;
	}
}