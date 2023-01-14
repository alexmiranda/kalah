package net.alexmiranda.kalah;

class SowingOwnRow implements GameState {
    @Override
    public int visit(Game game, Pit pit, int seedsLeft) {
        Preconditions.check(seedsLeft > 0, "seedsLeft");
        pit.take(game.player(), 1);
        if (pit instanceof Store && seedsLeft > 1) {
            game.setState(SOWING_OPPONENT_ROW);
        }
        return seedsLeft - 1;
    }

    @Override
    public void endTurn(Game game, Pit pit) {
        if (pit instanceof Store) {
            this.resume(game, true, false);
            return;
        }
        
        if (pit instanceof House h && h.seeds() == 1) {
            this.captureAndResume(game, h);
            return;
        }
        
        this.waitForNextPlayer(game);
    }

    private void captureAndResume(Game game, House house) {
        game.captureIntoStore(house);
        this.resume(game, false, true);
    }

    private void resume(Game game, boolean samePlayer, boolean checkBothPlayers) {
        if (game.checkGameOver(checkBothPlayers)) {
            game.terminate();
            game.setState(FINISHED);
        } else if (samePlayer) {
            game.setState(WAITING);
        } else if (!samePlayer) {
            this.waitForNextPlayer(game);
        }
    }

    private void waitForNextPlayer(Game game) {
        game.switchPlayer();
        game.setState(WAITING);
    }
}
