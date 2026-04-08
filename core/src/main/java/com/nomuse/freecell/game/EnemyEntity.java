package com.nomuse.freecell.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class EnemyEntity {

    public int id;
    public float x, y, z;
    public float radius = 0.5f;
    public float speed = 4f;

    // Rotation and pathfinding
    public float rotation = 0f;
    private Array<Vector2> currentPath = new Array<>();
    private float pathTimer = 0f;

    // Physics
    public float yVelocity = 0f;
    public boolean isGrounded = true;

    // AI
    public float aggroRange = 15f;
    public float health = 100f;

    // Enemy attack stats
    public float attackCooldown = 0f;
    public static final float ATTACK_RATE = 1.5f;
    public float attackDamage = 15f;

    // Hit detection vars
    public float damageTimer = 0f;
    public static final float DAMAGE_DURATION = 0.5f;
    public float knockbackX = 0f;
    public float knockbackZ = 0f;

    // Status effect vars
    public float burnTimer = 0f;
    public float burnDps = 0f;

    public EnemyEntity(int id, float startX, float startY, float startZ) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.z = startZ;
    }

    public void takeHit(float sourceX, float sourceZ, float damage) {
        health -= damage;
        damageTimer = DAMAGE_DURATION;

        float dx = x - sourceX;
        float dz = z - sourceZ;
        float dist = (float)Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.001f) {
            knockbackX = (dx / dist) * 15f;
            knockbackZ = (dz / dist) * 15f;
        }
    }

    // Apply DoT
    public void applyBurn(float duration, float dps) {
        this.burnTimer = duration;
        this.burnDps = dps;
    }

    // Server-side Sim Logic
    public void update(float deltaTime, Array<PlayerEntity> players, MapManager mapManager, Array<EnemyEntity> allEnemies) {

        if (burnTimer > 0) {
            burnTimer -= deltaTime;
            health -= burnDps * deltaTime;
            if (MathUtils.randomBoolean(0.05f)) damageTimer = 0.2f;
        }

        if (attackCooldown > 0) {
            attackCooldown -= deltaTime;
        }

        if (damageTimer > 0) {
            damageTimer -= deltaTime;
            if (damageTimer < 0) damageTimer = 0f;
        }

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

        PlayerEntity closestPlayer = null;
        float closestDistSq = Float.MAX_VALUE;

        // Scan all players to find closest
        for (int i = 0; i < players.size; i++) {
            PlayerEntity p = players.get(i);

            float dx = p.x - x;
            float dz = p.z - z;
            float distSq = (dx * dx) + (dz * dz);

            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestPlayer = p;
            }
        }

        // Movement AI
        float moveX = 0;
        float moveZ = 0;

        if (closestPlayer != null) {
            float distanceToPlayer = (float) Math.sqrt(closestDistSq);

            if (distanceToPlayer < aggroRange && distanceToPlayer > 1.2f) {
                boolean hasLOS = mapManager.hasLineOfSight(x, z, closestPlayer.x, closestPlayer.z);

                if (hasLOS) {
                    // Walk to player
                    float dx = closestPlayer.x - x;
                    float dz = closestPlayer.z - z;
                    moveX = (dx / distanceToPlayer * speed);
                    moveZ = (dz / distanceToPlayer * speed);

                    currentPath.clear();
                    pathTimer = 1.0f;
                } else {
                    // Update path 2x/sec
                    pathTimer += deltaTime;
                    if (pathTimer > 0.5f) {
                        pathTimer = 0f;
                        currentPath = mapManager.findPath(x, z, closestPlayer.x, closestPlayer.z);
                    }

                    // Follow path nodes
                    if (currentPath.size > 0) {
                        Vector2 nextNode = currentPath.first();
                        float nx = nextNode.x - x;
                        float nz = nextNode.y - z;
                        float distToNode = (float)Math.sqrt(nx*nx + nz*nz);

                        if (distToNode < 0.8f) {
                            currentPath.removeIndex(0);
                        } else {
                            moveX = (nx / distToNode) * speed;
                            moveZ = (nz / distToNode) * speed;
                        }
                    }
                }

                // Separation force
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
            } else {
                currentPath.clear();
            }

            if (distanceToPlayer <= 1.5f) {
                if (attackCooldown <= 0f) {
                    closestPlayer.takeDamage(attackDamage);
                    attackCooldown = ATTACK_RATE;
                }
            }
        }

        // Smooth rotation
        if (moveX != 0 || moveZ != 0) {
            float targetAngle = MathUtils.radiansToDegrees * MathUtils.atan2(moveX, moveZ);
            rotation = MathUtils.lerpAngleDeg(rotation, targetAngle, deltaTime * 10f);
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
