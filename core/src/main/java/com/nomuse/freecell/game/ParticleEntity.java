package com.nomuse.freecell.game;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class ParticleEntity {

    public ModelInstance instance;
    public float originX, originY, originZ;
    public float localX, localY, localZ;
    public float vx, vy, vz;
    public float life, maxLife;

    public ParticleEntity(Model particleModel, float enemyWorldX, float enemyWorldY, float enemyWorldZ) {
        this.instance = new ModelInstance(particleModel);

        this.originX = enemyWorldX;
        this.originY = enemyWorldY - 0.8f;
        this.originZ = enemyWorldZ;

        this.localX = 0f;
        this.localY = 0f;
        this.localZ = 0f;

        Vector3 randomDir = new Vector3().setToRandomDirection();
        float speed = MathUtils.random(4f, 5f);

        // Explode outward in random dir
        this.vx = randomDir.x * speed;
        this.vy = Math.abs(randomDir.y * speed) + 3f; 
        this.vz = randomDir.z * speed;

        // Random lifetime
        this.maxLife = MathUtils.random(0.5f, 1.0f);
        this.life = this.maxLife;
    }

    public boolean update(float deltaTime) {
        // Gravity
        vy += PlayerEntity.GRAVITY * deltaTime;

        vx -= vx * 1.5f * deltaTime;
        vz -= vz * 1.5f * deltaTime;

        localX += vx * deltaTime; 
        localY += vy * deltaTime;
        localZ += vz * deltaTime;

        float worldY = originY + localY;

        // Floor collision
        if (worldY < 1.1f) {
            localY = 1.1f - originY;
            vy = -vy * 0.4f;
            vx *= 0.8f;
            vz *= 0.8f;
        }

        life -= deltaTime;

        float renderX = originX + localX;
        float renderY = originY + localY;
        float renderZ = originZ + localZ;

        float scale = (life / maxLife) * 0.2f;
        instance.transform.setToTranslation(renderX, renderY, renderZ).scale(scale, scale, scale);
        return life <= 0;
    }

}
