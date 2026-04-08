package com.nomuse.freecell.game;

public class DefaultAmmo extends AmmoType {

    public DefaultAmmo() {
        // Name, speed, damage, cooldown, radius, hasInfiniteAmmo?
        super("Default", 18f, 15f, 0.15f, 0.4f, true);
    }

    @Override
    public void onHit(EnemyEntity target) {
        // No special on-hit effects to add
    }

}
