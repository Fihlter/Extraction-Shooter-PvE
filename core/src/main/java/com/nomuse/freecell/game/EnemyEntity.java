package com.nomuse.freecell.game;

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
            dx /= distance;
            dz /= distance;

            float moveX = dx * speed * deltaTime;
            float moveZ = dz * speed * deltaTime;

            if (!mapManager.isColliding(x + moveX, z, radius)) {
                x += moveX;
            }
            if (!mapManager.isColliding(x, z + moveZ, radius)) {
                z += moveZ;
            }
        }

        applySoftCollision(mapManager, allEnemies);
    }

    private void applySoftCollision(MapManager mapManager, Array<EnemyEntity> allEnemies) {
        for (EnemyEntity other : allEnemies) {
            if (other == this) continue;

            float diffX = x - other.x;
            float diffZ = z - other.z;
            float distanceSquared = (diffX * diffX) + (diffZ * diffZ);

            float combinedRadii = this.radius + other.radius;

            if ((distanceSquared < combinedRadii) && distanceSquared > 0.001f) {
                float distance = (float)Math.sqrt(distanceSquared);

                float overlap = combinedRadii - distance;

                float pushX = (diffX / distance) * (overlap * 0.5f);
                float pushZ = (diffZ / distance) * (overlap * 0.5f);

                if (!mapManager.isColliding(x + pushX, z, radius)) x += pushX;
                if (!mapManager.isColliding(x, z + pushZ, radius)) z += pushZ;
            }
        }
    }

}
