package net.alexmiranda.kalah;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Kalah {
    public static Stream<String> everyHouse(int houses) {
        return IntStream.rangeClosed(1, houses)
            .boxed()
            .flatMap(n -> Stream.of(Player.A.house(n), Player.B.house(n)));
    }

    public static Stream<String> everyHouse(Player player, int houses) {
        return IntStream.rangeClosed(1, houses).mapToObj(n -> player.house(n));
    }

    public static String oppositeOf(String position, int houses) {
        var player = Player.valueOf(position.substring(0, 1));
        var opponent = player.opponent();
        int n = Integer.parseInt(position.substring(1));
        return opponent.house(houses - n + 1);
    }

    public static String plan(String start, int houses, int seeds) {
        Preconditions.check(houses > 0, "houses");
        Preconditions.check(seeds >= 0, "seeds");

        int n = Integer.parseInt(start.substring(1));
        Preconditions.check(n <= houses, "start");        

        String pref = start.substring(0, 1);
        Preconditions.check(pref.equals("A") || pref.equals("B"), "start");

        if (seeds == 0) {
            return start;
        }

        int pitsCount = houses * 2 + 2;
        int pitsToVisit = pitsCount - 1; // opponent store is skipped
        int mod = seeds % pitsToVisit;

        if (mod == 0) {
            return start;
        } else if (mod <= houses - n) {
            return pref + (n + mod);
        } else if (mod == houses - n + 1) {
            return "S" + pref;
        } else if (mod <= houses * 2 + 1 - n) {
            mod -= (houses + 1 - n);
            return switch (pref) {
                case "A" -> "B" + mod;
                case "B" -> "A" + mod;
                default -> throw new AssertionError("impossible state");
            };
        }

        return pref + (n + mod + 1 - pitsCount);
    }

    public static Stream<String> forwardSequence(String startPos, int houses) {
        var player = Player.valueOf(startPos.substring(0, 1));
        var opponent = player.opponent();

        return Stream.iterate(startPos, (prev) -> {
            if (prev.equals(player.house(houses))) {
                return player.store();
            } else if (prev.equals(player.store())) {
                return opponent.house(1);
            } else if (prev.equals(opponent.house(houses))) {
                return player.house(1);
            }

            String p = prev.substring(0, 1);
            int n = Integer.parseInt(prev.substring(1));
            return Player.valueOf(p).house(n + 1);
        }).skip(1);
    }

    public static Stream<String> backwardSequence(String startPos, int houses) {
        var player = Player.valueOf(startPos.substring(0, 1));
        var opponent = player.opponent();

        return Stream.iterate(startPos, (prev) -> {
            if (prev.equals(player.house(1))) {
                return opponent.house(houses);
            } else if (prev.equals(opponent.house(1))) {
                return player.store();
            } else if (prev.equals(player.store())) {
                return player.house(houses);
            }
            String p = prev.substring(0, 1);
            int n = Integer.parseInt(prev.substring(1));
            return Player.valueOf(p).house(n - 1);
        }).skip(1);
    }
}
