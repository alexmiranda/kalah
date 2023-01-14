package net.alexmiranda.kalah;

sealed interface Pit permits House, Store {
    int seeds();
    Pit next();
    int select(Player player);
    int take(Player player, int seeds);

    default boolean isEmpty() {
        return this.seeds() == 0;
    }

    default boolean isNotEmpty() {
        return !this.isEmpty();
    }
}
