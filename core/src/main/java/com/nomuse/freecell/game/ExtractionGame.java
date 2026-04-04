package com.nomuse.freecell.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.BloomEffect;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class ExtractionGame extends ApplicationAdapter {

    // Sub-systems
    private EntityManager entityManager;
    private PlayerController playerController;
    private MapManager mapManager;

    // State machine
    public enum GameState { LOBBY, PLAYING }
    public GameState currentState = GameState.LOBBY;

    private final Matrix4 tmpMatrix = new Matrix4();

    // Graphics & assets
    private BitmapFont font;
    private ModelBuilder modelBuilder;
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private PointLight playerLight;

    // Player/Network Array
    private Array<PlayerEntity> players = new Array<>();

    // Visuals and brushes
    private Model handModel, projectileModel, particleModel, enemyModel;
    private ModelInstance swordInstance, torsoBrush, headBrush, limbBrush;

    private final Color hitColor = new Color(Color.RED);
    private final Color workingColor = new Color();

    // Audio
    private Sound footstepSound;
    private Sound hitSound;
    private Sound deathSound;
    private Sound swooshSound;
    private Sound playerHitSound;
    private Sound playerShootSound;

    // VFX and HUD
    private VfxManager vfxManager;
    private BloomEffect bloomEffect;
    private FrameBuffer fbo;
    private SpriteBatch spriteBatch;
    private TextureRegion fboRegion;
    private ShapeRenderer shapeRenderer;

    @Override
    public void create() {
        entityManager = new EntityManager();
        playerController = new PlayerController();

        camera = new PerspectiveCamera(90, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;

        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        font = new BitmapFont();
        
        setupEnvironment();
        loadAudio();
        createModels();
        setupVFX();

        // Pass dependencies to subsystems
        entityManager.init(particleModel, deathSound);
    }

    private void setupEnvironment() {
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.05f, 0.05f, 0.08f, 1f));
        environment.add(new com.badlogic.gdx.graphics.g3d.environment.DirectionalLight().set(0.05f, 0.05f, 0.1f, -1f, -0.8f, -0.2f));

        playerLight = new PointLight().set(0.7f, 0.8f, 1.0f, 0f, 0f, 0f, 9f);
        environment.add(playerLight);
    }

    private void loadAudio() {
        footstepSound = Gdx.audio.newSound(Gdx.files.internal("sounds/footsteps/footstep01.ogg"));
        hitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/npc/hit02.wav"));
        deathSound = Gdx.audio.newSound(Gdx.files.internal("sounds/npc/npc_death.wav"));
        swooshSound = Gdx.audio.newSound(Gdx.files.internal("sounds/player/swoosh.wav"));
        playerHitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/player/player_hit.wav"));
        playerShootSound = Gdx.audio.newSound(Gdx.files.internal("sounds/player/player_shoot03.wav"));
    }

    private void createModels() {
        particleModel = modelBuilder.createBox(1f, 1f, 1f, new Material(ColorAttribute.createDiffuse(Color.FIREBRICK), ColorAttribute.createEmissive(new Color(1.0f, 0.7f, 0.2f, 1f))), Usage.Position | Usage.Normal);
        enemyModel = modelBuilder.createBox(1f, 1f, 1f, new Material(ColorAttribute.createDiffuse(Color.FIREBRICK)), Usage.Position | Usage.Normal);
        projectileModel = modelBuilder.createSphere(0.3f, 0.3f, 0.3f, 10, 10, new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.9f, 1.0f, 1f)), ColorAttribute.createEmissive(new Color(0.6f, 0.95f, 1.0f, 1f))), Usage.Position | Usage.Normal);
        handModel = modelBuilder.createBox(0.27f, 0.27f, 0.85f, new Material(ColorAttribute.createDiffuse(new Color(0.0f, 0.0f, 1.0f, 1f)), ColorAttribute.createEmissive(new Color(0.6f, 0.8f, 1.0f, 1f))), Usage.Position | Usage.Normal);
        
        swordInstance = new ModelInstance(handModel);
        torsoBrush = new ModelInstance(modelBuilder.createBox(0.4f, 0.6f, 0.4f, new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)), Usage.Position | Usage.Normal));
        headBrush = new ModelInstance(modelBuilder.createBox(0.4f, 0.4f, 0.4f, new Material(ColorAttribute.createDiffuse(Color.MAROON)), Usage.Position | Usage.Normal));
        limbBrush = new ModelInstance(modelBuilder.createBox(0.2f, 0.6f, 0.2f, new Material(ColorAttribute.createDiffuse(Color.FIREBRICK)), Usage.Position | Usage.Normal));
    }

    private void setupVFX() {
        vfxManager = new VfxManager(Pixmap.Format.RGBA8888);
        bloomEffect = new BloomEffect();
        bloomEffect.setBloomIntensity(2.0f);
        bloomEffect.setThreshold(0.45f);
        vfxManager.addEffect(bloomEffect);

        spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);
    }

    @Override
    public void render() {
        if (currentState == GameState.LOBBY) {
            renderLobby();
        } else if (currentState == GameState.PLAYING) {
            renderGame();
        }
    }

    // -- START GAME --
    private void startGame() {
        currentState = GameState.PLAYING;
        Gdx.input.setCursorCatched(true);

        PlayerEntity localPlayer = new PlayerEntity(1);
        players.clear();
        players.add(localPlayer);

        if (mapManager != null) mapManager.dispose();
        mapManager = new MapManager();
        mapManager.buildVisuals(modelBuilder);

        entityManager.clearAll();
        for(int i = 0; i < 5; i++) {
            entityManager.spawnEnemy(MathUtils.random(-15f, 15f), 1.6f, MathUtils.random(-15f, 15f));
        }

        // Link player controller
        playerController.localPlayer = localPlayer;
        playerController.camera = camera;
        playerController.playerLight = playerLight;
        playerController.swordInstance = swordInstance;
        playerController.mapManager = mapManager;
        playerController.entityManager = entityManager;
        playerController.projectileModel = projectileModel;
        playerController.footstepSound = footstepSound;
        playerController.hitSound = hitSound;
        playerController.swooshSound = swooshSound;
        playerController.playerShootSound = playerShootSound;
    }

    // -- RENDER GAME --
    private void renderGame() {
        float delta = Gdx.graphics.getDeltaTime();

        playerController.update(delta);
        playerController.localPlayer.update(delta);

        if (playerController.localPlayer.health <= 0) {
            currentState = GameState.LOBBY;
            return;
        }

        if (playerController.localPlayer.justTookDamage) {
            playerController.localPlayer.justTookDamage = false;
            if (playerHitSound != null) playerHitSound.play(1.0f, MathUtils.random(0.6f, 0.8f), 0f);
        }

        entityManager.updateAll(delta, players, mapManager);

        // Render 3D scene to FBO
        fbo.begin();
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 1f);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT | com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        for (ModelInstance block : mapManager.blocks) modelBatch.render(block, environment);
        for (EnemyEntity enemy : entityManager.enemies) renderHumanoid(enemy, modelBatch, environment);
        for (ParticleEntity p : entityManager.particles) modelBatch.render(p.instance, environment);
        modelBatch.render(swordInstance, environment);
        modelBatch.end();
        fbo.end();

        // Apply VFX
        vfxManager.cleanUpBuffers();
        vfxManager.beginInputCapture();
        spriteBatch.begin();
        spriteBatch.draw(fboRegion, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        spriteBatch.end();
        vfxManager.endInputCapture();
        vfxManager.applyEffects();
        vfxManager.renderToScreen();

        renderHUD();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
    }

    // -- RENDER HUD --
    private void renderHUD() {
        shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.updateMatrices();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.05f, 0.05f, 0.08f, 0.9f);
        shapeRenderer.rect(20, 20, 300, 25);
        float healthPercent = Math.max(0, (float)playerController.localPlayer.health / (float)playerController.localPlayer.maxHealth);
        shapeRenderer.setColor(0.2f, 0.8f, 1.0f, 1f);
        shapeRenderer.rect(22, 22, (300 - 4) * healthPercent, 25 - 4);
        shapeRenderer.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    // -- RENDER HUMANOID --
    private void renderHumanoid(EnemyEntity enemy, ModelBatch batch, Environment env) {
        float flashAmount = enemy.damageTimer > 0 ? MathUtils.sin((enemy.damageTimer / EnemyEntity.DAMAGE_DURATION) * MathUtils.PI) : 0f;
        tmpMatrix.idt().translate(enemy.x, enemy.y, enemy.z).rotate(Vector3.Y, enemy.rotation);

        workingColor.set(Color.DARK_GRAY).lerp(hitColor, flashAmount);
        ((ColorAttribute)torsoBrush.materials.get(0).get(ColorAttribute.Diffuse)).color.set(workingColor);
        torsoBrush.transform.set(tmpMatrix);
        batch.render(torsoBrush, env);

        workingColor.set(Color.DARK_GRAY).lerp(hitColor, flashAmount); // Head
        ((ColorAttribute)headBrush.materials.get(0).get(ColorAttribute.Diffuse)).color.set(workingColor);
        headBrush.transform.set(tmpMatrix).translate(0, 0.5f, 0);
        batch.render(headBrush, env);

        workingColor.set(Color.DARK_GRAY).lerp(hitColor, flashAmount); // Limbs
        ((ColorAttribute)limbBrush.materials.get(0).get(ColorAttribute.Diffuse)).color.set(workingColor);
        limbBrush.transform.set(tmpMatrix).translate(-0.1f, -0.6f, 0); batch.render(limbBrush, env); // L Leg
        limbBrush.transform.set(tmpMatrix).translate(0.1f, -0.6f, 0); batch.render(limbBrush, env); // R Leg
        limbBrush.transform.set(tmpMatrix).translate(-0.3f, 0, 0); batch.render(limbBrush, env); // L Arm
        limbBrush.transform.set(tmpMatrix).translate(0.3f, 0, 0); batch.render(limbBrush, env); // R Arm
        batch.flush();
    }

    // -- RENDER LOBBY --
    private void renderLobby() {
        com.badlogic.gdx.utils.ScreenUtils.clear(0.05f, 0.05f, 0.08f, 1f, true);
        if (Gdx.input.isCursorCatched()) Gdx.input.setCursorCatched(false);

        shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.updateMatrices();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);

        float btnW = 250, btnH = 60, btnX = (Gdx.graphics.getWidth() - btnW) / 2f, btnY = (Gdx.graphics.getHeight() - btnH) / 2f;
        float mx = Gdx.input.getX(), my = Gdx.graphics.getHeight() - Gdx.input.getY();
        boolean hover = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(hover ? new Color(0.2f, 0.8f, 1.0f, 1f) : new Color(0.1f, 0.4f, 0.6f, 1f));
        if (hover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) startGame();
        shapeRenderer.rect(btnX, btnY, btnW, btnH);
        shapeRenderer.end();

        spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        spriteBatch.begin();
        font.getData().setScale(1.5f);
        font.draw(spriteBatch, "ENTER GRID", btnX + 50, btnY + 40);
        spriteBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        if (vfxManager != null) vfxManager.resize(width, height);
        if (fbo != null) fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
        if (modelBatch != null) modelBatch.dispose();
        if (mapManager != null) mapManager.dispose();
        if (vfxManager != null) vfxManager.dispose();
        if (bloomEffect != null) bloomEffect.dispose();
        if (fbo != null) fbo.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (handModel != null) handModel.dispose();
        if (enemyModel != null) enemyModel.dispose();
        if (particleModel != null) particleModel.dispose();
        if (projectileModel != null) projectileModel.dispose();
        if (footstepSound != null) footstepSound.dispose();
        if (hitSound != null) hitSound.dispose();
        if (deathSound != null) deathSound.dispose();
        if (swooshSound != null) swooshSound.dispose();
        if (playerHitSound != null) playerHitSound.dispose();
        if (playerShootSound != null) playerShootSound.dispose();
    }

}