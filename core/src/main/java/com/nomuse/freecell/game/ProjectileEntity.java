package com.nomuse.freecell.game;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class ProjectileEntity {
    public float x, y, z;
    public float vx, vy, vz;
    public float speed = 25f;
    public float damage = 25f;
    public float radius = 0.2f;
    public boolean isDead = false;
    public ModelInstance instance;

    public ProjectileEntity(ModelInstance instance, float startX, float startY, float startZ, Vector3 direction) {
        this.instance = instance;
        this.x = startX;
        this.y = startY;
        this.z = startZ;

        // Calculate velocity
        this.vx = direction.x * speed;
        this.vy = direction.y * speed;
        this.vz = direction.z * speed;
    }

    public void update(float deltaTime, MapManager mapManager, Array<EnemyEntity> enemies) {
        x += vx * deltaTime;
        y += vy * deltaTime;
        z += vz * deltaTime;

        if (mapManager.isColliding(x, z, radius)) {
            isDead = true;
            return;
        }

        if (y <= 1.0f || y >= 4.0f) {
            isDead = true;
            return;
        }

        for (EnemyEntity enemy : enemies) {
            float dx = enemy.x - x;
            float dy = (enemy.y + 0.5f) - y;
            float dz = enemy.z - z;

            float distSq = (dx * dx) + (dy * dy) + (dz * dz);
            float sumRadius = enemy.radius + this.radius;

            if (distSq < sumRadius * sumRadius) {
                enemy.takeHit(x, z, damage);
                isDead = true;
                return;
            }
        }

        instance.transform.setToTranslation(x, y, z);
    }

}
