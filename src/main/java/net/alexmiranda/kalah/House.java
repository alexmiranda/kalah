package net.alexmiranda.kalah;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
final class House implements Pit {
    private final Player controlledBy;
    private int seeds;
    private Pit next;
    private House opposite;

    House(int seeds, Player controlledBy) {
        Preconditions.check(seeds >= 0, "seeds");
        Preconditions.check(controlledBy != null, "controlledBy");
        this.seeds = seeds;
        this.controlledBy = controlledBy;
    }
    
    @Override
    public int seeds() {
        return this.seeds;
    }

    @Override
    public Pit next() {
        return this.next;
    }

    public void followedBy(Pit next) {
        this.next = next;
    }
    
    void oppositeTo(House other) {
        assert this.controlledBy != other.controlledBy;
        this.opposite = other;
        other.opposite = this;
    }
    
    @Override
    public int select(Player player) {
        if (this.seeds == 0) {
            throw new HouseEmptyException();
        }
        return this.yield(player);
    }
    
    int yield(Player player) {
        if (player != this.controlledBy) {
            throw new OpponentHouseException();
        }
        int existingSeeds = this.seeds;
        this.seeds = 0;
        return existingSeeds;
    }

    int capture(Player player) {
        if (player != this.controlledBy) {
            throw new HouseCaptureException();
        }
        int existingSeeds = this.seeds + opposite.seeds;
        this.seeds = 0;
        opposite.seeds = 0;
        return existingSeeds;
    }

    @Override
    public int take(Player player, int seeds) {
        assert seeds == 1;
        var existingSeeds = this.seeds;
        this.seeds++;
        return existingSeeds;
    }
}
