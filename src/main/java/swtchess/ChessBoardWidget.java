package swtchess;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import ictk.boardgame.AmbiguousMoveException;
import ictk.boardgame.IllegalMoveException;
import ictk.boardgame.chess.ChessBoard;
import ictk.boardgame.chess.ChessGame;
import ictk.boardgame.chess.ChessMove;
import ictk.boardgame.chess.io.FEN;

public class ChessBoardWidget extends Canvas implements PaintListener {
	static Image[][] squares;	
	Rectangle clientArea;

	ChessGame game = new ChessGame();
	
	ChessBoard currentBoard = null;

	Map<Character, Image> imageMap = new HashMap<Character, Image>();
	FenListener fenListener;
	FEN f = new FEN();

	Point fromPoint = new Point(-1, -1);
	char pickedPiece;
		
	public Point computeSize (int wHint, int hHint, boolean changed) {
		return new Point(500,500);
	}
	
	public ChessBoardWidget(Composite parent, int style, Display display) {
		super(parent, SWT.NO_BACKGROUND);
			
		game.setBoard(new ChessBoard(true));	
						

		Listener listener = new Listener() {
			Point toPoint;
			boolean drag = false; 
			public void handleEvent(Event event) {
				
				boolean positionChanged = false; 

				int x = 1 + (event.x / (getClientArea().width / 8));
				int y = 8 - (event.y / (getClientArea().height / 8));

				currentBoard = (ChessBoard)game.getBoard();
				
				switch (event.type) {
				case SWT.MouseDown:
					fromPoint = new Point(x, y);					
					pickedPiece = currentBoard.toCharArray()[x-1][y-1];										
					break;
				case SWT.DragDetect:
					drag = true;
					break;
					
				case SWT.MouseMove:
					if(drag)
						redraw();
					break; 
				case SWT.MouseUp:
					drag = false;
					toPoint = new Point(x, y);
					ChessMove move;									
					try { 											
						char[][] pos = currentBoard.toCharArray();						
						move = new ChessMove(currentBoard, currentBoard.getSquare((byte) (fromPoint.x), (byte) (fromPoint.y)),
								currentBoard.getSquare((byte) (toPoint.x), (byte) (toPoint.y)));						 
						if(pos[fromPoint.x-1][fromPoint.y-1] == 'K' || pos[fromPoint.x-1][fromPoint.y-1] == 'k') {						
							if(fromPoint.x - x == -2) {
								move = new ChessMove(currentBoard,  ChessMove.CASTLE_KINGSIDE);
							} else if(fromPoint.x - x == 2) { 
								move = new ChessMove(currentBoard,  ChessMove.CASTLE_QUEENSIDE);
							}
						}					
						
						currentBoard.verifyIsLegalMove(move);
						try {
							game.getHistory().add(move);
						} catch (AmbiguousMoveException | IndexOutOfBoundsException e) {
							e.printStackTrace();
						} 				
						positionChanged = true;						
						
					} catch (ArrayIndexOutOfBoundsException a) { 						
					} catch (IllegalMoveException e) {						
						return;
					} finally { 
						redraw();
						fromPoint = new Point(-1, -1);
					}
					break;
					
				case SWT.MouseWheel: 
					// take back last move				 
					game.getHistory().prev();				
					redraw();
					positionChanged = true;
				}
			
				if(positionChanged) { 
					notifyListener(f.boardToString(game.getBoard()));
				}
			}
		};

		this.addListener(SWT.MouseDown, listener);
		this.addListener(SWT.DragDetect, listener);
		this.addListener(SWT.MouseUp, listener);
		this.addListener(SWT.MouseWheel, listener);
		this.addListener(SWT.MouseMove, listener);

		squares = new Image[8][8];

		imageMap.put('P', new Image(display, ChessBoardWidget.class.getResourceAsStream("wpawn.png")));
		imageMap.put('p', new Image(display, ChessBoardWidget.class.getResourceAsStream("bpawn.png")));
		imageMap.put('R', new Image(display, ChessBoardWidget.class.getResourceAsStream("wrook.png")));
		imageMap.put('r', new Image(display, ChessBoardWidget.class.getResourceAsStream("brook.png")));
		imageMap.put('B', new Image(display, ChessBoardWidget.class.getResourceAsStream("wbishop.png")));
		imageMap.put('b', new Image(display, ChessBoardWidget.class.getResourceAsStream("bbishop.png")));
		imageMap.put('N', new Image(display, ChessBoardWidget.class.getResourceAsStream("wknight.png")));
		imageMap.put('n', new Image(display, ChessBoardWidget.class.getResourceAsStream("bknight.png")));
		imageMap.put('K', new Image(display, ChessBoardWidget.class.getResourceAsStream("wking.png")));
		imageMap.put('k', new Image(display, ChessBoardWidget.class.getResourceAsStream("bking.png")));
		imageMap.put('Q', new Image(display, ChessBoardWidget.class.getResourceAsStream("wqueen.png")));
		imageMap.put('q', new Image(display, ChessBoardWidget.class.getResourceAsStream("bqueen.png")));
		this.addPaintListener(this);
						

	}

	public void paintControl(PaintEvent e) {
	//	if(getParent().getSize().x == 0 || getSize().y == 0) return;

		// Create the image to fill the canvas
		Image image = (Image) getData("double-buffer-image");
		if (image == null || image.getBounds().width != getSize().x || image.getBounds().height != getSize().y) {
			image = new Image(e.display, getSize().x, getSize().y);
			setData("double-buffer-image", image);
		}
		GC gcImage = new GC(image);
		drawBoard(gcImage);
		// Draw the offscreen buffer to the screen
		e.gc.drawImage(image, 0, 0);		
		gcImage.dispose();

	}
	
	public void drawBoard(GC c) {
				
		Rectangle clientArea = getClientArea();		
		
		for (int v = 0; v < 8; v++) {
			for (int h = 0; h < 8; h++) {

				int squareWidth = clientArea.width / 8;
				int squareHeight = clientArea.height / 8;
				boolean isWhiteSquare = ((v + h) % 2 == 0);
				Color squareColor = isWhiteSquare ? new Color(null, 200, 200, 200) : new Color(null, 0, 75, 0);
				c.setBackground(squareColor);
				c.fillRectangle(v * squareWidth, h * squareHeight, squareWidth, squareHeight);
				c.setForeground(new Color(null, 0, 0, 0));
				c.drawRectangle(v * squareWidth, h * squareHeight, squareWidth, squareHeight);
			}
			c.drawRectangle(0, 0, clientArea.width - 1, clientArea.height - 1);

		}

		char[][] chars = ((ChessBoard)game.getBoard()).toCharArray();
		for (int v = 0; v < 8; v++) {
			for (int h = 0; h < 8; h++) {				
				if( v == fromPoint.x-1 && h == 8 -fromPoint.y)  {										
					Point p = toControl(Display.getCurrent().getCursorLocation());					
					// draw hovering piece at mouse position
					drawPiece(c, p.x, p.y, imageMap.get(pickedPiece), true);					
					
				} else {  				
					drawPiece(c, v, h, imageMap.get(chars[v][7-h]), false);
				}
			}
		}
	}

	private void drawPiece(GC c, int v, int h, Image image, boolean absoluteCoords) {
		if (image == null)
			return;
		Rectangle imageRect = image.getBounds();
		int imageWidth = imageRect.width;
		int imageHeight = imageRect.height;
		int squareWidth = getClientArea().width / 8 - 2;
		int squareHeight = getClientArea().height / 8 - 2;
		
		int x = v * (squareWidth + 2) + 1;
		int y = h * (squareHeight + 2) + 1;
		
		if(absoluteCoords) { 
			x = v  - (squareWidth / 2) ;
			y = h - (squareHeight / 2 ); 
			
		}
		c.drawImage(image, 0, 0, imageWidth, imageHeight, x, y,
				squareWidth, squareHeight);

	}
	
	public void addFenListener(FenListener listener) { 
		fenListener = listener;
	}
	
	protected void notifyListener(String fen) { 
		if(fenListener != null) fenListener.newPosition(fen);
	}

}
