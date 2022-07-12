package uci.processor;

import java.util.List;

import uci.exception.UCIRuntimeException;
import uci.model.BestMove;
import uci.parser.BestMoveParser;

public class BestMoveProcessor extends UCICommandProcessor<BestMove> {

    protected static BestMoveParser bestMoveParser = new BestMoveParser();

    @Override
    public BestMove process(List<String> list) {
        return list.stream()
                .filter(bestMoveParser::matches)
                .findFirst()
                .map(bestMoveParser::parse)
                .orElseThrow(()->new UCIRuntimeException("Cannot find best movement in engine output!\n"));
    }
}
