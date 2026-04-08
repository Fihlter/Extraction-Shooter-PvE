package com.nomuse.freecell.game;

public class FireAmmo extends AmmoType {

    public FireAmmo() {
        super("Fire", 12f, 5f, 0.6f, 0.5f, false);
    }

    @Override
    public void onHit(EnemyEntity target) {
        target.applyBurn(4.0f, 8.0f);
    }

}
