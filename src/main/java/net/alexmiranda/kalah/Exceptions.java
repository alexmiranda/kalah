package net.alexmiranda.kalah;

class CannotPlayOnStoreException extends RuntimeException {}

class NoSuchPositionException extends RuntimeException {
    NoSuchPositionException(String position) {
        super(position);
    }
}

class HouseEmptyException extends RuntimeException {}

class OpponentHouseException extends RuntimeException {}

class HouseCaptureException extends RuntimeException {}

class NegativeSeedsException extends RuntimeException {}

class GameOverException extends RuntimeException {}
