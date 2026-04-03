package com.nomuse.freecell.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.ScreenUtils;

public class ExtractionGame extends ApplicationAdapter {
    
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private MapManager mapManager;

    // Camera/player position
    private float camX = 0f;
    private float camY = 4f;
    private float camZ = 5f;
    private float moveSpeed = 10f;

    @Override
    public void create() {
        // Setup 3D Camera (FOV: 90)
        camera = new PerspectiveCamera(90, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(camX, camY, camZ);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        // Setup 3D renderer and model builder
        modelBatch = new ModelBatch();
        ModelBuilder modelBuilder = new ModelBuilder();

        // Setup lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        mapManager = new MapManager(modelBuilder);
    }

    @Override
    public void render() {
        handleInput();

        // Clear screen to sky blue
        ScreenUtils.clear(0.5f, 0.8f, 1f, 1f, true);

        // Render all 3D objects
        modelBatch.begin(camera);
        for (ModelInstance block : mapManager.blocks) {
            modelBatch.render(block, environment);
        }
        modelBatch.end();
    }

    private void handleInput() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // Basic floating camera movement
        if (Gdx.input.isKeyPressed(Input.Keys.W)) camZ -= moveSpeed * deltaTime;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) camZ += moveSpeed * deltaTime;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) camX -= moveSpeed * deltaTime;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) camX += moveSpeed * deltaTime;

        // Apply new positon to camera
        camera.position.set(camX, camY, camZ);
        camera.update();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        mapManager.dispose();
    }

}