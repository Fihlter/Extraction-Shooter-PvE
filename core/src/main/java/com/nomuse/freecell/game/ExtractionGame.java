package com.nomuse.freecell.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

public class ExtractionGame extends ApplicationAdapter {

    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private MapManager mapManager;

    private float playerX = 400;
    private float playerY = 400;
    private float playerSpeed = 200f;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 1280, 720);

        shapeRenderer = new ShapeRenderer();

        mapManager = new MapManager(100, 100);
    }

    @Override
    public void render() {
        handleInput();
        updateCamera();

        ScreenUtils.clear(0, 0, 0, 1);

        shapeRenderer.setProjectionMatrix(camera.combined);

        mapManager.render(shapeRenderer);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.rect(playerX, playerY, 24, 24);
        shapeRenderer.end();
    }

    private void handleInput() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) playerY += playerSpeed * deltaTime;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) playerY -= playerSpeed * deltaTime;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) playerX -= playerSpeed * deltaTime;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) playerX += playerSpeed * deltaTime;
    }

    private void updateCamera() {
        camera.position.set(playerX, playerY, 0);
        camera.update();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }

}
