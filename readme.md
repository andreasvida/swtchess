SWT GUI for analysing chess games with UCI engines.
----


The ChessBoardWidget is a resizable SWT widget, which only allows input of legal moves via drag and drop.
Mouse wheel may be used to rewind moves.

EngineWidget can start/stop a UCI engine. Number of best lines shown can be configured through +/- Buttons.

The main class is Chess (in package swtchess)
Engine executable and parameters may be supplied via command line arguments, or hardcoded in the Chess class.


Known limits
---
Only promotion to queen is implemented. The game will always start from starting position.
Make sure the png files for the pieces are on the classpath.



This project was made possible by:
---

https://github.com/PrivateEvgeny/chess
(basis for SWT chessboard drawing code enhanced with double buffering of the canvas, and drag and drop for pieces)  

https://github.com/jvarsoke/ictk
(MIT license, used for legal move generation, starting position, FEN logic etc)

https://github.com/nomemory/neat-chess
(UCI protocol logic, extended by infinite analysis, stop command)

https://github.com/cjbolt/EubosChess
(Engine used for testing) 

https://github.com/official-stockfish/Stockfish
(Engine used for testing)

https://wikipedia.org
(png files of chess pieces)





