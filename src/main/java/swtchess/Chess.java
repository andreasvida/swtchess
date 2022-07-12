package swtchess;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.swt.SWT;

import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Chess {
	public static Display display = new Display();
	
	public static void main(String[] args) throws IOException {
		final Shell shell = new Shell(display);
		shell.setSize(1000, 500);
		shell.setText("SWT Chess");		
		shell.setLayout(new FillLayout());		
		SashForm sashForm = new SashForm(shell, SWT.HORIZONTAL);		
		ChessBoardWidget chessboardWidget = new ChessBoardWidget(sashForm, SWT.NONE, display);
		
		String[] engine; 
		
		if(args.length>1) { 
			 engine = Arrays.copyOfRange(args, 0, args.length);
		} else { 				
			engine = new String[] {"C:\\Users\\User\\Desktop\\games\\arena\\Engines\\stockfish-8-win\\Windows\\stockfish_8_x64.exe"};			
			//engine = new String[] {"C:\\java8\\bin\\java.exe", "-jar", "C:\\Users\\User\\Desktop\\Eubos.jar"};
		}
		
		EngineWidget engineWidget = new EngineWidget(sashForm, engine);
		chessboardWidget.addFenListener(engineWidget);		
		shell.open();		
		while ((!shell.isDisposed())) {
			if (!display.readAndDispatch())
				display.sleep();
		}		
	}

}
