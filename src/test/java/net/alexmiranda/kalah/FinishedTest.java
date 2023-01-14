package net.alexmiranda.kalah;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

public class FinishedTest {
    private final GameState sut = GameState.FINISHED;

    @Example
    public void testCannotBeginTurn() {
        assertThatThrownBy(() -> sut.beginTurn(null, null))
            .isInstanceOf(GameOverException.class);
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

    @Property
    public void testWinner(@ForAll Player player) {
        var game = mock(Game.class);
        when(game.leadingPlayer()).thenReturn(Optional.of(player));
        var winner = sut.winner(game);
        assertThat(winner).contains(player);
        verify(game, times(1)).leadingPlayer();
    }
}
