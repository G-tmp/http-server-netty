package neh.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class RangeRequestParser {

    private RangeRequestParser(){}


    public static class Range {
        public Integer start;
        public Integer end;

        public String toString() {
            return String.format("range: %d - %d", start, end);
        }
    }

    public static List<Range> parseRanges(String input) {
        List<Range> ranges = new ArrayList();

        Range range;
        for(StringTokenizer tokenizer = new StringTokenizer(input.substring("bytes=".length()), ", "); tokenizer.hasMoreTokens(); ranges.add(range)) {
            range = new Range();
            String split = tokenizer.nextToken();

            if (split.startsWith("-")) {
                range.start = null;
                range.end = Integer.valueOf(split.substring(1));
            } else if (split.endsWith("-")) {
                range.end = null;
                range.start = Integer.valueOf(split.substring(0, split.length() - 1));
            } else {
                String s = split.split("-")[0];
                String e = split.split("-")[1];
                range.start = Integer.valueOf(s);
                range.end = Integer.valueOf(e);
            }
        }

        return ranges;
    }


}
