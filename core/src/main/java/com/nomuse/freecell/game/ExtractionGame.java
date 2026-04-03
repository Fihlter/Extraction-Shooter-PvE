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
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class ExtractionGame extends ApplicationAdapter {
    
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private MapManager mapManager;

    // Network player state
    private PlayerEntity localPlayer;
    private float moveSpeed = 8f;
    private float mouseSensitivity = 0.2f;

    // Melee weapon visuals
    private Model swordModel;
    private ModelInstance swordInstance;

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
        ModelBuilder modelBuilder = new ModelBuilder();

        // Setup lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        mapManager = new MapManager(modelBuilder);

        // Create melee weapon
        swordModel = modelBuilder.createBox(0.2f, 1.5f, 0.2f,
            new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        swordInstance = new ModelInstance(swordModel);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        handleInput(delta);

        localPlayer.update(delta);

        updateCameraAndWeapon();

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

        // Offset sword
        swordInstance.transform.translate(0.5f, -0.4f, -0.8f);

        //swordInstance.transform.rotate(Vector3.X, 15f);
        //swordInstance.transform.rotate(Vector3.Z, 15f);

        // Animate sword swing
        if (localPlayer.isAttacking) {
            float progress = localPlayer.attackTimer / PlayerEntity.ATTACK_DURATION;
            // Swing down and to the left
            float swingAngle = MathUtils.sin((float) (progress * Math.PI)) * 80f;
            swordInstance.transform.rotate(Vector3.Z, swingAngle);
            swordInstance.transform.rotate(Vector3.X, -swingAngle / 0.3f);
        }
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        mapManager.dispose();
        swordModel.dispose();
    }

}