package uci;

import uci.processor.AnalysisProcessor;
import uci.processor.BestMoveProcessor;
import uci.processor.EngineInfoProcessor;
import uci.exception.*;
import uci.model.Analysis;
import uci.model.BestMove;
import uci.model.EngineInfo;
import uci.model.Move;
import uci.parser.InfoDepthParser;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;
import static uci.breaks.Break.breakOn;

/**
 * <p>
 * This class represents a simple UCI Client implementation.
 * </p>
 *
 * <p>
 * Not all the UCI operations are implemented, but the most important ones are.
 * </p>
 *
 * <p>
 * The client was tested with Stockfish 13
 * </p>
 */
public class UCI {

	private static final long DEFAULT_TIMEOUT_VALUE = 60_000l;

	// Processors
	public static final BestMoveProcessor bestMove = new BestMoveProcessor();
	public static final AnalysisProcessor analysis = new AnalysisProcessor();
	public static final EngineInfoProcessor engineInfo = new EngineInfoProcessor();

	TreeMap<Integer, Move> bestLines = new TreeMap<>();

	private final long defaultTimeout;
	private Process process = null;
	private BufferedReader reader = null;
	private OutputStreamWriter writer = null;
	private Thread readerThread = null;
	private NewAnalysisListener analysisListener;
	private Queue<String> queue = new LinkedList<>();
	
	boolean go = false; 

	public UCI(long defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public UCI() {
		this(DEFAULT_TIMEOUT_VALUE);
	}

	public EngineInfo start(String[] cmd) {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			this.process = pb.start();
			this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			this.writer = new OutputStreamWriter(process.getOutputStream());
			// An UCI command needs to be sent to initialize the engine
			readerThread = new Thread(() -> {
				String line;
				InfoDepthParser parser = new InfoDepthParser();
				try {
					while ((line = reader.readLine()) != null) {
						synchronized (queue) {
							if (parser.matches(line)) {
								Move move = parser.parse(line);
								bestLines.put(move.getPv(), move);
								StringBuffer buf = new StringBuffer();
								for (Move m : bestLines.values()) {
									buf.append(m.getStrength()).append(" ").append(m.getLan()).append(" ");
									for (String s : m.getContinuation()) {
										buf.append(" ").append(s);
									}
									buf.append('\n');
								}
								if (analysisListener != null)
									analysisListener.notify(buf.toString());
							}
							queue.add(line);
							queue.notifyAll();
						}
					}
				} catch (IOException e) {
					//swallow IO exception
				}
			});
			readerThread.start();
			return getEngineInfo().getResult();
		} catch (IOException e) {
			throw new UCIRuntimeException(e);
		}
	}

	private String getNextLine() {
		synchronized (queue) {
			while (queue.isEmpty()) {
				try {
					queue.wait(1000);					
				} catch (InterruptedException e) {
					return "";
				}
			}
			return queue.remove();

		}
	}

	public void close() {
		if (this.process.isAlive()) {
			this.process.destroy();
		}
		try {
			reader.close();
			writer.close();
			queue.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public long getDefaultTimeout() {
		return defaultTimeout;
	}

	public <T> UCIResponse<T> command(String cmd, Function<List<String>, T> commandProcessor,
			Predicate<String> breakCondition, long timeout) {
		CompletableFuture<List<String>> command = supplyAsync(() -> {
			final List<String> output = new ArrayList<>();
			try {
				writer.flush();
				writer.write(cmd + "\n");
				writer.write("isready\n");
				writer.flush();
				String line;
				do {
					line = getNextLine();
					output.add(line);
				} while (!breakCondition.test(line));
			} catch (IOException e) {
				throw new UCIUncheckedIOException(e);
			} catch (RuntimeException e) {
				throw new UCIRuntimeException(e);
			}
			return output;
		});

		CompletableFuture<UCIResponse<T>> processorFuture = command.handle((list, ex) -> {
			try {
				return new UCIResponse<>(commandProcessor.apply(list), (UCIRuntimeException) ex);
			} catch (RuntimeException e) {
				return new UCIResponse<T>(null, new UCIRuntimeException(e));
			}
		});

		try {
			return processorFuture.get(timeout, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			return new UCIResponse<>(null, new UCITimeoutException(e));
		} catch (RuntimeException e) {
			return new UCIResponse<>(null, new UCIRuntimeException(e));
		} catch (InterruptedException e) {
			return new UCIResponse<>(null, new UCIInterruptedException(e));
		} catch (ExecutionException e) {
			return new UCIResponse<>(null, new UCIExecutionException(e));
		}

	}

	public <T> UCIResponse<T> command(String cmd, Function<List<String>, T> commandProcessor,
			Predicate<String> breakCondition) {
		return command(cmd, commandProcessor, breakCondition, defaultTimeout);
	}

	public UCIResponse<EngineInfo> getEngineInfo() {
		return command("uci", engineInfo::process, breakOn("readyok"), defaultTimeout);
	}

	public UCIResponse<List<String>> uciNewGame(long timeout) {
		return command("ucinewgame", identity(), breakOn("readyok"), timeout);
	}

	public UCIResponse<List<String>> uciNewGame() {
		return command("ucinewgame", identity(), breakOn("readyok"), defaultTimeout);
	}

	public void stop() {		
		bestLines.clear();
		// ignore stop without go (may crash some engine e.g. Eubos)
		if(go) {
			go = false;
		    command("stop", identity(), s -> true, defaultTimeout);
		}
	}

	public void goInfinite(NewAnalysisListener listener) {

		analysisListener = listener;
		command("go infinite", identity(), s -> true, defaultTimeout);
		go = true; 
	}

	public UCIResponse<List<String>> setOption(String optionName, String value, long timeout) {
		return command(format("setoption name %s value %s", optionName, value), identity(), breakOn("readyok"),
				timeout);
	}
	
	public void setVariations(int i) {
		bestLines.clear();
		setOption("MultiPV", Integer.toString(i));
	}

	public UCIResponse<List<String>> setOption(String optionName, String value) {
		return setOption(optionName, value, defaultTimeout);
	}

	public UCIResponse<List<String>> positionFen(String fen, long timeout) {
		return command(format("position fen %s", fen), identity(), breakOn("readyok"), timeout);
	}

	public UCIResponse<List<String>> positionFen(String fen) {
		return positionFen(fen, defaultTimeout);
	}

	public UCIResponse<BestMove> bestMove(int depth, long timeout) {
		return command(format("go bestmove depth %d", depth), bestMove::process, breakOn("bestmove"), timeout);
	}

	public UCIResponse<BestMove> bestMove(int depth) {
		return bestMove(depth, defaultTimeout);
	}

	public UCIResponse<BestMove> bestMove(long moveTime, long timeout) {
		return command(format("go bestmove movetime %d", moveTime), bestMove::process, breakOn("bestmove"), timeout);
	}

	public UCIResponse<BestMove> bestMove(long moveTime) {
		return bestMove(moveTime, defaultTimeout);
	}

	public UCIResponse<Analysis> analysis(long moveTime, long timeout) {
		return command(format("go movetime %d", moveTime), analysis::process, breakOn("bestmove"), timeout);
	}

	public UCIResponse<Analysis> analysis(long moveTime) {
		return analysis(moveTime, defaultTimeout);
	}

	public UCIResponse<Analysis> analysis(int depth, long timeout) {
		return command(format("go depth %d", depth), analysis::process, breakOn("bestmove"), timeout);
	}

	public UCIResponse<Analysis> analysis(int depth) {
		return analysis(depth, defaultTimeout);
	}

}