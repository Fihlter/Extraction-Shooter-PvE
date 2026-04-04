package com.nomuse.freecell.game;

import com.badlogic.gdx.math.MathUtils;

public class PlayerEntity {

    public int id; // Network ID
    public float x = 0f, y = 2f, z = 5f; // Position
    public float yaw = 0f, pitch = 0f; // Look dir

    // Melee combat state
    public boolean isAttacking = false;
    public float attackTimer = 0f;
    public float attackOffset = 0f;
    public static final float ATTACK_DURATION = 0.3f;

    public float yVelocity = 0f;
    public boolean isGrounded = true;

    public static final float GRAVITY = -25f;
    public static final float JUMP_FORCE = 9f;
    public static final float BASE_EYE_HEIGHT = 2f;

    public PlayerEntity(int id) {
        this.id = id;
    }

    public void update(float deltaTime) {
        if (isAttacking) {
            attackTimer += deltaTime;

            float progress = attackTimer / ATTACK_DURATION;
            attackOffset = MathUtils.sin(progress * MathUtils.PI) * 0.6f;

            if (attackTimer >= ATTACK_DURATION) {
                isAttacking = false;
                attackTimer = 0f;
                attackOffset = 0f;
            }
        }

        if (!isGrounded) {
            yVelocity += GRAVITY * deltaTime;
        }

        y += yVelocity * deltaTime;

        if (y <= BASE_EYE_HEIGHT) {
            y = BASE_EYE_HEIGHT;
            yVelocity = 0f;
            isGrounded = true;
        } else {
            isGrounded = false;
        }
    }

}
