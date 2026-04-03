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

// ./gradlew :lwjgl3:run
// git add .
// git commit -m "changes"
// git push

public class ExtractionGame extends ApplicationAdapter {

    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private MapManager mapManager;

    private float playerX = 400;
    private float playerY = 400;
    private float playerSpeed = 200f;
    private float playerAngle = 0f;

    // Melee combat vars
    private boolean isAttacking = false;
    private float attackTimer = 0f;
    private final float ATTACK_DURATION = 0.15f;

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

        // Draw melee slash
        if (isAttacking) {
            attackTimer += Gdx.graphics.getDeltaTime();

            float progress = MathUtils.clamp(attackTimer / ATTACK_DURATION, 0f, 1f);

            shapeRenderer.setColor(Color.WHITE);
            float currentSweep = 120f * progress;
            shapeRenderer.arc(20, 0, 30, -60, currentSweep);

            if (attackTimer >= ATTACK_DURATION) {
                isAttacking = false;
            }
        }

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

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isAttacking) {
            isAttacking = true;
            attackTimer = 0f;
        }

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
