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

// ./gradlew :lwjgl3:run
// git add .
// git commit -m "changes"
// git push

public class MapManager {
    // Build all 3D blocks in the world
    public Array<ModelInstance> blocks = new Array<>();

    // Collisioin grid
    public boolean[][] solidGrid = new boolean[20][20];

    private Model floorModel;
    private Model wallModel;

    public MapManager(ModelBuilder modelBuilder) {
        // 2x2x2 green cube for floor
        floorModel = modelBuilder.createBox(2f, 2f, 2f,
            new Material(ColorAttribute.createDiffuse(Color.FOREST)),
            Usage.Position | Usage.Normal);

        // 2x2x2 gray cube for walls
        wallModel = modelBuilder.createBox(2f, 2f, 2f,
            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
            Usage.Position | Usage.Normal);

        generateMap();
    }

    private void generateMap() {
        Random rand = new Random();

        // Generate a 20x20 grid
        for (int x = -10; x < 10; x++) {
            for (int z = -10; z < 10; z++) {
                int gridX = x + 10;
                int gridZ = z + 10;

                solidGrid[gridX][gridZ] = false;

                ModelInstance floor = new ModelInstance(floorModel);
                floor.transform.setToTranslation(x * 2f, 0f, z * 2f);
                blocks.add(floor);

                boolean isSpawnArea = (x >= -1 && x <= 1) && (z >= 1 && z <= 3);

                if (!isSpawnArea && rand.nextFloat() < 0.2f) {
                    ModelInstance wall = new ModelInstance(wallModel);
                    wall.transform.setToTranslation(x * 2f, 2f, z * 2f);
                    blocks.add(wall);

                    solidGrid[gridX][gridZ] = true;
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
        floorModel.dispose();
        wallModel.dispose();
    }
}