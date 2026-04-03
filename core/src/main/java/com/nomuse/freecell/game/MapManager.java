package com.nomuse.freecell.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;

import java.util.Random;

// ./gradlew :lwjgl3:run
// git add .
// git commit -m "changes"
// git push

public class MapManager {
    // Build all 3D blocks in the world
    public Array<ModelInstance> blocks = new Array<>();

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
                // Place floor block at Y = 0
                ModelInstance floor = new ModelInstance(floorModel);
                floor.transform.setToTranslation(x * 2f, 0f, z * 2f);
                blocks.add(floor);

                // 20% chance to place wall block above the floor at Y = 2
                if (rand.nextFloat() < 0.2f) {
                    ModelInstance wall = new ModelInstance(wallModel);
                    wall.transform.setToTranslation(x * 2f, 2f, z * 2f);
                    blocks.add(wall);
                }
            }
        }
    }

    public void dispose() {
        floorModel.dispose();
        wallModel.dispose();
    }
}