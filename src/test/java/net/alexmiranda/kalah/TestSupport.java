package net.alexmiranda.kalah;

import java.util.Map;

class TestSupport {
    static boolean newGamePreconditions(int houses, int seeds) {
        if (houses < 1) {
            return false;
        }

        if (seeds < 1) {
            return false;
        }

        if (houses * seeds == 1) {
            return false;
        }
        
        try {
            Math.addExact(Math.multiplyExact(houses, 2), 2);
            Math.multiplyExact(houses, seeds);
        } catch (ArithmeticException e) {
            return false;
        }
        
        return true;
    }

    static int translatePosition(String position, int houses) {
        if (position.startsWith("A")) {
            return nth(position);
        } else if (position.equals("SA")) {
            return houses;
        } else if (position.startsWith("B")) {
            int n = nth(position);
            return n + houses + 1;
        } else if (position.equals("SB")) {
            return houses * 2 + 1;
        }
        throw new IllegalArgumentException("position");
    }

    private static int nth(String position) {
        return Integer.parseInt(position.substring(1)) - 1;
    }

    static int sum(int[] arr) {
        int sum = 0;
        for (int n : arr) {
            sum += n;
        }
        return sum;
    }

    static int sum(Map<String, Integer> pits) {
        int sum = 0;
        for (var i : pits.values()) {
            sum += i;
        }
        return sum;
    }
}
