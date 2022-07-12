package uci.parser;

import static java.lang.Integer.parseInt;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uci.model.Move;
import uci.model.Strength;

public class InfoDepthParser extends AbstractParser<Move> {

	Pattern movePattern = Pattern.compile("[a-h][1-8][a-h][1-8][KkQqNnRr]?");
	
    private InfoDepthParser(String regex) {
        super(regex);
    }
    
    @Override
    public boolean matches(String line) { 
    	return line.startsWith("info depth") && line.indexOf(" pv ") > 0;
    }

    public InfoDepthParser() {
        this("");
    }

    @Override
    public Move parse(String line) {
        int depth = parseIntOption(line, "depth", 0);
        int multipv = parseIntOption(line, "multipv", 1); 
        int cp = parseIntOption(line, "cp", Integer.MIN_VALUE);
        int mate = parseIntOption(line, "mate", 0);                   
        int idx = line.indexOf(" pv ");        
        ArrayList<String> moves = new ArrayList<>();        
        boolean first = true;
        String move = "";        
        if(idx >= 0 ) {                 	
        	for(String s: line.substring(idx+4).split(" ")) { 
        		if(movePattern.matcher(s).matches()) {
        			if(!first) { 
        				moves.add(s);
        			} else { move = s; first = false; }
        		}
        	}
        }        
        return new Move(move, depth, new Strength(cp > Integer.MIN_VALUE ? "cp "+cp : "mate "+mate), multipv, moves.toArray(new String[] {}));        
    }
    
    public int parseIntOption(String line, String option, int defaultValue) {    	
    	Pattern pattern = Pattern.compile(".*"+option+"\\s+(-?\\d+).*");    	
    	Matcher m = pattern.matcher(line);    	
    	if(m.matches()) return parseInt(m.group(1));
    	return defaultValue;    	
    }

	@Override
	protected Move doParse(String line, Matcher matcher) {
		return null;
	}
}
