package swtchess;
import org.eclipse.swt.SWT;

import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import uci.NewAnalysisListener;
import uci.UCI;
import uci.UCIResponse;
import uci.model.Analysis;
import uci.model.EngineInfo;
import uci.model.Move;

public class EngineWidget extends Composite implements FenListener, NewAnalysisListener {

	Button startStop;
	Button variationPlus;
	Button variationMinus;
	Text lines;
	UCI uci;
	String currentFen;
	boolean engineRunning = false;	
	int variations = 1;
	Label engineName;
	String[] engineCmd;

	public EngineWidget(Composite parent, String[] engineCmd) {		
		super(parent, 0);
		this.engineCmd = engineCmd;
		uci = new UCI();
		RowLayout rowlayout = new RowLayout(SWT.VERTICAL);
		rowlayout.wrap = false;
		setLayout(rowlayout);
		Composite buttons = new Composite(this, SWT.NONE);
		buttons.setLayout(new RowLayout(SWT.HORIZONTAL));
		startStop = new Button(buttons, SWT.NONE);
		startStop.setText("Start");
		startStop.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				startEngine();				
				if(engineRunning) { 
					Display.getCurrent().timerExec(1000, ()->{ updateAnalysis();  } );
				}
			}
		});
		variationPlus = new Button(buttons, SWT.NONE);
		variationPlus.setText("+");
		variationMinus = new Button(buttons, SWT.NONE);
		variationMinus.setText("-");
		
		engineName = new Label(buttons, SWT.NONE);
		
		Listener variationsListener  = new Listener() { 
			@Override
			public void handleEvent(Event event) {
				if(variationPlus.equals(event.widget)) variations++; else variations--;
				if(variations < 0) variations = 0;
				
				uci.setVariations(variations);				
			}
		};
		
		variationPlus.addListener(SWT.Selection, variationsListener);
		variationMinus.addListener(SWT.Selection, variationsListener);
						
		lines = new Text(this, SWT.MULTI);		
		lines.setLayoutData(new RowData(500,500));		
					
	}
	
	private void updateAnalysis() {		
		if(engineRunning) { 
			Display.getCurrent().timerExec(1000, ()->{  } );
		}		
	}

	private void startEngine() {
		if (!engineRunning) {						
			final String engine = uci.start(engineCmd).getName();
			this.getDisplay().asyncExec( ()-> { engineName.setText(engine); pack();} );
			uci.uciNewGame();
			engineRunning = true;
			if (currentFen != null && !currentFen.isEmpty()) {
				newPosition(currentFen);
			}
			startStop.setText("Stop");
		} else {			
			uci.stop();
			uci.close();
			startStop.setText("Start");
			engineRunning = false;
		}
	}

	@Override
	public void newPosition(String fen) {
		currentFen = fen;
		if (engineRunning) {
			uci.stop();
			uci.positionFen(fen);			
			uci.goInfinite(this);			
		}

	}

	@Override
	public void notify(final String newAnalysis) {				
		this.getDisplay().asyncExec( ()-> {
			lines.setText(newAnalysis);
			lines.redraw();
		} );
		
	}
}
