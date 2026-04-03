package com.nomuse.freecell.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

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

    // Melee weapon visuals
    private Model swordModel;
    private ModelInstance swordInstance;

    private float swordScale = 0.0025f;
    private float hiltOffsetY = 0f;

    @Override
    public void create() {

        // Setup local player state
        localPlayer = new PlayerEntity(1);

        // Setup 3D Camera (FOV: 90)
        camera = new PerspectiveCamera(90, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;

        Gdx.input.setCursorCatched(true);

        // Setup 3D renderer and model builder
        modelBatch = new ModelBatch();

        shadowBatch = new ModelBatch(new DepthShaderProvider());

        ModelBuilder modelBuilder = new ModelBuilder();

        // Setup lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        
        shadowLight = new DirectionalShadowLight(4096, 4096, 45f, 45f, 1f, 300f);
        shadowLight.set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);

        environment.add(shadowLight);
        environment.shadowMap = shadowLight;

        mapManager = new MapManager(modelBuilder);

        // Create melee weapon
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
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        handleInput(delta);
        localPlayer.update(delta);
        updateCameraAndWeapon();

        shadowLight.begin(camera.position, camera.direction);
        shadowBatch.begin(shadowLight.getCamera());

        for (ModelInstance block : mapManager.blocks) {
            shadowBatch.render(block);
        }

        shadowBatch.end();
        shadowLight.end();

        // Clear screen to sky blue
        ScreenUtils.clear(0.5f, 0.8f, 1f, 1f, true);

        // Render all 3D objects
        modelBatch.begin(camera);
        for (ModelInstance block : mapManager.blocks) {
            modelBatch.render(block, environment);
        }

        // Render weapon
        modelBatch.render(swordInstance, environment);

        modelBatch.end();

        // Press ESC to release mouse cursor
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
        }
    }

    private void handleInput(float deltaTime) {
        if (!Gdx.input.isCursorCatched()) return;

        // Mouse look
        float deltaX = -Gdx.input.getDeltaX() * mouseSensitivity;
        float deltaY = -Gdx.input.getDeltaY() * mouseSensitivity;

        localPlayer.yaw += deltaX;
        localPlayer.pitch += deltaY;

        // Clamp pitch 
        localPlayer.pitch = MathUtils.clamp(localPlayer.pitch, -89f, 89f);

        // WASD Movement
        Vector3 forward = new Vector3((float)Math.sin(Math.toRadians(localPlayer.yaw)), 0, (float)Math.cos(Math.toRadians(localPlayer.yaw))).nor();
        Vector3 right = new Vector3(forward).crs(Vector3.Y).nor();

        float dx = 0;
        float dz = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            localPlayer.x -= forward.x * moveSpeed * deltaTime;
            localPlayer.z -= forward.z * moveSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            localPlayer.x += forward.x * moveSpeed * deltaTime;
            localPlayer.z += forward.z * moveSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            localPlayer.x += right.x * moveSpeed * deltaTime;
            localPlayer.z += right.z * moveSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            localPlayer.x -= right.x * moveSpeed * deltaTime;
            localPlayer.z -= right.z * moveSpeed * deltaTime;
        }

        float playerRadius = 0.4f;

        if (!mapManager.isColliding(localPlayer.x + dx, localPlayer.z, playerRadius)) {
            localPlayer.x += dx;
        }

        if (!mapManager.isColliding(localPlayer.x, localPlayer.z + dz, playerRadius)) {
            localPlayer.z += dz;
        }

        // Melee attack
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !localPlayer.isAttacking) {
            localPlayer.isAttacking = true;
            localPlayer.attackTimer = 0f;
        }
    }

    private void updateCameraAndWeapon() {
        // Apply player state to camera
        camera.position.set(localPlayer.x, localPlayer.y, localPlayer.z);
        camera.direction.set(0, 0, -1);
        camera.up.set(Vector3.Y);
        camera.direction.rotate(Vector3.X, localPlayer.pitch);
        camera.direction.rotate(Vector3.Y, localPlayer.yaw);
        camera.update();

        // Position sword relative to camera
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

        // Offset sword
        swordInstance.transform.translate(0.75f, -0.15f, -0.8f);

        
        swordInstance.transform.rotate(Vector3.Y, 90f);
        swordInstance.transform.rotate(Vector3.Z, -90f);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        shadowBatch.dispose();
        shadowLight.dispose();
        mapManager.dispose();
        swordModel.dispose();
    }

}