package com.nomuse.freecell.game;

public abstract class AmmoType {

    public String name;
    public float speed;
    public float directDamage;
    public float fireCooldown;
    public float radius;
    public boolean isInfinite;

    public AmmoType(String name, float speed, float directDamagem, float fireCooldown, float radius, boolean isInfinite) {
        this.name = name;
        this.speed = speed;
        this.directDamage = directDamage;
        this.fireCooldown = fireCooldown;
        this.radius = radius;
        this.isInfinite = isInfinite;
    }

    public abstract void onHit(EnemyEntity target);

}
