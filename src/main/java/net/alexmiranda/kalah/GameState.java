package net.alexmiranda.kalah;

import java.util.Optional;

interface GameState {
    static GameState WAITING = new Waiting();
    static GameState SOWING_OWN_ROW = new SowingOwnRow();
    static GameState SOWING_OPPONENT_ROW = new SowingOpponentRow();
    static GameState FINISHED = new Finished();

    default void beginTurn(Game game, Pit pit) {
        unsupportedOperation();
    }

    default int visit(Game game, Pit pit, int seedsLeft) {
        unsupportedOperation();
        return 0;
    }

    default void endTurn(Game game, Pit pit) {
        unsupportedOperation();
    }

    default Optional<Player> winner(Game game) {
        return Optional.empty();
    }

    private void unsupportedOperation() {
        StackWalker walker = StackWalker.getInstance();
        String operation = walker.walk(frames -> frames.skip(1).findFirst())
            .map(StackWalker.StackFrame::getMethodName)
            .get();
        String state = this.getClass().getSimpleName();
        String msg = String.format("Unsupported operation '%s' on state '%s'", operation, state);
        throw new UnsupportedOperationException(msg);
    }
}
