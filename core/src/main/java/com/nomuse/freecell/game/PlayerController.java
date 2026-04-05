package com.nomuse.freecell.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class PlayerController {

    private final Vector3 tmpForward = new Vector3();
    private final Vector3 tmpRight = new Vector3();

    private float handTiltX = 0f;
    private float handTiltY = 0f;
    private float moveSpeed = 4f;
    private float mouseSensitivity = 0.2f;
    private float footstepTimer = 0f;
    private static final float FOOTSTEP_INTERVAL = 0.35f;

    // References
    public PlayerEntity localPlayer;
    public PerspectiveCamera camera;
    public PointLight playerLight;
    public ModelInstance swordInstance;
    public MapManager mapManager;
    public EntityManager entityManager;
    public Model projectileModel;

    // Sounds
    public Sound footstepSound;
    public Sound hitSound;
    public Sound swooshSound;
    public Sound playerShootSound;

    public void update(float deltaTime) {
        if (!Gdx.input.isCursorCatched()) return;

        handleMouseLook();
        handleMovement(deltaTime);
        handleCombat();
        updateCameraAndWeapon(deltaTime);
    }

    private void handleMouseLook() {
        float deltaX = -Gdx.input.getDeltaX() * mouseSensitivity;
        float deltaY = -Gdx.input.getDeltaY() * mouseSensitivity;

        handTiltX += deltaX * 0.5f;
        handTiltY += deltaY * 0.5f;

        localPlayer.yaw += deltaX;
        localPlayer.pitch += deltaY;
        localPlayer.pitch = MathUtils.clamp(localPlayer.pitch, -89f, 89f);

        tmpForward.set((float)Math.sin(Math.toRadians(localPlayer.yaw)), 0, (float)Math.cos(Math.toRadians(localPlayer.yaw))).nor();
        tmpRight.set(tmpForward).crs(Vector3.Y).nor();
    }

    private void handleMovement(float deltaTime) {
        float dx = 0;
        float dz = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) { dx -= tmpForward.x * moveSpeed * deltaTime; dz -= tmpForward.z * moveSpeed * deltaTime; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { dx += tmpForward.x * moveSpeed * deltaTime; dz += tmpForward.z * moveSpeed * deltaTime; }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { dx += tmpRight.x * moveSpeed * deltaTime; dz += tmpRight.z * moveSpeed * deltaTime; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { dx -= tmpRight.x * moveSpeed * deltaTime; dz -= tmpRight.z * moveSpeed * deltaTime; }

        float playerRadius = 0.4f;

        if (!mapManager.isColliding(localPlayer.x + dx, localPlayer.z, playerRadius)) localPlayer.x += dx;
        if (!mapManager.isColliding(localPlayer.x, localPlayer.z + dz, playerRadius)) localPlayer.z += dz;

        // Footstep logic
        boolean isMoving = dx != 0 || dz != 0;
        if (isMoving && localPlayer.isGrounded) {
            if (footstepTimer >= FOOTSTEP_INTERVAL) {
                if (footstepSound != null) footstepSound.play(1.0f, MathUtils.random(0.85f, 1.15f), 0f);
                footstepTimer = 0f;
            }
            footstepTimer += deltaTime;
        } else if (!isMoving && localPlayer.isGrounded) {
            footstepTimer = FOOTSTEP_INTERVAL;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && localPlayer.isGrounded) {
            localPlayer.yVelocity = PlayerEntity.JUMP_FORCE;
            localPlayer.isGrounded = false;
        }

        // Debug spawn enemy
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            entityManager.spawnEnemy(localPlayer.x - (tmpForward.x * 3f), 3f, localPlayer.z - (tmpForward.z * 3f));
        }
    }

    private void handleCombat() {
        // Melee attack (left-click)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !localPlayer.isAttacking) {
            localPlayer.isAttacking = true;
            localPlayer.attackTimer = 0f;
            if (swooshSound != null) swooshSound.play(1.0f, MathUtils.random(0.85f, 1.15f), 0f);

            float hitRange = 2.0f;
            float lookX = -tmpForward.x;
            float lookZ = -tmpForward.z;

            for (EnemyEntity enemy : entityManager.enemies) {
                float eDx = enemy.x - localPlayer.x;
                float eDz = enemy.z - localPlayer.z;
                float dist = (float) Math.sqrt(eDx * eDx + eDz * eDz);

                if (dist < hitRange) {
                    float dirX = eDx / dist;
                    float dirZ = eDz / dist;
                    if ((lookX * dirX) + (lookZ * dirZ) > 0.5f) {
                        enemy.takeHit(localPlayer.x, localPlayer.z, 35f);
                        if (hitSound != null) hitSound.play(1.0f, MathUtils.random(0.9f, 1.1f), 0f);
                    }
                }
            }
        }

        // Ranged attack (right-click)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && !localPlayer.isShooting && !localPlayer.isAttacking) {
            localPlayer.isShooting = true;
            localPlayer.shootTimer = 0f;
            if (playerShootSound != null) playerShootSound.play(0.8f, MathUtils.random(1.4f, 1.6f), 0f);

            Vector3 shootDir = new Vector3(camera.direction).nor();
            Vector3 rDir = new Vector3(shootDir).crs(Vector3.Y).nor();
            float sx = localPlayer.x + (shootDir.x * 0.8f) + (rDir.x * 0.25f);
            float sy = camera.position.y + (shootDir.y * 0.8f) - 0.25f;
            float sz = localPlayer.z + (shootDir.z * 0.8f) + (rDir.z * 0.25f);

            entityManager.spawnProjectile(new ModelInstance(projectileModel), sx, sy, sz, shootDir, hitSound);
        }
    }

    private void updateCameraAndWeapon(float delta) {
        camera.position.set(localPlayer.x, localPlayer.y, localPlayer.z);
        playerLight.position.set(camera.position);
        camera.direction.set(0, 0, -1);
        camera.up.set(Vector3.Y);
        camera.direction.rotate(Vector3.X, localPlayer.pitch);
        camera.direction.rotate(Vector3.Y, localPlayer.yaw);
        camera.update();

        handTiltX = MathUtils.lerp(handTiltX, 0, 10f * delta);
        handTiltY = MathUtils.lerp(handTiltY, 0, 10f * delta);

        swordInstance.transform.set(camera.view).inv();
        swordInstance.transform.translate(0.25f, -0.7f, 0.1f);
        swordInstance.transform.rotate(Vector3.X, 20f).rotate(Vector3.Y, -16f);
        swordInstance.transform.rotate(Vector3.Y, handTiltX * 0.5f).rotate(Vector3.Z, handTiltX * 0.5f).rotate(Vector3.X, handTiltY * 0.5f);
        swordInstance.transform.rotate(Vector3.X, -localPlayer.attackOffset * 60f).rotate(Vector3.Y, localPlayer.attackOffset * 30f).rotate(Vector3.Z, localPlayer.attackOffset * 40f);
        swordInstance.transform.translate(0, 0, -0.6f - localPlayer.shootOffset);
    }

}
