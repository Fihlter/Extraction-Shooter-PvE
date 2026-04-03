package com.nomuse.freecell.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

public class EnemyEntity {

    public int id;
    public float x, y, z;
    public float radius = 0.5f;
    public float speed = 4f;

    // Physics
    public float yVelocity = 0f;
    public boolean isGrounded = true;

    // AI
    public float aggroRange = 15f;

    // Hit detection vars
    public float damageTimer = 0f;
    public static final float DAMAGE_DURATION = 0.5f;
    public float knockbackX = 0f;
    public float knockbackZ = 0f;

    public EnemyEntity(int id, float startX, float startY, float startZ) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.z = startZ;
    }

    public void takeHit(float sourceX, float sourceZ) {
        damageTimer = DAMAGE_DURATION;

        float dx = x - sourceX;
        float dz = z - sourceZ;
        float dist = (float)Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.001f) {
            knockbackX = (dx / dist) * 15f;
            knockbackZ = (dz / dist) * 15f;
        }
    }

    // Server-side Sim Logic
    public void update(float deltaTime, PlayerEntity targetPlayer, MapManager mapManager, Array<EnemyEntity> allEnemies) {

        if (damageTimer > 0) {
            damageTimer -= deltaTime;
            if (damageTimer < 0) damageTimer = 0f;
        }

        if (!isGrounded) {
            yVelocity += PlayerEntity.GRAVITY * deltaTime;
        }

        y += yVelocity * deltaTime;

        if (y <= 1.6f) {
            y = 1.6f;
            yVelocity = 0f;
            isGrounded = true;
        } else {
            isGrounded = false;
        }

        // Movement AI
        float dx = targetPlayer.x - x;
        float dz = targetPlayer.z - z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);

        float moveX = 0;
        float moveZ = 0;

        if (distance < aggroRange && distance > 1.2f) {
            moveX = (dx / distance) * speed;
            moveZ = (dz / distance) * speed;

            // Separation Force
            for (int i = 0; i < allEnemies.size; i++) {
                EnemyEntity other = allEnemies.get(i);
                if (other == this) continue;

                float diffX = x - other.x;
                float diffZ = z - other.z;
                float distSq = diffX * diffX + diffZ * diffZ;
                float personalSpace = 1.1f;

                if (distSq < personalSpace * personalSpace && distSq > 0) {
                    float d = (float)Math.sqrt(distSq);
                    float force = (personalSpace - d) / personalSpace;
                    moveX += (diffX / d) * force * speed;
                    moveZ += (diffZ / d) * force * speed;
                }
            }

            /*if (!mapManager.isColliding(x + moveX, z, radius)) {
                x += moveX;
            }
            if (!mapManager.isColliding(x, z + moveZ, radius)) {
                z += moveZ;
            }*/
        }
        // Apply knockback physics
        moveX += knockbackX;
        moveZ += knockbackZ;

        // Friction
        knockbackX = MathUtils.lerp(knockbackX, 0, deltaTime * 10f);
        knockbackZ = MathUtils.lerp(knockbackZ, 0, deltaTime * 10f);

        // Wall collision
        if (!mapManager.isColliding(x + moveX * deltaTime, z, radius)) {
            x += moveX * deltaTime;
        }
        if (!mapManager.isColliding(x, z + moveZ * deltaTime, radius)) {
            z += moveZ * deltaTime;
        }
    }

}
