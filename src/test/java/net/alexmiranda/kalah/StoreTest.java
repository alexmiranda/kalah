package net.alexmiranda.kalah;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.Negative;
import net.jqwik.api.constraints.Positive;

public class StoreTest {
    @Property
    public void testCannotHaveStoreWithNegativeSeeds(@ForAll @Negative int seeds, @ForAll Player player) {
        assertThatThrownBy(() -> new Store(seeds, player))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    public void testCannotHaveStoreWithoutPlayer(@ForAll @Positive int seeds) {
        assertThatThrownBy(() -> new Store(seeds, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    public void testLinkToNextHouse(@ForAll @Positive int seeds, @ForAll Player player, @ForAll @Positive int nextSeeds, @ForAll Player nextPlayer) {
        var store = new Store(seeds, player);
        var next = new House(nextSeeds, nextPlayer);
        store.followedBy(next);
        assertThat(store.next()).isEqualTo(next);
    }

    @Property
    public void testSelectStore(@ForAll("stores") Store store, @ForAll Player player) {
        int seedsBefore = store.seeds();
        assertThatThrownBy(() -> store.select(player))
            .isInstanceOf(CannotPlayOnStoreException.class);
        assertThat(store.seeds()).isEqualTo(seedsBefore);
    }

    @Property
    public void testTakeSeedsFromCurrentPlayer(@ForAll @Positive int seeds, @ForAll Player player, @ForAll @Positive int toTake) {
        var store = new Store(seeds, player);
        int seedsBefore = store.seeds();
        store.take(player, toTake);
        assertThat(store.seeds()).isEqualTo(seedsBefore + toTake);
    }

    @Property
    public void testDoesNotTakeSeedsFromAnotherPlayer(@ForAll @Positive int seeds, @ForAll Player player, @ForAll @Positive int toTake) {
        var store = new Store(seeds, player);
        int seedsBefore = store.seeds();
        var opponent = player.opponent();
        store.take(opponent, toTake);
        assertThat(store.seeds()).isEqualTo(seedsBefore);
    }

    @Property
    public void testTakeNegativeNumbers(@ForAll("stores") Store store, @ForAll Player player, @ForAll @Negative int toTake) {
        int seedsBefore = store.seeds();
        assertThatThrownBy(() -> store.take(player, toTake)).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.seeds()).isEqualTo(seedsBefore);
    }

    @Property
    public void testLeadingPlayer(@ForAll @Positive int big, @ForAll @Positive int small, @ForAll Player player) {
        Assume.that(big > small);
        var leader = new Store(big, player);
        var behind = new Store(small, player.opponent());

        assertThat(leader.leader(behind)).hasValue(player);
        assertThat(behind.leader(leader)).hasValue(player);
        assertThat(leader.leader(leader)).isEmpty();
        assertThat(behind.leader(behind)).isEmpty();
    }

    @Property
    public void testLeadingPlayerDraw(@ForAll @Positive int score, @ForAll Player player) {
        var lhs = new Store(score, player);
        var rhs = new Store(score, player.opponent());

        assertThat(lhs.leader(rhs)).isEmpty();
        assertThat(rhs.leader(lhs)).isEmpty();
        assertThat(lhs.leader(lhs)).isEmpty();
        assertThat(rhs.leader(rhs)).isEmpty();
    }

    @Property
    public void testLeadingPlayerExpectsNonNull(@ForAll Player player) {
        var store = new Store(player);
        assertThatThrownBy(() -> store.leader(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    public void testTwoEqualStores(@ForAll @Positive int seeds, @ForAll Player player) {
        var a = new Store(seeds, player);
        var b = new Store(seeds, player);
        assertThat(a).isEqualTo(b);
    }
    
    @Property
    public void testTwoDifferentStores(
        @ForAll @Positive int aSeeds, @ForAll Player aPlayer,
        @ForAll @Positive int bSeeds, @ForAll Player bPlayer) {
            Assume.that(aSeeds != bSeeds || aPlayer != bPlayer);
            var a = new Store(aSeeds, aPlayer);
            var b = new Store(bSeeds, bPlayer);
            assertThat(a).isNotEqualTo(b);
    }

    @Provide
    public Arbitrary<Store> stores(@ForAll @Positive int seeds, @ForAll Player player) {
        var store = new Store(seeds, player);
        return Arbitraries.just(store);
    }
}
