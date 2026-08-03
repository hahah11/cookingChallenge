package at.fraihs.cookoff.cookoff.application.service;

/** Server-rendered display text for a cook-pair record, e.g. "Alice leads Bob 3-1 (1 draw)". */
final class RivalryHeadline {

    private RivalryHeadline() {
    }

    static String build(String cookAName, String cookBName, int cookAWins, int cookBWins, int draws) {
        if (cookAWins == 0 && cookBWins == 0 && draws == 0) {
            return cookAName + " and " + cookBName + " haven't faced off yet.";
        }
        String base;
        if (cookAWins == cookBWins) {
            base = cookAName + " and " + cookBName + " are tied " + cookAWins + "-" + cookBWins;
        } else if (cookAWins > cookBWins) {
            base = cookAName + " leads " + cookBName + " " + cookAWins + "-" + cookBWins;
        } else {
            base = cookBName + " leads " + cookAName + " " + cookBWins + "-" + cookAWins;
        }
        if (draws == 1) {
            return base + " (1 draw)";
        }
        if (draws > 1) {
            return base + " (" + draws + " draws)";
        }
        return base;
    }
}
