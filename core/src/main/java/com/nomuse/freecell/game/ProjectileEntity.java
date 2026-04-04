package com.nomuse.freecell.game;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class ProjectileEntity {
    public float x, y, z;
    public float vx, vy, vz;
    public float speed = 18f;
    public float damage = 25f;
    public float radius = 0.4f;
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

        this.instance.transform.setToTranslation(x, y, z);
    }

    public void update(float deltaTime, MapManager mapManager, Array<EnemyEntity> enemies, Array<ParticleEntity> particles, Model particleModel) {
        x += vx * deltaTime;
        y += vy * deltaTime;
        z += vz * deltaTime;

        // Leave glowing trail
        if (MathUtils.randomBoolean (0.6f)) {
            float px = x + MathUtils.random(-0.1f, 0.1f);
            float py = y + MathUtils.random(-0.1f, 0.1f);
            float pz = y + MathUtils.random(-0.1f, 0.1f);
            particles.add(new ParticleEntity(particleModel, px, py, pz));
        }

        // Check wall collision
        if (mapManager.isColliding(x, z, radius)) {
            isDead = true;
            return;
        }

        // Floor/ceiling bounds
        if (y <= 0.2f || y >= 10.0f) {
            isDead = true;
            return;
        }

        // Check enemy collision
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
