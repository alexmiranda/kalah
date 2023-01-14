package net.alexmiranda.kalah;

enum Player {
    A,
    B;

    Player opponent() {
        return switch (this) {
            case A -> B;
            case B -> A;
        };
    }

    public String house(int n) {
        Preconditions.check(n > 0, "n");
        return this.name() + n;
    }

    public String store() {
        return "S" + this.name();
    }
}
