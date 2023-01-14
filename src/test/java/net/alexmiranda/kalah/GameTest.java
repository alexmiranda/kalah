package net.alexmiranda.kalah;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static net.alexmiranda.kalah.Kalah.backwardSequence;
import static net.alexmiranda.kalah.Kalah.everyHouse;
import static net.alexmiranda.kalah.Kalah.forwardSequence;
import static net.alexmiranda.kalah.Kalah.oppositeOf;
import static net.alexmiranda.kalah.Kalah.plan;
import static net.alexmiranda.kalah.TestSupport.sum;
import static net.alexmiranda.kalah.TestSupport.translatePosition;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.RandomDistribution;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Negative;

public class GameTest {
    static final int MAX_HOUSES = 10;
    static final int MAX_SEEDS = 12;
    static final int MAX_ROUNDS = 5;

    @Property
    public void testNewGameAfterCreated(@ForAll(supplier = NewGameSupplier.class) Game game) {
        var houses = game.houses();
        var seeds = game.seeds();
        var length = houses * 2 + 2;
        var pits = game.pits();

        assertThat(pits).hasSize(length);
        assertThat(pits).containsEntry(Player.A.store(), 0);
        assertThat(pits).containsEntry(Player.B.store(), 0);
        assertThat(everyHouse(houses)).allSatisfy(pos -> {
            assertThat(pits).containsEntry(pos, seeds);
        });
    }

    @Property
    public void testCannotPlayOnUnexistingPosition(@ForAll(supplier = NewGameSupplier.class) Game game, @ForAll("invalidPositions") String position) {
        assertThatThrownBy(() -> game.play(position))
            .isInstanceOf(NoSuchPositionException.class);
    }

    @Property
    public void testCannotPlayOnStore(@ForAll(supplier = NewGameSupplier.class) Game game, @ForAll Player player) {
        assertThatThrownBy(() -> game.play(player.store()))
            .isInstanceOf(CannotPlayOnStoreException.class);
    }

    @Property
    public void testCannotPlayOnEmptyHouse(@ForAll("boards") int[] board, @ForAll Player player, @ForAll @IntRange(min = 1, max = MAX_HOUSES) int n) {
        ensureEmptyHouse(board, player, n);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());

        String position = player.house(n);
        assertThatThrownBy(() -> game.play(position))
            .isInstanceOf(HouseEmptyException.class);
    }

    @Property
    public void testCannotAcceptNegativePits(@ForAll("boards") int board[], @ForAll @Negative int negativeValue) {
        int index = Arbitraries.integers().between(0, board.length - 1).sample();
        board[index] = negativeValue;

        assertThatThrownBy(() -> new Game(board, GameState.WAITING, Player.A))
            .isInstanceOf(NegativeSeedsException.class);
    }

    @Property
    public void testPlayEndsOnOwnHouse(@ForAll("boards") int[] board, @ForAll Player player, @ForAll @IntRange(min = 1, max = MAX_HOUSES - 1) int n) {
        String position = player.house(n);
        ensurePlaySameRow(board, player, n);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());

        withInvariants(game, () -> {
            var before = game.pits();
            int seeds = before.get(position);
            String endPos = plan(position, game.houses(), seeds);
            Assume.that(before.get(endPos) > 0);

            game.play(position);
            var after = game.pits();

            assertThat(before.get(position)).isGreaterThan(0);
            assertThat(after.get(position)).isZero();
            assertThat(game.player()).isEqualTo(player.opponent());
            forwardSequence(position, game.houses()).limit(seeds).forEach(pos -> {
                assertThat(after.get(pos)).isEqualTo(before.get(pos) + 1);
            });
        });
    }

    @Property
    public void testPlayEndsOnEmptyHouse(@ForAll("boards") int[] board, @ForAll Player player, @ForAll @IntRange(min = 1, max = MAX_HOUSES - 1) int n) {
        ensurePlaySameRow(board, player, n);
        ensureLandsOnEmptyHouse(board, player, n);
        ensureOpponentHasAtLeastTwoHousesWithSeeds(board, player);
        ensureStartsWithTwoSeedsAtLeastOrAnotherPitMustHaveSeeds(board, player, n);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());
        
        withInvariants(game, () -> {
            String position = player.house(n);

            var before = game.pits();
            int seeds = before.get(position);
            game.play(position);
            var after = game.pits();

            assertThat(before.get(position)).isGreaterThan(0);
            assertThat(after.get(position)).isZero();
            assertThat(game.player()).isEqualTo(player.opponent());
            forwardSequence(position, game.houses()).limit(seeds - 1).forEach(pos -> {
                assertThat(after.get(pos)).isEqualTo(before.get(pos) + 1);
            });

            var endPos = plan(position, game.houses(), seeds);
            var captured = oppositeOf(endPos, game.houses());
            assertThat(after.get(endPos)).isZero();
            assertThat(after.get(captured)).isZero();

            var movedIntoStore = 1 + before.get(endPos) + before.get(captured);
            assertThat(after.get(player.store())).isEqualTo(before.get(player.store()) + movedIntoStore);
        });
    }

    @Property
    public void testPlayEndsOnStore(@ForAll("boards") int[] board, @ForAll Player player, @ForAll @IntRange(min = 1, max = MAX_HOUSES) int n) {
        String position = player.house(n);
        ensurePlaySameRow(board, player, n);
        ensureLandsOnStore(board, player, n);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());

        withInvariants(game, () -> {
            var before = game.pits();
            int seeds = before.get(position);
            game.play(position);
            var after = game.pits();

            assertThat(before.get(position)).isGreaterThan(0);
            assertThat(after.get(position)).isZero();
            assertThat(game.player()).isEqualTo(player);
            forwardSequence(position, game.houses()).limit(seeds).forEach(pos -> {
                assertThat(after.get(pos)).isEqualTo(before.get(pos) + 1);
            });
        });
    }

    @Property 
    public void testPlayEndsOnOpponentHouse(@ForAll("boards") int[] board, @ForAll Player player, @ForAll @IntRange(min = 1, max = MAX_HOUSES) int n) {
        String position = player.house(n);
        ensureLandsOnOpponentHouse(board, player, n);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());

        withInvariants(game, () -> {
            var before = game.pits();
            int seeds = before.get(position);
            game.play(position);
            var after = game.pits();

            Assume.that(!game.isOver());
            assertThat(after.get(position)).isZero();
            assertThat(game.player()).isEqualTo(player.opponent());
            forwardSequence(position, game.houses()).limit(seeds).forEach(pos -> {
                assertThat(after.get(pos)).isEqualTo(before.get(pos) + 1);
            });
        });
    }

    @Property
    public void testPlayEndsOnEarlierHouse(@ForAll("boards") int[] board, @ForAll Player player, @ForAll @IntRange(min = 2, max = MAX_HOUSES) int n) {
        String position = player.house(n);
        ensureLandsOnEarlierHouse(board, player, n);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());

        withInvariants(game, () -> {
            var before = game.pits();
            int seeds = before.get(position);
            String endPos = plan(position, game.houses(), seeds);
            Assume.that(before.get(endPos) > 0);

            game.play(position);
            var after = game.pits();

            assertThat(before.get(position)).isGreaterThan(0);
            assertThat(after.get(position)).isZero();
            assertThat(game.player()).isEqualTo(player.opponent());
            forwardSequence(position, game.houses()).limit(seeds).forEach(pos -> {
                assertThat(after.get(pos)).isEqualTo(before.get(pos) + 1);
            });
        });
    }

    @Property
    public void testPlayEndsOnEarlierEmptyHouse(@ForAll("boards") int[] board, @ForAll Player player, @ForAll @IntRange(min = 2, max = MAX_HOUSES) int n) {
        String position = player.house(n);
        ensureLandsOnEarlierHouse(board, player, n);
        ensureLandsOnEmptyHouse(board, player, n);
        ensureOpponentHasAtLeastTwoHousesWithSeeds(board, player);
        ensurePlayerHasTwoHousesWithSeedsBesidesTheFirstOne(board, player);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());

        withInvariants(game, () -> {
            var before = game.pits();
            int seeds = before.get(position);
            var endPos = plan(position, game.houses(), seeds);
            Assume.that(before.get(endPos) == 0);

            game.play(position);
            var after = game.pits();

            assertThat(after.get(position)).isZero();
            assertThat(game.player()).isEqualTo(player.opponent());

            var storePos = player.store();
            var capturedPos = oppositeOf(endPos, game.houses());

            forwardSequence(position, game.houses())
                .limit(seeds - 1)
                .filter(pos -> !pos.equals(storePos) && !pos.equals(capturedPos))
                .forEach(pos -> {
                    assertThat(after.get(pos)).isEqualTo(before.get(pos) + 1);
                });

            assertThat(before.get(endPos)).isZero();
            assertThat(after.get(endPos)).isZero();
            assertThat(after.get(capturedPos)).isZero();

            int addedToStore = 1;
            int addedToCaptured = 1;
            int addedToEndPos = 1;
            int priorInCaptured = before.get(capturedPos);

            var movedIntoStore = addedToStore + addedToCaptured + addedToEndPos + priorInCaptured;
            assertThat(after.get(storePos)).isEqualTo(before.get(storePos) + movedIntoStore);
        });
    }

    @Property
    public void testPlayEndsOnSameHouse(@ForAll("boards") int[] board, @ForAll Player player, @ForAll @IntRange(min = 1, max = MAX_HOUSES) int n) {
        String position = player.house(n);
        ensureLandsOnSameHouse(board, player, n);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());

        withInvariants(game, () -> {
            var before = game.pits();
            int seeds = before.get(position);
            game.play(position);
            var after = game.pits();

            assertThat(before.get(position)).isGreaterThan(0);
            assertThat(after.get(position)).isZero();
            assertThat(game.player()).isEqualTo(player.opponent());

            var storePos = player.store();
            var capturedPos = oppositeOf(position, game.houses());

            forwardSequence(position, game.houses())
                .limit(seeds - 1)
                .filter(pos -> !pos.equals(storePos) && !pos.equals(capturedPos))
                .forEach(pos -> {
                    assertThat(after.get(pos)).isEqualTo(before.get(pos) + 1);
                });

            assertThat(after.get(position)).isZero();
            assertThat(after.get(capturedPos)).isZero();

            int addedToStore = 1;
            int addedToCaptured = 1;
            int addedToItself = 1;
            int priorInCaptured = before.get(capturedPos);

            var movedIntoStore = addedToStore + addedToCaptured + addedToItself + priorInCaptured;
            assertThat(after.get(storePos)).isEqualTo(before.get(storePos) + movedIntoStore);
        });
    }

    @Property
    public void testGameOverAfterLastSeedMovedIntoStore(@ForAll("boards") int[] board, @ForAll Player player) {
        ensureOnlyOneSeedLeftBeforeGameIsOver(board, player);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());
        
        withInvariants(game, () -> {
            int houses = (board.length - 2) / 2;
            String position = player.house(houses);
            String storePos = player.store();

            var before = game.pits();
            game.play(position);
            var after = game.pits();
            
            assertThat(game.isOver()).isTrue();
            assertThat(after.get(storePos)).isEqualTo(before.get(storePos) + before.get(position));

            Player opponent = player.opponent();
            int otherPlayerSeeds = everyHouse(opponent, houses).mapToInt(before::get).sum();
            assertThat(after.get(opponent.store())).isEqualTo(before.get(opponent.store()) + otherPlayerSeeds);

            everyHouse(houses).forEach(pos -> {
                assertThat(after.get(pos)).isZero();
            });
        });
    }

    @Property
    public void testGameOverAfterLastSeedsMovedIntoOpponentRow(@ForAll("boards") int[] board, @ForAll Player player) {
        ensureRemainingSeedsWillEndGameOnOpponentRow(board, player);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());
        
        withInvariants(game, () -> {
            int houses = (board.length - 2) / 2;
            String position = player.house(houses);
            String storePos = player.store();

            var before = game.pits();
            int seeds = before.get(position);
            game.play(position);
            var after = game.pits();
            
            assertThat(game.isOver()).isTrue();
            assertThat(after.get(storePos)).isEqualTo(before.get(storePos) + 1);

            Player opponent = player.opponent();
            int otherPlayerSeeds = everyHouse(opponent, houses).mapToInt(before::get).sum() + seeds - 1;
            assertThat(after.get(opponent.store())).isEqualTo(before.get(opponent.store()) + otherPlayerSeeds);

            everyHouse(houses).forEach(pos -> {
                assertThat(after.get(pos)).isZero();
            });
        });
    }

    @Property
    public void testGameOverAfterLastOpponentHouseIsCaptured(@ForAll("boards") int[] board, @ForAll Player player, @ForAll @IntRange(min = 1, max = MAX_HOUSES - 1) int n) {
        ensurePlaySameRow(board, player, n);
        ensureLandsOnEmptyHouse(board, player, n);
        ensureOpponentHasOnlyOneHouseLeftBeforeGameOver(board, player, n);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());

        withInvariants(game, () -> {
            String position = player.house(n);

            var before = game.pits();
            int seeds = before.get(position);
            String endPos = plan(position, game.houses(), seeds);
            String capturedPos = oppositeOf(endPos, game.houses());
            Assume.that(before.get(endPos).equals(0));

            game.play(position);
            var after = game.pits();

            assertThat(game.isOver()).isTrue();
            everyHouse(game.houses()).forEach(pos -> {
                assertThat(after.get(pos)).isZero();
            });

            int playerSeeds = everyHouse(player, game.houses()).mapToInt(before::get).sum();
            int capturedSeeds = before.get(capturedPos);
            String playerStore = player.store();
            String opponentStore = player.opponent().store();

            assertThat(after.get(playerStore)).isEqualTo(before.get(playerStore) + playerSeeds + capturedSeeds);
            assertThat(after.get(opponentStore)).isEqualTo(before.get(opponentStore));
        });
    }

    @Property
    public void testGameOverAfterLandingOnFirstHouseAndOpponentHouseIsCaptured(@ForAll("boards") int[] board, @ForAll Player player) {
        int houses = (board.length - 2) / 2;
        ensureStartsOnLastAndEndsOnFirstHouse(board, player);
        ensureLandsOnEmptyHouse(board, player, houses);
        ensureNoSeedsBetweenFirstAndLastHouse(board, player);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());

        withInvariants(game, () -> {
            String position = player.house(houses);

            var before = game.pits();
            int seeds = before.get(position);
            String endPos = plan(position, game.houses(), seeds);
            String capturedPos = oppositeOf(endPos, game.houses());
            Assume.that(before.get(endPos).equals(0));

            game.play(position);
            var after = game.pits();

            assertThat(game.isOver()).isTrue();
            everyHouse(game.houses()).forEach(pos -> {
                assertThat(after.get(pos)).isZero();
            });
            
            Player opponent = player.opponent();
            String playerStore = player.store();
            String opponentStore = opponent.store();
            int playerSeeds = seeds;
            int capturedSeeds = before.get(capturedPos) + 1;
            int sownOverOpponentRow = houses;
            int opponentSeeds = everyHouse(opponent, game.houses()).mapToInt(before::get).sum();

            assertThat(after.get(playerStore)).isEqualTo(before.get(playerStore) + playerSeeds - sownOverOpponentRow + capturedSeeds);
            assertThat(after.get(opponentStore)).isEqualTo(before.get(opponentStore) + opponentSeeds + sownOverOpponentRow - capturedSeeds);
        });
    }

    @Property
    public void testPlayFullRoundsAroundBoard(@ForAll("boards") int[] board, @ForAll Player player, @ForAll @IntRange(min = 1, max = MAX_HOUSES) int n, @ForAll @IntRange(min = 1, max = MAX_ROUNDS) int rounds) {
        ensureNumberOfRounds(board, player, n, rounds);
        var game = new Game(board, GameState.WAITING, player);
        Assume.that(!game.isOver());

        withInvariants(game, () -> {
            var before = game.pits();
            String startPos = player.house(n);
            int seeds = before.get(startPos);
            String endPos = plan(startPos, game.houses(), seeds);
            int seedsPerRound = game.houses() * 2 + 1;
            int actualRounds = seeds / seedsPerRound;
            int totalSeedsRounds = actualRounds * seedsPerRound;

            game.play(startPos);
            var after = game.pits();

            assertThat(after.get(startPos)).isEqualTo(actualRounds);
            forwardSequence(startPos, game.houses())
                .limit(seeds - totalSeedsRounds)
                .forEach(pos -> {
                    assertThat(after.get(pos)).isEqualTo(before.get(pos) + actualRounds + 1);
                });
            
            backwardSequence(startPos, game.houses())
                .takeWhile(pos -> !pos.equals(endPos))
                .forEach(pos -> {
                    assertThat(after.get(pos)).isEqualTo(before.get(pos) + actualRounds);
                });
            
            String opponentStore = player.opponent().store();
            assertThat(after.get(opponentStore)).isEqualTo(before.get(opponentStore));
        });
    }

    private void withInvariants(Game game, Runnable test) {
        int h = game.houses();
        int s = game.seeds();
        int seedsBefore = sum(game.pits());
        boolean wasOver = game.isOver();
        
        test.run();

        assertThat(game.houses()).isEqualTo(h);
        assertThat(game.seeds()).isEqualTo(s);
        assertThat(sum(game.pits())).isEqualTo(seedsBefore);
        if (wasOver) {
            assertThat(game.isOver()).isTrue();
        }
    }

    private void ensureEmptyHouse(int[] board, Player player, int n) {
        int houses = (board.length - 2) / 2;
        Assume.that(n <= houses);

        int positionIndex = translatePosition(player.house(n), houses);
        int storeIndex = translatePosition(player.store(), houses);
        board[storeIndex] += board[positionIndex];
        board[positionIndex] = 0;
    }
    
    private void ensurePlaySameRow(int[] board, Player player, int n) {
        String position = player.house(n);
        int houses = (board.length -2) / 2;
        Assume.that(n < houses);

        int i = translatePosition(position, houses);
        Assume.that(board[i] > 0);

        int diff = n + board[i] - houses;
        if (diff <= 0) { // possible to play in the same row
            return;
        }

        // not possible to play in the same row, substract seeds so that is terminates in the last store
        int s = translatePosition(player.store(), houses);
        board[i] -= diff;
        board[s] += diff;
    }

    private void ensureLandsOnEmptyHouse(int[] board, Player player, int n) {
        int houses = (board.length - 2) / 2;
        String startPos = player.house(n);
        int startIndex = translatePosition(startPos, houses);
        String endPos = plan(startPos, houses, board[startIndex]);
        int endIndex = translatePosition(endPos, houses);
        if (board[endIndex] != 0) {
            int storeIndex = translatePosition(player.store(), houses);
            board[storeIndex] += board[endIndex];
            board[endIndex] = 0;
        }
    }
    
    private void ensureLandsOnStore(int[] board, Player player, int n) {
        String startPos = player.house(n);
        int houses = (board.length - 2) / 2;
        int startIndex = translatePosition(startPos, houses);
        int storePos = translatePosition(player.store(), houses);
        int seeds = board[startIndex];
        int diff = startIndex + seeds - storePos;
        if (diff > 0) {
            board[startIndex] -= diff;
            board[storePos] += diff;
        } else if (diff < 0) {
            board[startIndex] -= diff;
            board[storePos] += houses * 2 + diff;
        }
    }

    private void ensureLandsOnOpponentHouse(int[] board, Player player, int n) {
        String startPos = player.house(n);
        int houses = (board.length - 2) / 2;
        Assume.that(n <= houses);

        int startIndex = translatePosition(startPos, houses);
        int seeds = board[startIndex];

        int min = houses - n + 2;
        int max = (houses * 2) - n + 1;
        if (seeds >= min && seeds <= max) {
            return;
        }

        int storeIndex = translatePosition(player.store(), houses);
        if (seeds < min) {
            int diff = min - seeds;
            board[startIndex] += diff;
            board[storeIndex] += houses * 2 - diff;
        } else if (seeds > max) {
            int diff = seeds - max;
            board[startIndex] -= diff;
            board[storeIndex] += diff;
        }
    }

    private void ensureLandsOnEarlierHouse(int[] board, Player player, int n) {
        String startPos = player.house(n);
        int houses = (board.length - 2) / 2;
        Assume.that(n > 1 && n <= houses);

        int startIndex = translatePosition(startPos, houses);
        int seeds = board[startIndex];

        int min = (houses * 2) - n + 2;
        int max = (houses * 2);
        if (seeds >= min && seeds <= max) {
            return;
        }

        int storeIndex = translatePosition(player.store(), houses);
        if (seeds < min) {
            int diff = min - seeds;
            board[startIndex] += diff;
            board[storeIndex] += houses * 2 - diff;
        } else if (seeds > max) {
            int diff = seeds - max;
            board[startIndex] -= diff;
            board[storeIndex] += diff;
        }
    }

    private void ensureLandsOnSameHouse(int[]board, Player player, int n) {
        int houses = (board.length - 2) / 2;
        Assume.that(n <= houses);
        
        String startPos = player.house(n);
        int startIndex = translatePosition(startPos, houses);
        int seeds = board[startIndex];
        
        int target = houses * 2 + 1;
        if (seeds == target) {
            return;
        }

        int storeIndex = translatePosition(player.store(), houses);
        if (seeds < target) {
            int diff = target - seeds;
            board[startIndex] += diff;
            board[storeIndex] += houses * 2 - diff;
        } else if (seeds > target) {
            int diff = seeds - target;
            board[startIndex] -= diff;
            board[storeIndex] += diff;
        }
    }

    private void ensureOnlyOneSeedLeftBeforeGameIsOver(int[] board, Player player) {
        int houses = (board.length - 2) / 2;
        int firstHousePos = translatePosition(player.house(1), houses);
        int storePos = translatePosition(player.store(), houses);
        int lastHousePos = storePos - 1;

        board[storePos] += board[lastHousePos] - 1;
        board[lastHousePos] = 1;

        for (int i = firstHousePos; i < lastHousePos; i++) {
            board[storePos] += board[i];
            board[i] = 0;
        }
    }

    private void ensureRemainingSeedsWillEndGameOnOpponentRow(int[] board, Player player) {
        int houses = (board.length - 2) / 2;
        int firstHousePos = translatePosition(player.house(1), houses);
        int storePos = translatePosition(player.store(), houses);
        int lastHousePos = storePos - 1;

        for (int i = firstHousePos; i < lastHousePos; i++) {
            board[storePos] += board[i];
            board[i] = 0;
        }

        int min = 2;
        int max = houses + 1;
        int seeds = board[lastHousePos];
        if (seeds >= min && seeds <= max) {
            return;
        }

        if (seeds < min) {
            int diff = min - seeds;
            board[lastHousePos] += diff;
            board[storePos] += (houses * 2) - diff;
        }

        if (seeds > max) {
            int diff = seeds - max;
            board[lastHousePos] -= diff;
            board[storePos] += diff;
        }
    }

    private void ensureOpponentHasOnlyOneHouseLeftBeforeGameOver(int board[], Player player, int n) {
        int houses = (board.length - 2) / 2;
        Assume.that(n < houses);

        String startPos = player.house(n);
        int startIndex = translatePosition(startPos, houses);
        int seeds = board[startIndex];
        
        String endPos = plan(startPos, houses, seeds);
        int endIndex = translatePosition(endPos, houses);
        Assume.that(board[endIndex] == 0);

        String capturedPos = oppositeOf(endPos, houses);
        int capturedIndex = translatePosition(capturedPos, houses);
        Assume.that(board[capturedIndex] > 0);

        Player opponent = player.opponent();
        int opponentStoreIndex = translatePosition(opponent.store(), houses);
        everyHouse(opponent, houses).filter(pos -> !pos.equals(capturedPos))
            .mapToInt(pos -> translatePosition(pos, houses))
            .forEach(i -> {
                board[opponentStoreIndex] += board[i];
                board[i] = 0;
            });
    }

    private void ensureOpponentHasAtLeastTwoHousesWithSeeds(int[] board, Player player) {
        int houses = (board.length - 2) / 2;
        Assume.that(houses > 1);

        Player opponent = player.opponent();
        int opponentFirstHouseIndex = translatePosition(opponent.house(1), houses);
        int nonEmptyCount = 0;
        for (int i = 0; i < houses - 2; i++) {
            if (board[opponentFirstHouseIndex + i] > 0) {
                nonEmptyCount++;
            }
            if (nonEmptyCount == 2) {
                return;
            }
        }

        int diff = 2 - nonEmptyCount;
        int last = opponentFirstHouseIndex + houses;
        int lastButOne = last - 1;
        if (diff > 1 && board[lastButOne] == 0) {
            board[lastButOne] = houses * 2;
        }

        if (diff > 0 && board[last] == 0) {
            board[last] = houses * 2;
        }
    }

    private void ensurePlayerHasTwoHousesWithSeedsBesidesTheFirstOne(int[] board, Player player) {
        int houses = (board.length - 2) / 2;
        Assume.that(houses > 2);

        int firstHouseIndex = translatePosition(player.house(1), houses);
        int lastHouseIndex = translatePosition(player.house(houses), houses);
        int nonEmptyCount = 0;
        for (int i = firstHouseIndex + 1; i < lastHouseIndex - 2; i++) {
            if (board[i] > 0) {
                nonEmptyCount++;
            }
            if (nonEmptyCount == 2) {
                return;
            }
        }

        if (board[lastHouseIndex - 1] == 0) {
            board[lastHouseIndex - 1] = houses * 2;
            nonEmptyCount++;
        }

        if (nonEmptyCount < 2 && board[lastHouseIndex] == 0) {
            board[lastHouseIndex] = houses * 2;
            nonEmptyCount++;
        }
    }

    private void ensureStartsWithTwoSeedsAtLeastOrAnotherPitMustHaveSeeds(int[] board, Player player, int n) {
        int houses = (board.length - 2) / 2;
        Assume.that(n < houses);

        String startPos = player.house(n);
        int startIndex = translatePosition(startPos, houses);
        int seeds = board[startIndex];
        if (seeds > 1) {
            return;
        }

        String endPos = plan(startPos, houses, seeds);
        var anotherHouse = everyHouse(player, houses)
            .filter(pos -> !pos.equals(startPos) && !pos.equals(endPos))
            .findAny();
        
        Assume.that(anotherHouse.isPresent());
        anotherHouse.ifPresent(pos -> {
            int i = translatePosition(pos, houses);
            if (board[i] == 0) {
                board[i] = houses * 2;
            }
        });
    }

    private void ensureStartsOnLastAndEndsOnFirstHouse(int[] board, Player player) {
        int houses = (board.length - 2) / 2;
        int required = houses + 2;
        int lastHouseIndex = translatePosition(player.house(houses), houses);
        int storeIndex = translatePosition(player.store(), houses);

        if (board[lastHouseIndex] > required) {
            int diff = board[lastHouseIndex] - required;
            board[lastHouseIndex] = required;
            board[storeIndex] += diff;
        } else if (board[lastHouseIndex] < required) {
            int diff = required - board[lastHouseIndex];
            board[lastHouseIndex] = required;
            board[storeIndex] += houses * 2 - diff;
        }
    }

    private void ensureNoSeedsBetweenFirstAndLastHouse(int[] board, Player player) {
        int houses = (board.length - 2) / 2;
        Assume.that(houses > 1);

        int firstIndex = translatePosition(player.house(1), houses);
        int lastIndex = translatePosition(player.house(houses), houses);
        int storeIndex = translatePosition(player.store(), houses);
        for (int i = firstIndex + 1; i <= lastIndex - 1; i++) {
            board[storeIndex] += board[i];
            board[i] = 0;
        }
    }

    private void ensureNumberOfRounds(int[] board, Player player, int n, int rounds) {
        int houses = (board.length - 2) / 2;
        Assume.that(n <= houses);
        int startIndex = translatePosition(player.house(n), houses);
        int storeIndex = translatePosition(player.store(), houses);
        Assume.that(board[startIndex] > 0);

        int round = houses * 2 + 1;
        rounds += board[startIndex] / round;

        Assume.that(rounds >= 0);
        if (rounds == 0) {
            return;
        }

        board[startIndex] += rounds * round;
        int nn = rounds / (houses * 2) + 1;
        board[storeIndex] += (nn * houses * 2) - rounds;
    }

    @Provide
    public Arbitrary<int[]> boards(@ForAll @IntRange(min = 2, max = MAX_HOUSES) int houses, @ForAll @IntRange(min = 2, max = MAX_SEEDS) int seeds) {
        int totalHouses = houses * 2;
        int size = totalHouses + 2;
        int max = totalHouses * seeds;
        return Arbitraries.integers()
            .between(0, max)
            .shrinkTowards(seeds)
            .withDistribution(RandomDistribution.gaussian())
            .array(int[].class)
            .ofSize(size)
            .map(this::fixNumberOfSeeds);
    }

    @Provide
    @SuppressWarnings("unchecked")
    public Arbitrary<String> invalidPositions() {
        var housesThatDontExist = Combinators.combine(
            Arbitraries.of(Player.class), 
            Arbitraries.integers().greaterOrEqual(MAX_HOUSES + 1))
            .as((p, n) -> p.name() + n);
            
        var randomStrings = Arbitraries.strings().ofMaxLength(5)
        .filter(s -> {
            if (!s.startsWith("A") && !s.startsWith("B")) {
                return true;
            }
            return s.substring(1).chars().anyMatch(Character::isDigit);
        });
            
        return Arbitraries.oneOf(housesThatDontExist, randomStrings);
    }
        
    private int[] fixNumberOfSeeds(int[] board) {
        int totalHouses = board.length - 2;
        int sum = sum(board);
        Assume.that(sum > 0);
        int mod = sum % totalHouses;
        if (mod == 0) return board;
        int target = (sum / totalHouses + 1) * totalHouses;
        board[board.length - 1] += target - mod;
        return board;
    }
}
