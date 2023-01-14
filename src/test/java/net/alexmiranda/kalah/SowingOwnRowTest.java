package net.alexmiranda.kalah;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

public class SowingOwnRowTest {
    private final GameState sut = GameState.SOWING_OWN_ROW;

    @Property
    public void testVisitPitWhenNoSeedsLeft(@ForAll @IntRange(max = 0) int seedsLeft) {
        assertThatThrownBy(() -> sut.visit(null, null, seedsLeft))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    public void testVisitHouse(@ForAll @Positive int seeds, @ForAll Player player, @ForAll @IntRange(min = 1) int seedsLeft) {
        var house = new House(seeds, player);
        var game = mock(Game.class);
        when(game.player()).thenReturn(player);
        int remaining = sut.visit(game, house, seedsLeft);
        assertThat(house.seeds()).isEqualTo(seeds + 1);
        assertThat(remaining).isEqualTo(seedsLeft - 1);
    }

    @Property
    public void testVisitStore(@ForAll @Positive int seeds, @ForAll Player player, @ForAll @IntRange(min = 2) int seedsLeft) {
        var store = new Store(seeds, player);
        var game = mock(Game.class);
        when(game.player()).thenReturn(player);
        int remaining = sut.visit(game, store, seedsLeft);
        assertThat(store.seeds()).isEqualTo(seeds + 1);
        assertThat(remaining).isEqualTo(seedsLeft - 1);
        verify(game, times(1)).setState(GameState.SOWING_OPPONENT_ROW);
    }

    @Property
    public void testVisitOwnStoreCloseToEndTurn(@ForAll @Positive int seeds, @ForAll Player player) {
        var store = new Store(seeds, player);
        var game = mock(Game.class);
        when(game.player()).thenReturn(player);
        int remaining = sut.visit(game, store, 1);
        assertThat(store.seeds()).isEqualTo(seeds + 1);
        assertThat(remaining).isEqualTo(0);
        verify(game, never()).setState(any(GameState.class));
    }

    @Property
    public void testEndTurnHouse(@ForAll @IntRange(min = 2) int seeds, @ForAll Player player) {
        var house = new House(seeds, player);
        var game = mock(Game.class);
        when(game.player()).thenReturn(player);
        sut.endTurn(game, house);
        verify(game, times(1)).setState(GameState.WAITING);
        verify(game, times(1)).switchPlayer();
        verify(game, never()).captureIntoStore(any(House.class));
    }

    @Property
    public void testEndTurnStore(@ForAll Player player) {
        var store = new Store(player);
        var game = mock(Game.class);
        when(game.player()).thenReturn(player);
        sut.endTurn(game, store);
        verify(game, times(1)).setState(GameState.WAITING);
        verify(game, never()).switchPlayer();
        verify(game, never()).captureIntoStore(any(House.class));
    }

    @Property
    public void testGameOverAfterEndingTurnStore(@ForAll Player player) {
        var store = new Store(player);
        var game = mock(Game.class);
        when(game.player()).thenReturn(player);
        when(game.checkGameOver()).thenReturn(true);
        when(game.checkGameOver(anyBoolean())).thenReturn(true);
        sut.endTurn(game, store);
        verify(game, times(1)).setState(GameState.FINISHED);
        verify(game, times(1)).terminate();
        verify(game, never()).captureIntoStore(any(House.class));
        verify(game, never()).switchPlayer();
    }

    @Property
    public void testEndTurnEmptyHouse(@ForAll Player player) {
        var house = new House(1, player); // last seed dropped, thus it has exactly one seed
        var game = mock(Game.class);
        when(game.player()).thenReturn(player);
        sut.endTurn(game, house);
        verify(game, times(1)).captureIntoStore(house);
        verify(game, times(1)).setState(GameState.WAITING);
        verify(game, times(1)).switchPlayer();
    }

    @Property
    public void testGameOverAfterEndingTurnEmptyHouse(@ForAll Player player) {
        var house = new House(1, player); // last seed dropped, thus it has exactly one seed
        var game = mock(Game.class);
        when(game.player()).thenReturn(player);
        when(game.checkGameOver()).thenReturn(true);
        when(game.checkGameOver(anyBoolean())).thenReturn(true);
        sut.endTurn(game, house);
        verify(game, times(1)).setState(GameState.FINISHED);
        verify(game, times(1)).terminate();
        verify(game, times(1)).captureIntoStore(house);
        verify(game, never()).switchPlayer();
    }

    @Example
    public void testCannotBeginTurn() {
        assertThatThrownBy(() -> sut.beginTurn(null, null))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
