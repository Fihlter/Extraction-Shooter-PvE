package com.nomuse.freecell.game;

public class PlayerEntity {

    public int id; // Network ID
    public float x = 0f, y = 2f, z = 5f; // Position
    public float yaw = 0f, pitch = 0f; // Look dir

    // Melee combat state
    public boolean isAttacking = false;
    public float attackTimer = 0f;
    public static final float ATTACK_DURATION = 0.3f;

    public PlayerEntity(int id) {
        this.id = id;
    }

    public void update(float deltaTime) {
        if (isAttacking) {
            attackTimer += deltaTime;
            if (attackTimer >= ATTACK_DURATION) {
                isAttacking = false;
                attackTimer = 0f;
            }
        }
    }

}
