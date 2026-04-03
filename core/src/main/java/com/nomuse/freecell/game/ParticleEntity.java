package com.nomuse.freecell.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class ParticleEntity {

    public float x, y, z;
    public float vx, vy, vz;
    public float life, maxLife;

    public ParticleEntity(float startX, float startY, float startZ) {
        this.x = startX;
        this.y = startY;
        this.z = startZ;

        Vector3 randomDir = new Vector3().setToRandomDirection();
        float speed = MathUtils.random(4f, 10f);

        // Explode outward in random dir
        this.vx = randomDir.x * speed;
        this.vy = randomDir.z * speed;
        this.vz = Math.abs(randomDir.y * speed) + 3f;

        // Random lifetime
        this.maxLife = MathUtils.random(0.5f, 1.5f);
        this.life = this.maxLife;
    }

    public boolean update(float deltaTime) {
        // Gravity
        vy += PlayerEntity.GRAVITY * deltaTime;

        vx -= vx * 1.5f * deltaTime;
        vz -= vz * 1.5f * deltaTime;

        x += vy * deltaTime;
        y += vy * deltaTime;
        z += vz * deltaTime;

        // Floor collision
        if (y < 1.1f) {
            y = 1.1f;
            vy = -vy * 0.4f;
            vx *= 0.8f;
            vz *= 0.8f;
        }

        life -= deltaTime;
        return life <= 0;
    }

}
