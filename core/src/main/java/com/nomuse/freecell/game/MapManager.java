package com.nomuse.freecell.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
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
        
        wallModel = modelBuilder.createBox(2f, 2f, 2f,
            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
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
                if (gx < 0 || gx >= 20 || gz < 0 || gz >= 20) {
                    return true;
                }
                if (solidGrid[gx][gz]) {
                    return true;
                }
            }
        }
        return false;
    }

    public void dispose() {
        if (floorModel != null) floorModel.dispose();
        if (wallModel != null) wallModel.dispose();
    }
}