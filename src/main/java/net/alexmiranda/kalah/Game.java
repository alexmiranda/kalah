package net.alexmiranda.kalah;

import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

public class Game {
    private final int houses;
    private final int seeds;

    private LinkedHashMap<String, Pit> pits;
    private Map<String, Integer> view;
    private Store storeA;
    private Store storeB;
    private GameState state = GameState.WAITING;
    private Player player = Player.A;
    private boolean isOver = false;

    public Game(int houses, int seeds) {
        Preconditions.check(houses > 0, "houses");
        Preconditions.check(seeds > 0, "seeds");
        Preconditions.check(houses > 1 || seeds > 1, "seeds");
        this.houses = houses;
        this.seeds = seeds;
        var board = createBoardPrototype(houses, seeds);
        init(board, false);
    }

    Game(int[] board, GameState state, Player player) {
        Preconditions.check(board != null, "board");
        Preconditions.check(state != null, "state");
        Preconditions.check(player != null, "player");
        Preconditions.check(board.length >= 4 && board.length % 2 == 0, "board");

        int totalSeeds = sum(board);
        int houses = board.length / 2 - 1;
        Preconditions.check(totalSeeds > 0, "board");
        Preconditions.check(totalSeeds % (houses * 2) == 0, "board");

        this.houses = houses;
        this.seeds = sum(board) / this.houses;
        this.state = state;
        this.player = player;
        init(board, true);
    }

    public void play(String position) {
        var pit = this.pits.get(position);
        if (pit != null) {
            this.state.beginTurn(this, pit);
            return;
        }
        throw new NoSuchPositionException(position);
    }

    public boolean isOver() {
        return this.isOver;
    }

    public Optional<Player> winner() {
        return this.state.winner(this);
    }

    public Map<String, Integer> pits() {
        if (this.view != null) {
            return view;
        }
        this.view = this.pits.entrySet()
            .stream()
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                    kv -> kv.getKey(),
                    kv -> kv.getValue().seeds(),
                    (x, y) -> y,
                    LinkedHashMap::new), Collections::unmodifiableMap));
        return view;
    }

    public int houses() {
        return this.houses;
    }

    public int seeds() {
        return this.seeds;
    }

    public Player player() {
        return this.player;
    }

    void accept(Pit pit, int seedsLeft) {
        this.view = null; // invalidate view
        Pit next = pit;
        while (seedsLeft > 0) {
            next = next.next();
            seedsLeft = this.state.visit(this, next, seedsLeft);
        }
        this.state.endTurn(this, next);
    }

    void setState(GameState state) {
        this.state = state;
    }

    void captureIntoStore(House house) {
        int seeds = house.capture(this.player);
        this.currentPlayerStore().take(this.player, seeds);
    }

    void switchPlayer() {
        this.player = this.player.opponent();
    }

    boolean checkGameOver() {
        return this.checkGameOver(false);
    }

    boolean checkGameOver(boolean checkBothPlayers) {
        return this.isOver || this.updateGameOver(this.player, checkBothPlayers);
    }

    Optional<Player> leadingPlayer() {
        return this.storeA.leader(this.storeB);
    }

    void terminate() {
        for (Player player : Player.values()) {
            Pit pit = this.pits.get(player.house(1));

            int seeds = 0;
            while (true) {
                if (pit instanceof House h) {
                    seeds += h.yield(player);
                } else if (pit instanceof Store s) {
                    s.take(player, seeds);
                    break;
                }
                pit = pit.next();
            }
        }
    }

    private Store currentPlayerStore() {
        return this.storeForPlayer(this.player);
    }

    private Store storeForPlayer(Player player) {
        return switch (player) {
            case A -> this.storeA;
            case B -> this.storeB;
        };
    }

    private boolean updateGameOver(Player player, boolean checkBothPlayers) {
        for (int i = 1; i <= this.houses; i++) {
            if (!this.pits.get(player.name() + i).isEmpty()) {
                if (checkBothPlayers) {
                    return updateGameOver(player.opponent(), false);
                }
                return false;
            }
        }

        return this.isOver = true;
    }

    private void init(int[] board, boolean checkState) {
        pits = new LinkedHashMap<>(board.length);

        int n = -1;
        var row1 = new Stack<House>();
        var a1 = new House(board[++n], Player.A);
        this.pits.put("A1", a1);
        row1.push(a1);

        var prev = a1;
        for (int i = 2; i <= this.houses; i++) {
            var house = new House(board[++n], Player.A);
            prev.followedBy(house);
            this.pits.put("A" + i, house);
            row1.push(house);
            prev = house;
        }

        storeA = new Store(board[++n], Player.A);
        prev.followedBy(storeA);
        this.pits.put("SA", storeA);

        var b1 = new House(board[++n], Player.B);
        this.pits.put("B1", b1);
        storeA.followedBy(b1);
        b1.oppositeTo(row1.pop());

        prev = b1;
        for (int i = 2; i <= this.houses; i++) {
            var house = new House(board[++n], Player.B);
            prev.followedBy(house);
            house.oppositeTo(row1.pop());
            this.pits.put("B" + i, house);
            prev = house;
        }

        storeB = new Store(board[++n], Player.B);
        prev.followedBy(storeB);
        storeB.followedBy(a1);
        this.pits.put("SB", storeB);

        if (checkState) {
            checkGameOver(true);
        }
    }

    private static int[] createBoardPrototype(int h, int s) {
        int[] board = new int[h * 2 + 2];
        for (int i = 0; i < h; i++) {
            board[i] = s;
            board[h + i + 1] = s;
        }
        return board;
    }

    private static int sum(int[] board) {
        return Arrays.stream(board).takeWhile(n -> {
            if (n >= 0) return true;
            throw new NegativeSeedsException();
        }).sum();
    }
}
