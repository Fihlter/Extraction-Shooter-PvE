package com.nomuse.freecell.game;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.audio.Sound;

public class EntityManager {

    public Array<EnemyEntity> enemies = new Array<>();
    public Array<ProjectileEntity> projectiles = new Array<>();
    public Array<ParticleEntity> particles = new Array<>();

    public Model particleModel;
    public Sound deathSound;

    public void init(Model particleModel, Sound deathSound) {
        this.particleModel = particleModel;
        this.deathSound = deathSound;
    }

    public void updateAll(float delta, Array<PlayerEntity> players, MapManager mapManager) {
        
        // Update enemies
        for (int i = enemies.size - 1; i >= 0; i--) {
            EnemyEntity enemy = enemies.get(i);
            enemy.update(delta, players, mapManager, enemies);

            if (enemy.health <= 0) {
                if (deathSound != null) deathSound.play(1.0f, MathUtils.random(0.85f, 1.15f), 0f);

                for (int p = 0; p < 50; p++) {
                    particles.add(new ParticleEntity(particleModel, enemy.x, enemy.y + 0.5f, enemy.z));
                }
                enemies.removeIndex(i);
            }
        }

        // Update projectiles
        for (int i = projectiles.size - 1; i >= 0; i--) {
            ProjectileEntity proj = projectiles.get(i);
            proj.update(delta, mapManager, enemies, particles, particleModel);

            if (proj.isDead) {
                for (int p = 0; p < 5; p++) {
                    particles.add(new ParticleEntity(particleModel, proj.x, proj.y, proj.z));
                }
                projectiles.removeIndex(i);
            }
        }

        // Update particles
        for (int i = particles.size - 1; i >= 0; i--) {
            ParticleEntity p = particles.get(i);
            if (p.update(delta)) {
                particles.removeIndex(i);
            }
        }
    }

    public void spawnEnemy(float x, float y, float z) {
        int randomId = MathUtils.random(1000, 9999);
        enemies.add(new EnemyEntity(randomId, x, y, z));
    }

    public void spawnProjectile(ModelInstance instance, float x, float y, float z, com.badlogic.gdx.math.Vector3 dir, Sound hitSound) {
        projectiles.add(new ProjectileEntity(instance, x, y, z, dir, hitSound));
    }

    public void clearAll() {
        enemies.clear();
        projectiles.clear();
        particles.clear();
    }

}
