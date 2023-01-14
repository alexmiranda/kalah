package net.alexmiranda.kalah;

import static net.alexmiranda.kalah.TestSupport.newGamePreconditions;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;

public class NewGameSupplier implements ArbitrarySupplier<Game> {
    @Override
    public Arbitrary<Game> get() {
        record Args(int houses, int seeds) {};
        return Arbitraries.integers()
            .between(1, Math.max(GameTest.MAX_SEEDS, GameTest.MAX_HOUSES))
            .array(int[].class)
            .ofSize(2)
            .filter(arr -> arr[0] <= GameTest.MAX_HOUSES && arr[1] <= GameTest.MAX_SEEDS)
            .map(arr -> new Args(arr[0], arr[1]))
            .filter(args -> newGamePreconditions(args.houses, args.seeds))
            .map(args -> new Game(args.houses, args.seeds))
            .ignoreException(IllegalArgumentException.class);
    };
}
