package net.alexmiranda.kalah;

class SowingOpponentRow implements GameState {
    @Override
    public int visit(Game game, Pit pit, int seedsLeft) {
        Preconditions.check(seedsLeft > 0, "seedsLeft");
        if (pit instanceof Store) {
            game.setState(SOWING_OWN_ROW);
            return seedsLeft;
        }
        pit.take(game.player(), 1);
        return seedsLeft - 1;
    }

    @Override
    public void endTurn(Game game, Pit pit) {
        if (game.checkGameOver()) {
            game.terminate();
            game.setState(FINISHED);
        } else {
            game.switchPlayer();
            game.setState(WAITING);
        }
    }
}