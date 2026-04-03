package com.nomuse.freecell.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.Random;

public class MapManager {

    public int width, height;
    public int[][] map;
    private final int TILE_SIZE = 32;

    public MapManager(int width, int height) {
        this.width = width;
        this.height = height;
        map = new int[width][height];
        generateMap();
    }

    private void generateMap() {
        Random rand = new Random();
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map[x][y] = (rand.nextFloat() < 0.45f) ? 1 : 0;
            }
        }

        for (int i = 0; i < 4; i++) {
            smoothMap();
        }
    }

    private void smoothMap() {
        int[][] newMap = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int neighborWallTiles = getSurroundingWallCount(x, y);
                if (neighborWallTiles > 4) newMap[x][y] = 1;
                else if (neighborWallTiles < 4) newMap[x][y] = 0;
                else newMap[x][y] = map[x][y];
            }
        }
        map = newMap;
    }

    private int getSurroundingWallCount(int gridX, int gridY) {
        int wallCount = 0;
        for (int neighborX = gridX - 1; neighborX <= gridX + 1; neighborX++) {
            for (int neighborY = gridY - 1; neighborY <= gridY + 1; neighborY++) {
                if (neighborX >= 0 && neighborX < width && neighborY >= 0 && neighborY < height) {
                    if (neighborX != gridX || neighborY != gridY) {
                        wallCount += map[neighborX][neighborY];
                    }
                } else {
                    wallCount++;
                }
            }
        }
        return wallCount;
    }

    public void render(ShapeRenderer shapeRenderer) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (map[x][y] == 1) {
                    shapeRenderer.setColor(Color.DARK_GRAY);
                } else {
                    shapeRenderer.setColor(Color.LIGHT_GRAY);
                }
                shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
        shapeRenderer.end();
    }

}
