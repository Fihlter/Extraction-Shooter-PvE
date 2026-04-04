package com.nomuse.freecell.game;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class ParticleEntity {

    public static final int TYPE_EXPLOSION = 0;
    public static final int TYPE_TRAIL = 1;

    public ModelInstance instance;
    public float worldX, worldY, worldZ;
    public float vx, vy, vz;
    public float life, maxLife;
    public int type;

    public ParticleEntity(Model particleModel, float startX, float startY, float startZ) {
        this(particleModel, startX, startY, startZ, TYPE_EXPLOSION);
    }

    public ParticleEntity(Model particleModel, float startX, float startY, float startZ, int type) {
        this.instance = new ModelInstance(particleModel);
        this.type = type;

        this.worldX = startX;
        this.worldY = startY;
        this.worldZ = startZ;

        if (type == TYPE_EXPLOSION) {
            // Explode outward
            Vector3 randomDir = new Vector3().setToRandomDirection();
            float speed = MathUtils.random(4f, 6f);
            this.vx = randomDir.x * speed;
            this.vy = Math.abs(randomDir.y * speed) + 3f;
            this.vz = randomDir.z * speed;
            this.maxLife = MathUtils.random(0.5f, 1.0f);
        } else if (type == TYPE_TRAIL) {
            this.vx = MathUtils.random(-0.5f, 0.5f);
            this.vy = MathUtils.random(0.2f, 0.8f);
            this.vz = MathUtils.random(-0.5f, 0.5f);
            this.maxLife = MathUtils.random(0.15f, 0.35f);
        }

        this.life = this.maxLife;
    }

    public boolean update(float deltaTime) {

        if (type == TYPE_EXPLOSION) {
            vx += PlayerEntity.GRAVITY * deltaTime;
            vx -= vx * 1.5f * deltaTime;
            vz -= vz * 1.5f * deltaTime;
        } else {
            vx -= vx * 2f * deltaTime;
            vz -= vz * 2f * deltaTime;
        }

        worldX += vx * deltaTime;
        worldY += vy * deltaTime;
        worldZ += vz * deltaTime;

        // Floor collision
        if (worldY < 1.1f) {
            worldY = 1.1f;
            if (type == TYPE_EXPLOSION) {
                vy = -vy * 0.4f;
                vx *= 0.8f;
                vz *= 0.8f;
            }
        }

        life -= deltaTime;

        float scale = (life / maxLife) * 0.2f;
        if (scale > 0) scale = 0f;
        
        instance.transform.setToTranslation(worldX, worldY, worldZ).scale(scale, scale, scale);
        return life <= 0;
    }

}
