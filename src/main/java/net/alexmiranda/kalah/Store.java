package net.alexmiranda.kalah;

import java.util.Optional;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
final class Store implements Pit {
    private final Player belongsTo;
    private int seeds;
    private House next;

    Store(Player belongsTo) {
        this(0, belongsTo);
    }

    Store(int seeds, Player belongsTo) {
        Preconditions.check(seeds >= 0, "seeds");
        Preconditions.check(belongsTo != null, "belongsTo");
        this.seeds = seeds;
        this.belongsTo = belongsTo;
    }

    @Override
    public int seeds() {
        return this.seeds;
    }

    @Override
    public Pit next() {
        return this.next;
    }

    void followedBy(House next) {
        this.next = next;
    }

    @Override
    public int select(Player player) {
        throw new CannotPlayOnStoreException();
    }

    @Override
    public int take(Player player, int seeds) {
        Preconditions.check(seeds >= 0, "seeds");
        if (player != this.belongsTo) {
            return this.seeds;
        }

        var existingSeeds = this.seeds;
        this.seeds += seeds;
        return existingSeeds;
    }

    Optional<Player> leader(Store other) {
        Preconditions.check(other != null, "other");
        if (this.seeds > other.seeds) {
            return Optional.of(this.belongsTo);
        } else if (other.seeds > this.seeds) {
            return Optional.of(other.belongsTo);
        }
        return Optional.empty();
    }
}
