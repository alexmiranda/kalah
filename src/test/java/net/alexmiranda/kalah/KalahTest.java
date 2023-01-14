package net.alexmiranda.kalah;

import static net.alexmiranda.kalah.Kalah.backwardSequence;
import static net.alexmiranda.kalah.Kalah.everyHouse;
import static net.alexmiranda.kalah.Kalah.forwardSequence;
import static net.alexmiranda.kalah.Kalah.oppositeOf;
import static net.alexmiranda.kalah.Kalah.plan;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

public class KalahTest {
    @ParameterizedTest
    @CsvSource(textBlock = """
        1,'A1 B1'
        2,'A1 A2 B1 B2'
        3,'A1 A2 A3 B1 B2 B3'
    """)
    public void testEveryHouse(int n, String expectedHouses) {
        var houses = everyHouse(n);
        assertThat(houses).containsExactlyInAnyOrder(expectedHouses.split(" "));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        1,'A','A1'
        1,'B','B1'
        2,'A','A1 A2'
        3,'B','B1 B2 B3'
        4,'A','A1 A2 A3 A4'
    """)
    public void testEveryHouseOfPlayer(int n, String player, String expectedHouses) {
        var p = Enum.valueOf(Player.class, player);
        var houses = everyHouse(p, n);
        assertThat(houses).containsExactlyInAnyOrder(expectedHouses.split(" "));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        1,'A1','B1'
        1,'B1','A1'
        2,'A1','B2'
        2,'B1','A2'
        3,'A1','B3'
        3,'A2','B2'
        3,'A3','B1'
    """)
    public void testOppositeOf(int n, String position, String opposite) {
        assertThat(oppositeOf(position, n)).isEqualTo(opposite);
        assertThat(oppositeOf(opposite, n)).isEqualTo(position);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/plan.csv")
    public void testPlan(String start, int houses, int seeds, String expectedEnd) {
        var end = plan(start, houses, seeds);
        assertThat(end).isEqualTo(expectedEnd);
    }

    @Test
    public void testForwardBackwardSequence() {
        int houses = 6;
        for (Player p : Player.values()) {
            var start = p.house(1);
            var counterClockwisePositions = forwardSequence(start, houses)
                .takeWhile(pos -> !pos.equals(start))
                .collect(Collectors.toList());

            var clockwisePositions = backwardSequence(start, houses)
                .takeWhile(pos -> !pos.equals(start))
                .collect(Collectors.toList());
            
            var reversed = new ArrayList<>(clockwisePositions);
            Collections.reverse(reversed);

            assertThat(counterClockwisePositions).hasSize(houses * 2);
            assertThat(clockwisePositions).hasSameSizeAs(counterClockwisePositions);
            assertThat(counterClockwisePositions).doesNotContain(p.opponent().store());
            assertThat(clockwisePositions).doesNotContain(p.opponent().store());
            assertThat(counterClockwisePositions).containsExactlyElementsOf(reversed);

            assertThat(counterClockwisePositions).containsExactly(
                p.house(2),
                p.house(3),
                p.house(4),
                p.house(5),
                p.house(6),
                p.store(),
                p.opponent().house(1),
                p.opponent().house(2),
                p.opponent().house(3),
                p.opponent().house(4),
                p.opponent().house(5),
                p.opponent().house(6)
            );
        }
    }
}
