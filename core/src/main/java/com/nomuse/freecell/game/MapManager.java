package com.nomuse.freecell.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
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
        floorModel = modelBuilder.createBox(2f, 2f, 2f,
            new Material(ColorAttribute.createDiffuse(Color.FOREST)),
            Usage.Position | Usage.Normal);

        Color softWhiteBlue = new Color(0.8f, 0.9f, 1.0f, 1f);
        Color strongGlow = new Color(0.4f, 0.6f, 1.0f, 1f);

        wallModel = modelBuilder.createBox(2f, 2f, 2f,
            new Material(
                ColorAttribute.createDiffuse(softWhiteBlue),
                ColorAttribute.createEmissive(strongGlow)
            ),
            Usage.Position | Usage.Normal);

        for (int x = -10; x < 10; x++) {
            for (int z = -10; z < 10; z++) {
                int gridX = x + 10;
                int gridZ = z + 10;

                ModelInstance floor = new ModelInstance(floorModel);
                floor.transform.setToTranslation(x * 2f, 0f, z * 2f);
                blocks.add(floor);

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
    }
}