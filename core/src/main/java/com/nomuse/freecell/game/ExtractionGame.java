package com.nomuse.freecell.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class ExtractionGame extends ApplicationAdapter {

    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private MapManager mapManager;

    private float playerX = 400;
    private float playerY = 400;
    private float playerSpeed = 200f;
    private float playerAngle = 0f;

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

        // Render player
        Matrix4 oldTransform = new Matrix4(shapeRenderer.getTransformMatrix());

        // Create new transform matrix
        Matrix4 transform = new Matrix4();
        transform.setToTranslation(playerX, playerY, 0);
        transform.rotate(0, 0, 1, playerAngle);
        shapeRenderer.setTransformMatrix(transform);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.triangle(15, 0, -12, -12, -12, 12);
        shapeRenderer.end();

        shapeRenderer.setTransformMatrix(oldTransform);
    }

    private void handleInput() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // Movement input
        if (Gdx.input.isKeyPressed(Input.Keys.W)) playerY += playerSpeed * deltaTime;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) playerY -= playerSpeed * deltaTime;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) playerX -= playerSpeed * deltaTime;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) playerX += playerSpeed * deltaTime;

        // Mouse aiming input
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);

        camera.unproject(mousePos);

        float dx = mousePos.x - playerX;
        float dy = mousePos.y - playerY;

        playerAngle = MathUtils.radiansToDegrees * MathUtils.atan2(dy, dx);
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
