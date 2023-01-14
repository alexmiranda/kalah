package net.alexmiranda.kalah;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Positive;

public class SowingOpponentRowTest {
    private final GameState sut = GameState.SOWING_OPPONENT_ROW;

    @Example
    public void testCannotBeginTurn() {
        assertThatThrownBy(() -> sut.beginTurn(null, null))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Property
    public void testVisitPitWithNoSeedsLeft(@ForAll @IntRange(max = 0) int seedsLeft) {
        assertThatThrownBy(() -> sut.visit(null, null, seedsLeft))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    public void testVisitHouse(@ForAll @Positive int seeds, @ForAll Player player, @ForAll @IntRange(min = 1) int seedsLeft) {
        var house = new House(seeds, player);
        var game = mock(Game.class);
        when(game.player()).thenReturn(player.opponent());
        int remaining = sut.visit(game, house, seedsLeft);
        assertThat(house.seeds()).isEqualTo(seeds + 1);
        assertThat(remaining).isEqualTo(seedsLeft - 1);
    }

    @Property
    public void testVisitStore(@ForAll @Positive int seeds, @ForAll Player player, @ForAll @IntRange(min = 1) int seedsLeft) {
        var store = new Store(seeds, player);
        var game = mock(Game.class);
        when(game.player()).thenReturn(player.opponent());
        int remaining = sut.visit(game, store, seedsLeft);
        assertThat(store.seeds()).isEqualTo(seeds);
        assertThat(remaining).isEqualTo(seedsLeft);
        verify(game, times(1)).setState(GameState.SOWING_OWN_ROW);
    }

    @Example
    public void testEndTurn() {
        var game = mock(Game.class);
        sut.endTurn(game, null);
        verify(game, times(1)).switchPlayer();
        verify(game, times(1)).setState(GameState.WAITING);
    }

    @Example
    public void testGameOverAfterEndingTurn() {
        var game = mock(Game.class);
        when(game.checkGameOver()).thenReturn(true);
        sut.endTurn(game, null);
        verify(game, times(1)).setState(GameState.FINISHED);
        verify(game, times(1)).terminate();
        verify(game, never()).switchPlayer();
    }
}
