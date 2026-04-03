package com.nomuse.freecell.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.audio.Sound;

public class ExtractionGame extends ApplicationAdapter {
    
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private MapManager mapManager;

    private ModelBatch shadowBatch;
    private DirectionalShadowLight shadowLight;

    // Network player state
    private PlayerEntity localPlayer;
    private float moveSpeed = 8f;
    private float mouseSensitivity = 0.2f;

    // Visuals
    private Model swordModel;
    private ModelInstance swordInstance;
    private ModelInstance torsoBrush;
    private ModelInstance headBrush;
    private ModelInstance limbBrush;

    // Pre-allocate colors
    private final Color baseTorsoColor = new Color(Color.FIREBRICK);
    private final Color baseHeadColor = new Color(Color.MAROON);
    private final Color baseLimbColor = new Color(Color.BROWN);
    private final Color hitColor = new Color(Color.RED);
    private final Color workingColor = new Color();

    // Sword vars
    private float swordScale = 0.0025f;
    private float hiltOffsetY = 0f;

    // Enemy vars
    private Array<EnemyEntity> enemies;
    private Model enemyModel;
    private ModelInstance enemyBrush;

    // Audio state
    private Sound footstepSound;
    private float footstepTimer = 0f;
    private static final float FOOTSTEP_INTERVAL = 0.35f;

    @Override
    public void create() {

        localPlayer = new PlayerEntity(1);

        camera = new PerspectiveCamera(90, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;

        Gdx.input.setCursorCatched(true);

        modelBatch = new ModelBatch();
        shadowBatch = new ModelBatch(new DepthShaderProvider());

        ModelBuilder modelBuilder = new ModelBuilder();

        // Setup environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        
        // Setup footsteps
        footstepSound = Gdx.audio.newSound(Gdx.files.internal("sounds/footstep01.ogg"));

        shadowLight = new DirectionalShadowLight(4096, 4096, 45f, 45f, 1f, 300f);
        shadowLight.set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);

        environment.add(shadowLight);
        environment.shadowMap = shadowLight;

        mapManager = new MapManager(modelBuilder);

        // --- SPAWN ENEMIES ---
        enemies = new Array<>();
        // Spawn two enemies nearby
        enemies.add(new EnemyEntity(101, 4f, 3f, 4f));
        enemies.add(new EnemyEntity(102, -4f, 3f, 8f));

        // Create a 1x1x1 Red Cube to represent enemies
        enemyModel = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(Color.FIREBRICK)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        enemyBrush = new ModelInstance(enemyModel);

        ObjLoader loader = new ObjLoader();
        swordModel = loader.loadModel(Gdx.files.internal("sword/kamasword.obj"));
        swordInstance = new ModelInstance(swordModel);

        BoundingBox bounds = new BoundingBox();
        swordInstance.calculateBoundingBox(bounds);
        hiltOffsetY = -bounds.min.y;

        swordInstance.materials.get(0).clear();
        swordInstance.materials.get(0).set(ColorAttribute.createDiffuse(Color.DARK_GRAY));
        swordInstance.materials.get(0).set(ColorAttribute.createSpecular(Color.WHITE));
        swordInstance.materials.get(0).set(FloatAttribute.createShininess(100f));

        // Humanoid models
        // Torso
        Model torsoModel = modelBuilder.createBox(0.4f, 0.6f, 0.4f,
            new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        // Head
        Model headModel = modelBuilder.createBox(0.4f, 0.4f, 0.4f,
            new Material(ColorAttribute.createDiffuse(Color.MAROON)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        // Limb
        Model limbModel = modelBuilder.createBox(0.2f, 0.6f, 0.2f,
            new Material(ColorAttribute.createDiffuse(Color.FIREBRICK)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        
        // Create instances
        torsoBrush = new ModelInstance(torsoModel);
        headBrush = new ModelInstance(headModel);
        limbBrush = new ModelInstance(limbModel);
    }

    // -- HANDLE RENDERING --
    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Process Input
        handleInput(delta);
        localPlayer.update(delta);

        for (int i = 0; i < enemies.size; i++) {
            EnemyEntity enemy = enemies.get(i);
            enemy.update(delta, localPlayer, mapManager, enemies);
        }

        // Update Camera
        updateCameraAndWeapon();

        // Shadows
        shadowLight.begin(camera.position, camera.direction);
        shadowBatch.begin(shadowLight.getCamera());

        for (ModelInstance block : mapManager.blocks) {
            shadowBatch.render(block);
        }

        for (int i = 0; i < enemies.size; i++) {
            EnemyEntity enemy = enemies.get(i);
            renderHumanoid(enemy, shadowBatch, null);
        }

        shadowBatch.end();
        shadowLight.end();

        ScreenUtils.clear(0.5f, 0.8f, 1f, 1f, true);

        modelBatch.begin(camera);
        for (ModelInstance block : mapManager.blocks) {
            modelBatch.render(block, environment);
        }
        modelBatch.end();

        // Clear depth buffer to prevent clipping
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);

        // Draw visible humanoid enemies
        for (int i = 0; i < enemies.size; i++) {
            EnemyEntity enemy = enemies.get(i);
            renderHumanoid(enemy, modelBatch, environment);
        }

        // Draw sword
        modelBatch.render(swordInstance, environment);

        modelBatch.end();

        // Press ESC to release mouse cursor
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
        }
    }

    // -- RENDER HUMANOID ENEMY --
    private void renderHumanoid(EnemyEntity enemy, ModelBatch batch, Environment env) {
        float ex = enemy.x;
        float ey = enemy.y;
        float ez = enemy.z;

        // Calculate damage glow
        float flashAmount = 0f;
        if (enemy.damageTimer > 0) {
            flashAmount = MathUtils.sin((enemy.damageTimer / EnemyEntity.DAMAGE_DURATION) * MathUtils.PI);
        }

        // Torso
        workingColor.set(baseTorsoColor).lerp(hitColor, flashAmount);
        ((ColorAttribute)torsoBrush.materials.get(0).get(ColorAttribute.Diffuse)).color.set(workingColor);
        torsoBrush.transform.setToTranslation(ex, ey, ez);
        if (env == null) {
            batch.render(torsoBrush);
        } else {
            batch.render(torsoBrush, env);
        }

        // Head
        workingColor.set(baseHeadColor).lerp(hitColor, flashAmount);
        ((ColorAttribute)headBrush.materials.get(0).get(ColorAttribute.Diffuse)).color.set(workingColor);
        headBrush.transform.setToTranslation(ex, ey + 0.5f, ez);
        if (env == null) {
            batch.render(headBrush);
        } else {
            batch.render(headBrush, env);
        }

        // Legs
        workingColor.set(baseLimbColor).lerp(hitColor, flashAmount);
        ((ColorAttribute)limbBrush.materials.get(0).get(ColorAttribute.Diffuse)).color.set(workingColor);
        limbBrush.transform.setToTranslation(ex - 0.1f, ey - 0.6f, ez);
        if (env == null) {
            batch.render(limbBrush);
        } else {
            batch.render(limbBrush, env);
        }
        limbBrush.transform.setToTranslation(ex + 0.1f, ey - 0.6f, ez);
        if (env == null) {
            batch.render(limbBrush);
        } else {
            batch.render(limbBrush, env);
        }

        // Arms
        limbBrush.transform.setToTranslation(ex - 0.3f, ey, ez);
        if (env == null) {
            batch.render(limbBrush);
        } else {
            batch.render(limbBrush, env);
        }
        limbBrush.transform.setToTranslation(ex + 0.3f, ey, ez);
        if (env == null) {
            batch.render(limbBrush);
        } else {
            batch.render(limbBrush, env);
        }
    }

    // -- INPUT HANDLING --
    private void handleInput(float deltaTime) {
        if (!Gdx.input.isCursorCatched()) return;

        float deltaX = -Gdx.input.getDeltaX() * mouseSensitivity;
        float deltaY = -Gdx.input.getDeltaY() * mouseSensitivity;

        localPlayer.yaw += deltaX;
        localPlayer.pitch += deltaY;
        localPlayer.pitch = MathUtils.clamp(localPlayer.pitch, -89f, 89f);

        Vector3 forward = new Vector3((float)Math.sin(Math.toRadians(localPlayer.yaw)), 0, (float)Math.cos(Math.toRadians(localPlayer.yaw))).nor();
        Vector3 right = new Vector3(forward).crs(Vector3.Y).nor();

        float dx = 0;
        float dz = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            dx -= forward.x * moveSpeed * deltaTime;
            dz -= forward.z * moveSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            dx += forward.x * moveSpeed * deltaTime;
            dz += forward.z * moveSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            dx += right.x * moveSpeed * deltaTime;
            dz += right.z * moveSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            dx -= right.x * moveSpeed * deltaTime;
            dz -= right.z * moveSpeed * deltaTime;
        }

        float playerRadius = 0.4f;

        if (!mapManager.isColliding(localPlayer.x + dx, localPlayer.z, playerRadius)) {
            localPlayer.x += dx;
        }

        if (!mapManager.isColliding(localPlayer.x, localPlayer.z + dz, playerRadius)) {
            localPlayer.z += dz;
        }

        // Footstep logic
        boolean isMoving =  Gdx.input.isKeyPressed(Input.Keys.W) ||
                            Gdx.input.isKeyPressed(Input.Keys.S) ||
                            Gdx.input.isKeyPressed(Input.Keys.A) ||
                            Gdx.input.isKeyPressed(Input.Keys.D);

        if (isMoving && localPlayer.isGrounded) {
            if (footstepTimer >= FOOTSTEP_INTERVAL) {
                footstepSound.play(1.0f, MathUtils.random(0.85f, 1.15f), 0f);
                footstepTimer = 0f;
            }
            footstepTimer += deltaTime;
        } else if (!isMoving && localPlayer.isGrounded) {
            footstepTimer = FOOTSTEP_INTERVAL;
        }
        
        // Jumping
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && localPlayer.isGrounded) {
            localPlayer.yVelocity = PlayerEntity.JUMP_FORCE;
            localPlayer.isGrounded = false;
        }

        // Melee attack
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !localPlayer.isAttacking) {
            localPlayer.isAttacking = true;
            localPlayer.attackTimer = 0f;

            // Hit detection cone
            float hitRange = 2.0f;
            float lookX = -forward.x;
            float lookZ = -forward.z;

            for (int i = 0; i < enemies.size; i++) {
                EnemyEntity enemy = enemies.get(i);

                float eDx = enemy.x - localPlayer.x;
                float eDz = enemy.z - localPlayer.z;
                float dist = (float) Math.sqrt(eDx * eDx + eDz * eDz);

                if (dist < hitRange) {
                    float dirX = eDx / dist;
                    float dirZ = eDz / dist;

                    float dotProduct = (lookX * dirX) + (lookZ * dirZ);

                    if (dotProduct > 0.5f) {
                        enemy.takeHit(localPlayer.x, localPlayer.z);
                        // TODO: Play hit sound
                    }
                }
            }
        }
    }

    // -- UPDATING WEAPON/CAMERA --
    private void updateCameraAndWeapon() {
        camera.position.set(localPlayer.x, localPlayer.y, localPlayer.z);
        camera.direction.set(0, 0, -1);
        camera.up.set(Vector3.Y);
        camera.direction.rotate(Vector3.X, localPlayer.pitch);
        camera.direction.rotate(Vector3.Y, localPlayer.yaw);
        camera.update();

        swordInstance.transform.setToTranslation(camera.position);
        swordInstance.transform.rotate(Vector3.Y, localPlayer.yaw);
        swordInstance.transform.rotate(Vector3.X, localPlayer.pitch);

        swordInstance.transform.translate(0.5f, -0.3f, -0.8f);

        if (localPlayer.isAttacking) {
            float progress = localPlayer.attackTimer / PlayerEntity.ATTACK_DURATION;
            float swingAngle = MathUtils.sin((float) (progress * Math.PI)) * 90f; 
            swordInstance.transform.rotate(Vector3.X, -swingAngle);
            swordInstance.transform.rotate(Vector3.Z, swingAngle * 0.2f);
        }

        swordInstance.transform.scale(swordScale, swordScale, swordScale);
        swordInstance.transform.translate(0, hiltOffsetY, 0);

        swordInstance.transform.translate(0.75f, -0.15f, -0.8f);
        swordInstance.transform.rotate(Vector3.Y, 90f);
        swordInstance.transform.rotate(Vector3.Z, -90f);
    }

    // -- GARBAGE COLLECTION --
    @Override
    public void dispose() {
        modelBatch.dispose();
        shadowBatch.dispose();
        shadowLight.dispose();
        mapManager.dispose();
        swordModel.dispose();
        enemyModel.dispose();
        footstepSound.dispose();
    }
}