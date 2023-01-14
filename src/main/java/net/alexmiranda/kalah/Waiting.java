package net.alexmiranda.kalah;

class Waiting implements GameState {
    @Override
    public void beginTurn(Game game, Pit pit) {
        int seeds = pit.select(game.player());
        game.setState(SOWING_OWN_ROW);
        game.accept(pit, seeds);
    }
}
