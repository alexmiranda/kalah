package net.alexmiranda.kalah;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Negative;
import net.jqwik.api.constraints.Positive;

public class HouseTest {
    @Property
    public void testCannotHaveHouseWithNegativeSeeds(@ForAll @Negative int seeds, @ForAll Player player) {
        assertThatThrownBy(() -> new House(seeds, player))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    public void testCannotHaveHouseWithoutPlayer(@ForAll @Positive int seeds) {
        assertThatThrownBy(() -> new House(seeds, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    public void testLinkToNextHouse(@ForAll @Positive int seeds, @ForAll Player player, @ForAll @Positive int nextSeeds, @ForAll Player nextPlayer) {
        var house = new House(seeds, player);
        var next = new House(nextSeeds, nextPlayer);
        house.followedBy(next);
        assertThat(house.next()).isEqualTo(next);
    }

    @Property
    public void testSelectEmptyHouse(@ForAll Player player) {
        var house = new House(0, player);
        Assume.that(house.isEmpty());

        assertThatThrownBy(() -> house.select(player))
            .isInstanceOf(HouseEmptyException.class);

        assertThat(house).matches(House::isEmpty);
    }

    @Property
    public void testSelectNonEmptyHouseWithRightPlayer(@ForAll @IntRange(min = 1) int seeds, @ForAll Player player) {
        var house = new House(seeds, player);
        Assume.that(house.isNotEmpty());
        
        house.select(player);

        assertThat(house).matches(House::isEmpty);
    }

    @Property
    public void testSelectNonEmptyHouseWithWrongPlayer(@ForAll @IntRange(min = 1) int seeds, @ForAll Player player) {
        var house = new House(seeds, player);
        Assume.that(house.isNotEmpty());

        int seedsBefore = house.seeds();
        var opponent = player.opponent();
        assertThatThrownBy(() -> house.select(opponent))
            .isInstanceOf(OpponentHouseException.class);

        assertThat(house.seeds()).isEqualTo(seedsBefore);
    }

    @Property
    public void testTakeOnlyOneSeed(@ForAll("houses") House house, @ForAll Player player) {
        int seedsBefore = house.seeds();
        house.take(player, 1);
        assertThat(house.seeds()).isEqualTo(seedsBefore + 1);
    }

    @Property
    public void testTakeMoreThanOneSeed(@ForAll("houses") House house, @ForAll Player player, @ForAll @IntRange(min = 2) int toTake) {
        int seedsBefore = house.seeds();
        assertThatThrownBy(() -> house.take(player, toTake)).isInstanceOf(AssertionError.class);
        assertThat(house.seeds()).isEqualTo(seedsBefore);
    }

    @Property
    public void testTakeNegativeNumbers(@ForAll("houses") House house, @ForAll Player player, @ForAll @Negative int toTake) {
        int seedsBefore = house.seeds();
        assertThatThrownBy(() -> house.take(player, toTake)).isInstanceOf(AssertionError.class);
        assertThat(house.seeds()).isEqualTo(seedsBefore);
    }

    @Property
    public void testYieldWithRightPlayer(@ForAll @IntRange(min = 1) int seeds, @ForAll Player player) {
        var house = new House(seeds, player);
        Assume.that(house.isNotEmpty());
        house.yield(player);
        assertThat(house).matches(House::isEmpty);
    }

    @Property
    public void testYieldWithWrongPlayer(@ForAll @IntRange(min = 1) int seeds, @ForAll Player player) {
        var house = new House(seeds, player);
        Assume.that(house.isNotEmpty());

        int seedsBefore = house.seeds();
        var opponent = player.opponent();
        assertThatThrownBy(() -> house.yield(opponent))
            .isInstanceOf(OpponentHouseException.class);

        assertThat(house.seeds()).isEqualTo(seedsBefore);
    }

    @Property
    public void testCaptureWithRightPlayer(@ForAll @IntRange(min = 1) int seeds, @ForAll Player player, @ForAll @Positive int opponentSeeds) {
        var house = new House(seeds, player);
        var other = new House(opponentSeeds, player.opponent());
        house.oppositeTo(other);
        Assume.that(house.isNotEmpty());
        
        house.capture(player);

        assertThat(house).matches(House::isEmpty);
        assertThat(other).matches(House::isEmpty);
    }

    @Property
    public void testCaptureWithWrongPlayer(@ForAll @IntRange(min = 1) int seeds, @ForAll Player player) {
        var house = new House(seeds, player);
        Assume.that(house.isNotEmpty());

        var opponent = player.opponent();
        int seedsBefore = house.seeds();
        assertThatThrownBy(() -> house.capture(opponent))
            .isInstanceOf(HouseCaptureException.class);

        assertThat(house.seeds()).isEqualTo(seedsBefore);
    }

    @Provide
    public Arbitrary<House> houses(@ForAll @Positive int seeds, @ForAll Player player) {
        var house = new House(seeds, player);
        return Arbitraries.just(house);
    }
}
