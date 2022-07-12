package uci.parser;

import uci.model.BestMove;
import java.util.regex.Matcher;

public class BestMoveParser extends AbstractParser<BestMove> {

    private static final String BEST_MOVE_REGEX = "bestmove\\s([\\d\\w]*)\\sponder\\s([\\d\\w]*)";

    private BestMoveParser(String regex) {
        super(regex);
    }

    public BestMoveParser() {
        this(BEST_MOVE_REGEX);
    }

    @Override
    protected BestMove doParse(String line, Matcher matcher) {
        
        return new BestMove(matcher.group(1), matcher.group(2));
    }
}
