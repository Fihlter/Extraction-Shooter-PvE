package com.nomuse.freecell.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.Random;

public class MapManager {
    // Client-side visuals
    public Array<ModelInstance> blocks = new Array<>();
    private Model floorModel;
    private Model wallModel;

    // Lightmap image
    private Texture floorTexture;

    // Server-side logic
    public boolean[][] solidGrid = new boolean[20][20];

    public MapManager() {
        generateLogicMap();
    }

    // Initialize math/logic
    private void generateLogicMap() {
        Random rand = new Random();
        for (int x = -10; x < 10; x++) {
            for (int z = -10; z < 10; z++) {
                int gridX = x + 10;
                int gridZ = z + 10;

                solidGrid[gridX][gridZ] = false;
                boolean isSpawnArea = (x >= -1 && x <= 1) && (z >= 1 && z <= 3);

                if (!isSpawnArea && rand.nextFloat() < 0.2f) {
                    solidGrid[gridX][gridZ] = true;
                }
            }
        }
    }

    // Initialize visuals
    public void buildVisuals(ModelBuilder modelBuilder) {
        // Setup wall materials
        Color softWhiteBlue = new Color(0.7f, 0.8f, 1.0f, 1f);
        Color strongGlow = new Color(0.9f, 0.95f, 1.0f, 1f);

        wallModel = modelBuilder.createBox(2f, 2f, 2f,
            new Material(
                ColorAttribute.createDiffuse(softWhiteBlue),
                ColorAttribute.createEmissive(strongGlow)
            ),
            Usage.Position | Usage.Normal);

        // Generate lightmap
        Pixmap lightmap = new Pixmap(512, 512, Pixmap.Format.RGBA8888);
        Color baseFloor = new Color(0.1f, 0.1f, 0.12f, 1f);
        Color glowColor = new Color(0.3f, 0.5f, 0.9f, 1f);

        for (int px = 0; px < 512; px++) {
            for (int py = 0; py < 512; py++) {
                float worldX = (px / 512f) * 40f - 21f;
                float worldZ = (py / 512f) * 40f - 21f;

                float lightIntensity = 0f;

                int gridX = MathUtils.floor((worldX + 1f) / 2f) + 10;
                int gridZ = MathUtils.floor((worldZ + 1f) / 2f) + 10;

                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        int nx = gridX + dx;
                        int nz = gridZ + dz;

                        if (nx >= 0 && nx < 20 && nz >= 0 && nz < 20 && solidGrid[nx][nz]) {
                            float wallWorldX = (nx - 10) * 2f;
                            float wallWorldZ = (nz - 10) * 2f;

                            float dist = (float)Math.sqrt((worldX - wallWorldX)*(worldX - wallWorldX) + (worldZ - wallWorldZ)*(worldZ - wallWorldZ));
                            if (dist < 3.0f) {
                                float intensity = (3.0f - dist) / 2.0f;
                                lightIntensity += intensity;
                            }
                        }
                    }
                }
                lightIntensity = MathUtils.clamp(lightIntensity, 0f, 1f);

                float r = baseFloor.r + (glowColor.r - baseFloor.r) * lightIntensity;
                float g = baseFloor.g + (glowColor.g - baseFloor.g) * lightIntensity;
                float b = baseFloor.b + (glowColor.b - baseFloor.b) * lightIntensity;

                lightmap.drawPixel(px, py, Color.rgba8888(r, g, b, 1f));
            }
        }

        floorTexture = new Texture(lightmap);
        floorTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        lightmap.dispose();

        Material floorMat = new Material(
            TextureAttribute.createDiffuse(floorTexture),
            TextureAttribute.createEmissive(floorTexture)
        );

        floorModel = modelBuilder.createRect(
            -21f, 1f, -21f,
            19f, 1f, -21f,
            19f, 1f, 19f,
            -21f, 1f, 19f,
            0f, 1f, 0f,
            floorMat,
            Usage.Position | Usage.Normal | Usage.TextureCoordinates
        );

        ModelInstance giantFloor = new ModelInstance(floorModel);
        blocks.add(giantFloor);

        // Build walls
        for (int x = -10; x < 10; x++) {
            for (int z = -10; z < 10; z++) {
                int gridX = x + 10;
                int gridZ = z + 10;

                if (solidGrid[gridX][gridZ]) {
                    ModelInstance wall = new ModelInstance(wallModel);
                    wall.transform.setToTranslation(x * 2f, 2f, z * 2f);
                    blocks.add(wall);
                }
            }
        }
    }

    // Grid converters
    public int worldToGrid(float worldPos) {
        return MathUtils.floor((worldPos + 1f) / 2f) + 10;
    }

    public float gridToWorld(int gridPos) {
        return (gridPos - 10) * 2f;
    }

    public boolean isValidGrid(int gx, int gz) {
        return gx >= 0 && gx < 20 && gz >= 0 && gz < 20;
    }

    // LoS Check
    public boolean hasLineOfSight(float x1, float z1, float x2, float z2) {
        float dx = x2 - x1;
        float dz = z2 - z1;
        float dist = (float)Math.sqrt(dx * dz + dz * dz);
        int steps = MathUtils.ceil(dist * 2f);
        for (int i = 0; i <= steps; i++) {
            float t = steps == 0 ? 0 : (float)i / steps;
            float cx = x1 + dx * t;
            float cz = z1 + dz * t;
            int gx = worldToGrid(cx);
            int gz = worldToGrid(cz);
            if (isValidGrid(gx, gz) && solidGrid[gx][gz]) return false;
        }
        return true;
    }

    // Pathfinding
    public Array<Vector2> findPath(float startX, float startZ, float targetX, float targetZ) {
        int sx = worldToGrid(startX);
        int sz = worldToGrid(startZ);
        int tx = worldToGrid(targetX);
        int tz = worldToGrid(targetZ);

        Array<Vector2> path = new Array<>();
        if (sx == tx && sz == tz) return path;
        if (!isValidGrid(tx, tz) || solidGrid[tx][tz]) return path;

        int[][] parentX = new int[20][20];
        int[][] parentZ = new int[20][20];
        boolean[][] visited = new boolean[20][20];

        Array<GridPoint2> queue = new Array<>();
        queue.add(new GridPoint2(sx, sz));
        visited[sx][sz] = true;

        int[] dx = {0, 0, -1, 1, -1, 1, -1, 1};
        int[] dz = {-1, 1, 0, 0, -1, -1, 1, 1};

        boolean found = false;
        while(queue.size > 0) {
            GridPoint2 curr = queue.removeIndex(0);
            if (curr.x == tx && curr.y == tz) { found = true; break; }
            for (int i = 0; i < 8; i++) {
                int nx = curr.x + dx[i];
                int nz = curr.y + dz[i];
                if (isValidGrid(nx, nz) && !solidGrid[nx][nz] && !visited[nx][nz]) {
                    if (i >= 4) {
                        if (solidGrid[curr.x][nz] || solidGrid[nx][curr.y]) continue;
                    }
                    visited[nx][nz] = true;
                    parentX[nx][nz] = curr.x;
                    parentZ[nx][nz] = curr.y;
                    queue.add(new GridPoint2(nx, nz));
                }
            }
        }

        if (found) {
            int cx = tx, cz = tz;
            while(cx != sx || cz != sz) {
                path.add(new Vector2(gridToWorld(cx), gridToWorld(cz)));
                int px = parentX[cx][cz];
                int pz = parentZ[cx][cz];
                cx = px; cz = pz;
            }
            path.reverse();
        }
        return path;
    }

    public boolean isColliding(float worldX, float worldZ, float playerRadius) {
        float minX = worldX - playerRadius;
        float maxX = worldX + playerRadius;
        float minZ = worldZ - playerRadius;
        float maxZ = worldZ + playerRadius;

        int startX = MathUtils.floor((minX + 1f) / 2f) + 10;
        int endX = MathUtils.floor((maxX + 1f) / 2f) + 10;
        int startZ = MathUtils.floor((minZ + 1f) / 2f) + 10;
        int endZ = MathUtils.floor((maxZ + 1f) / 2f) + 10;

        for (int gx = startX; gx <= endX; gx++) {
            for (int gz = startZ; gz <= endZ; gz++) {
                if (!isValidGrid(gx, gz)) return true;
                if (solidGrid[gx][gz]) return true;
            }
        }
        return false;
    }

    public void dispose() {
        if (floorModel != null) floorModel.dispose();
        if (wallModel != null) wallModel.dispose();
        if (floorTexture != null) floorTexture.dispose();
    }
}