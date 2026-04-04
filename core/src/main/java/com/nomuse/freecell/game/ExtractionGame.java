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
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.BloomEffect;
import com.badlogic.gdx.graphics.Pixmap;

public class ExtractionGame extends ApplicationAdapter {

    private final Vector3 tmpForward = new Vector3();
    private final Vector3 tmpRight = new Vector3();
    private final Matrix4 tmpMatrix = new Matrix4();
    
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private MapManager mapManager;
    private PointLight playerLight;

    //private ModelBatch shadowBatch;
    //private DirectionalShadowLight shadowLight;

    // Network player state
    private PlayerEntity localPlayer;
    private Array<PlayerEntity> players;
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
    private final Color baseLimbColor = new Color(Color.DARK_GRAY);
    private final Color hitColor = new Color(Color.RED);
    private final Color workingColor = new Color();

    // Sword vars
    private float swordScale = 0.0025f;
    private float hiltOffsetY = 0f;

    // Enemy vars
    private Array<EnemyEntity> enemies;
    private Array<ParticleEntity> particles = new Array<>();
    private Model particleModel;
    private Model enemyModel;
    private ModelInstance enemyBrush;

    // Audio state
    private Sound footstepSound;
    private Sound hitSound;
    private Sound deathSound;
    private float footstepTimer = 0f;
    private static final float FOOTSTEP_INTERVAL = 0.35f;

    // VFX
    private VfxManager vfxManager;
    private BloomEffect bloomEffect;

    @Override
    public void create() {

        mapManager = new MapManager();

        localPlayer = new PlayerEntity(1);

        players = new Array<>();
        players.add(localPlayer);

        camera = new PerspectiveCamera(90, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;

        Gdx.input.setCursorCatched(true);

        modelBatch = new ModelBatch();
        //shadowBatch = new ModelBatch(new DepthShaderProvider());

        ModelBuilder modelBuilder = new ModelBuilder();
        mapManager.buildVisuals(modelBuilder);

        // Setup environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.05f, 0.05f, 0.08f, 1f));
        environment.add(new com.badlogic.gdx.graphics.g3d.environment.DirectionalLight().set(0.05f, 0.05f, 0.1f, -1f, -0.8f, -0.2f));
        
        // Setup sounds
        footstepSound = Gdx.audio.newSound(Gdx.files.internal("sounds/footsteps/footstep01.ogg"));
        hitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/npc/hit02.wav"));
        deathSound = Gdx.audio.newSound(Gdx.files.internal("sounds/npc/npc_death.wav"));

        // Player lantern
        playerLight = new PointLight().set(0.7f, 0.8f, 1.0f, 0f, 0f, 0f, 12f);
        environment.add(playerLight);

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

        // Initialize particle model
        particleModel = modelBuilder.createBox(1f, 1f, 1f,
            new Material(ColorAttribute.createDiffuse(Color.FIREBRICK)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

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

        // Post-processing pipeline
        vfxManager = new VfxManager(Pixmap.Format.RGBA8888);
        bloomEffect = new BloomEffect();

        bloomEffect.setBloomIntensity(1.5f);
        bloomEffect.setThreshold(0.8f);
        vfxManager.addEffect(bloomEffect);
    }

    // -- HANDLE RENDERING --
    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Process Input
        handleInput(delta);
        //localPlayer.update(delta);

        for (int i = 0; i < enemies.size; i++) {
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

        // Update particles
        for (int i = particles.size - 1; i >= 0; i--) {
            ParticleEntity p = particles.get(i);
            if (p.update(delta)) {
                particles.removeIndex(i);
            }
        }

        // Update Camera
        updateCameraAndWeapon();

        vfxManager.cleanUpBuffers();
        vfxManager.beginInputCapture();

        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 1f);
        Gdx.gl.glClear(com.badlogic.gdx. graphics.GL20.GL_COLOR_BUFFER_BIT | com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);

        //ScreenUtils.clear(0.02f, 0.02f, 0.05f, 1f, true);

        modelBatch.begin(camera);

        for (ModelInstance block : mapManager.blocks) {
            modelBatch.render(block, environment);
        }
        for (int i = 0; i < enemies.size; i++) {
            renderHumanoid(enemies.get(i), modelBatch, environment);
        }
        for (int i = 0; i < particles.size; i++) {
            modelBatch.render(particles.get(i).instance, environment);
        
        }
        modelBatch.render(swordInstance, environment);

        modelBatch.end();

        vfxManager.endInputCapture();
        vfxManager.applyEffects();
        vfxManager.renderToScreen();

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

        // Move enemy's world position
        tmpMatrix.idt().translate(ex, ey, ez).rotate(Vector3.Y, enemy.rotation);

        // Torso
        workingColor.set(baseTorsoColor).lerp(hitColor, flashAmount);
        ((ColorAttribute)torsoBrush.materials.get(0).get(ColorAttribute.Diffuse)).color.set(workingColor);
        torsoBrush.transform.set(tmpMatrix);
        if (env == null) {
            batch.render(torsoBrush);
        } else {
            batch.render(torsoBrush, env);
        }

        // Head
        workingColor.set(baseHeadColor).lerp(hitColor, flashAmount);
        ((ColorAttribute)headBrush.materials.get(0).get(ColorAttribute.Diffuse)).color.set(workingColor);
        headBrush.transform.set(tmpMatrix).translate(0, 0.5f, 0);
        if (env == null) {
            batch.render(headBrush);
        } else {
            batch.render(headBrush, env);
        }

        // Legs
        workingColor.set(baseLimbColor).lerp(hitColor, flashAmount);
        ((ColorAttribute)limbBrush.materials.get(0).get(ColorAttribute.Diffuse)).color.set(workingColor);
        limbBrush.transform.set(tmpMatrix).translate(-0.1f, -0.6f, 0);
        if (env == null) {
            batch.render(limbBrush);
        } else {
            batch.render(limbBrush, env);
        }
        limbBrush.transform.set(tmpMatrix).translate(0.1f, -0.6f, 0);
        if (env == null) {
            batch.render(limbBrush);
        } else {
            batch.render(limbBrush, env);
        }
        
        // Arms
        limbBrush.transform.set(tmpMatrix).translate(-0.3f, 0, 0);
        if (env == null) {
            batch.render(limbBrush);
        } else {
            batch.render(limbBrush, env);
        }
        limbBrush.transform.set(tmpMatrix).translate(0.3f, 0, 0);
        if (env == null) {
            batch.render(limbBrush);
        } else {
            batch.render(limbBrush, env);
        }
  
        batch.flush();
    }   

    // -- INPUT HANDLING --
    private void handleInput(float deltaTime) {
        if (!Gdx.input.isCursorCatched()) return;

        float deltaX = -Gdx.input.getDeltaX() * mouseSensitivity;
        float deltaY = -Gdx.input.getDeltaY() * mouseSensitivity;

        localPlayer.yaw += deltaX;
        localPlayer.pitch += deltaY;
        localPlayer.pitch = MathUtils.clamp(localPlayer.pitch, -89f, 89f);

        tmpForward.set((float)Math.sin(Math.toRadians(localPlayer.yaw)), 0, (float)Math.cos(Math.toRadians(localPlayer.yaw))).nor();
        tmpRight.set(tmpForward).crs(Vector3.Y).nor();

        float dx = 0;
        float dz = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            dx -= tmpForward.x * moveSpeed * deltaTime;
            dz -= tmpForward.z * moveSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            dx += tmpForward.x * moveSpeed * deltaTime;
            dz += tmpForward.z * moveSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            dx += tmpRight.x * moveSpeed * deltaTime;
            dz += tmpRight.z * moveSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            dx -= tmpRight.x * moveSpeed * deltaTime;
            dz -= tmpRight.z * moveSpeed * deltaTime;
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

        // Spawn Enemy
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            float spawnX = localPlayer.x - (tmpForward.x * 3f);
            float spawnZ = localPlayer.z - (tmpForward.z * 3f);
            float spawnY = 3f;

            int randomId = MathUtils.random(1000, 9999);

            enemies.add(new EnemyEntity(randomId, spawnX, spawnY, spawnZ));
        }

        // Melee attack
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !localPlayer.isAttacking) {
            localPlayer.isAttacking = true;
            localPlayer.attackTimer = 0f;

            // Hit detection cone
            float hitRange = 2.0f;
            float lookX = -tmpForward.x;
            float lookZ = -tmpForward.z;

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
                        enemy.takeHit(localPlayer.x, localPlayer.z, 35f);
                        // Play hit sound
                        hitSound.play(1.0f, MathUtils.random(0.9f, 1.1f), 0f);
                    }
                }
            }
        }
    }

    // -- UPDATING WEAPON/CAMERA --
    private void updateCameraAndWeapon() {
        camera.position.set(localPlayer.x, localPlayer.y, localPlayer.z);
        playerLight.position.set(camera.position);
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

    @Override
    public void resize(int width, int height) {
        if (vfxManager != null) {
            vfxManager.resize(width, height);
        }
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    // Collect sound garbage
    private void disposeSounds() {
        if (footstepSound != null) footstepSound.dispose();
        if (hitSound != null) hitSound.dispose();
        if (deathSound != null) deathSound.dispose();
    }

    // Collect model garbage
    private void disposeModels() {
        if (swordModel != null) swordModel.dispose();
        if (enemyModel != null) enemyModel.dispose();
        if (particleModel != null) particleModel.dispose();
    }

    // Collect world garbage
    private void disposeWorld() {
        if (modelBatch != null) modelBatch.dispose();
        if (mapManager != null) mapManager.dispose();
        if (vfxManager != null) vfxManager.dispose();
        if (bloomEffect != null) bloomEffect.dispose();
    }

    // -- GARBAGE COLLECTION --
    @Override
    public void dispose() {
        disposeWorld();
        disposeModels();
        disposeSounds();
    }
}