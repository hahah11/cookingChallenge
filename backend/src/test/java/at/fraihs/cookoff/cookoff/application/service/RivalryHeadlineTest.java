package at.fraihs.cookoff.cookoff.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RivalryHeadlineTest {

    @Test
    void should_sayNoFaceOff_when_totalsAreAllZero() {
        assertEquals("Alice and Bob haven't faced off yet.", RivalryHeadline.build("Alice", "Bob", 0, 0, 0));
    }

    @Test
    void should_nameLeaderFirst_when_cookAHasMoreWins() {
        assertEquals("Alice leads Bob 3-1 (1 draw)", RivalryHeadline.build("Alice", "Bob", 3, 1, 1));
    }

    @Test
    void should_nameLeaderFirst_when_cookBHasMoreWins() {
        assertEquals("Bob leads Alice 4-2 (2 draws)", RivalryHeadline.build("Alice", "Bob", 2, 4, 2));
    }

    @Test
    void should_sayTied_when_winsAreEqual() {
        assertEquals("Alice and Bob are tied 2-2", RivalryHeadline.build("Alice", "Bob", 2, 2, 0));
    }
}
