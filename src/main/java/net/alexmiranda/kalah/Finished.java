package net.alexmiranda.kalah;

import java.util.Optional;

class Finished implements GameState {
    @Override
    public void beginTurn(Game game, Pit pit) {
        throw new GameOverException();
    }

    @Override
    public Optional<Player> winner(Game game) {
        return game.leadingPlayer();
    }
}
