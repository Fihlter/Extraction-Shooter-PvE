package com.nomuse.freecell.game;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Vector2;

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

    public EnemyEntity(int id, float startX, float startY, float startZ) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.z = startZ;
    }

    // Server-side Sim Logic
    public void update(float deltaTime, PlayerEntity targetPlayer, MapManager mapManager, Array<EnemyEntity> allEnemies) {

        if (!isGrounded) {
            yVelocity += PlayerEntity.GRAVITY * deltaTime;
        }

        y += yVelocity * deltaTime;

        if (y <= 1.5f) {
            y = 1.5f;
            yVelocity = 0f;
            isGrounded = true;
        } else {
            isGrounded = false;
        }

        float dx = targetPlayer.x - x;
        float dz = targetPlayer.z - z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);

        if (distance < aggroRange && distance > 1.2f) {
            float moveX = (dx / distance) * speed * deltaTime;
            float moveZ = (dz / distance) * speed * deltaTime;

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
                    moveX += (diffX / d) * force * speed * deltaTime;
                    moveZ += (diffZ / d) * force * speed * deltaTime;
                }
            }

            if (!mapManager.isColliding(x + moveX, z, radius)) {
                x += moveX;
            }
            if (!mapManager.isColliding(x, z + moveZ, radius)) {
                z += moveZ;
            }
        }
    }

}
