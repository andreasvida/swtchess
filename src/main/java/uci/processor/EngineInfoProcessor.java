package uci.processor;


import java.util.List;
import java.util.Map;

import uci.model.EngineInfo;
import uci.model.option.EngineOption;
import uci.parser.EngineNameParser;
import uci.parser.EngineOptionParser;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class EngineInfoProcessor extends UCICommandProcessor<EngineInfo> {

    protected static EngineNameParser engineNameParser = new EngineNameParser();
    protected static EngineOptionParser engineOptionParser = new EngineOptionParser();

    @Override
    public EngineInfo process(List<String> list) {
        final String engineName =
                list.stream()
                        .filter(engineNameParser::matches)
                        .map(engineNameParser::parse)
                        .findFirst()
                        .orElse("<<Unknown>>");
        final Map<String,EngineOption> options =
                list.stream()
                        .filter(engineOptionParser::matches)
                        .map(engineOptionParser::parse)
                        .collect(toMap(EngineOption::getName, identity()));
        return new EngineInfo(engineName, options);
    }
}
