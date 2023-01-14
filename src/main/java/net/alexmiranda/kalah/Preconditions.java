package net.alexmiranda.kalah;

class Preconditions {
    static void check(boolean condition, String argument) {
        if (!condition) {
            throw new IllegalArgumentException(argument);
        }
    }
}
