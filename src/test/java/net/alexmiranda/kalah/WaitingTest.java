package net.alexmiranda.kalah;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WaitingTest {
    private final GameState sut = GameState.WAITING;

    @Property
    public void testBeginTurn(@ForAll @IntRange(min = 1) int seeds, @ForAll Player player) {
        var house = new House(seeds, player);
        var game = mock(Game.class);
        when(game.player()).thenReturn(player);
        sut.beginTurn(game, house);
        verify(game, times(1)).accept(house, seeds);
    }

    @Example
    public void testCannotVisit() {
        assertThatThrownBy(() -> sut.visit(null, null, 0))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Example
    public void testCannotEndTurn() {
        assertThatThrownBy(() -> sut.endTurn(null, null))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
