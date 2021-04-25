package com.mycompany.app.levelGenerators.sampler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import com.mycompany.app.engine.core.MarioLevelGenerator;
import com.mycompany.app.engine.core.MarioLevelModel;
import com.mycompany.app.engine.core.MarioTimer;

public class LevelGenerator implements MarioLevelGenerator {
    private int sampleWidth = 10;
    private String folderName = "levels/original/";

    private Random rnd;

    public LevelGenerator() {
        this("levels/original/", 10);
    }

    public LevelGenerator(String sampleFolder) {
        this(sampleFolder, 10);
    }

    public LevelGenerator(String sampleFolder, int sampleWidth) {
        this.sampleWidth = sampleWidth;
        this.folderName = sampleFolder;
    }

    private String getRandomLevel() throws IOException {
        File[] listOfFiles = new File(folderName).listFiles();
        List<String> lines = Files.readAllLines(listOfFiles[rnd.nextInt(listOfFiles.length)].toPath());
        String result = "";
        for (int i = 0; i < lines.size(); i++) {
            result += lines.get(i) + "\n";
        }
        return result;
    }

    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer) {
        boolean levelIsValid = false;

        while (!levelIsValid) {
            rnd = new Random();
            model.clearMap();
            for (int i = 0; i < model.getWidth() / sampleWidth; i++) {
                try {
                    model.copyFromString(i * sampleWidth, 0, i * sampleWidth, 0, sampleWidth, model.getHeight(), this.getRandomLevel());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            levelIsValid = LevelStaticChecker.validateLevel(model);
        }

        return model.getMap();
    }

    public static class LevelStaticChecker {
        private static final int JUMP_HEIGHT = 4;
        private static final int JUMP_HALF_WIDTH = 6;
        private static final Cell[] adjacentCells = {
                new Cell(1, 0),
                new Cell(0, 1),
        };

        public static boolean validateLevel(MarioLevelModel model) {
            Cell spawnPoint = findSpawnPoint(new Cell(0, model.getHeight() - 1), model);

            HashMap<String, Integer> distances = bfs(spawnPoint, model);

            Cell finishPoint = findFinishPoint(model);
            return reachedFinish(finishPoint, distances);
        }

        private static boolean reachedFinish(Cell finishPoint, HashMap<String, Integer> distances) {
            for (int y = finishPoint.y; y >= 0; --y) {
                Cell cell = new Cell(finishPoint.x, y);
                if (distances.containsKey(cell.toString())) {
                    return true;
                }
            }

            return false;
        }

        private static Cell findFinishPoint(MarioLevelModel model) {
            for (int x = model.getWidth() - 1; x >= 0; --x) {
                for (int y = model.getHeight() - 1; y >= 0; --y) {
                    if (model.getBlock(x, y) == 'F') {
                        return new Cell(x, y);
                    }
                }
            }

            return new Cell(model.getWidth() - 1, model.getHeight() - 3);
        }

        private static HashMap<String, Integer> bfs(Cell spawnPoint, MarioLevelModel model) {
            LinkedList<Cell> cellsToVisit = new LinkedList<>();
            HashMap<String, Integer> distances = new HashMap<>();
            HashSet<String> visited = new HashSet<>();

            cellsToVisit.add(spawnPoint);
            distances.put(spawnPoint.toString(), 0);

            while (!cellsToVisit.isEmpty()) {
                Cell currentCell = cellsToVisit.removeFirst();
                int currentDistance = distances.get(currentCell.toString());

                if (!currentCell.isStandable(model)) {

                    continue;
                }

                visited.clear();

                for (int y = currentCell.y; y >= currentCell.y - JUMP_HEIGHT; --y) {
                    Cell fallStartCell = new Cell(currentCell.x, y);

                    if (!fallStartCell.isValid(model)
                            || !fallStartCell.isPassable(model)) {
                        break;
                    }

                    limitedDfs(
                            fallStartCell,
                            currentCell.x - JUMP_HALF_WIDTH,
                            currentCell.x + JUMP_HALF_WIDTH,
                            currentCell.y - JUMP_HEIGHT,
                            model.getHeight() - 1,
                            currentDistance,
                            cellsToVisit,
                            distances,
                            visited,
                            model
                    );
                }
            }

            return distances;
        }

        private static void limitedDfs(Cell currentCell,
                                       int minX, int maxX,
                                       int minY, int maxY,
                                       int currentDistance,
                                       LinkedList<Cell> cellsToVisit,
                                       HashMap<String, Integer> distances,
                                       HashSet<String> visited, MarioLevelModel model
        ) {
            for (Cell adjacentCell : adjacentCells) {
                visited.add(currentCell.toString());
                Cell nextCell = new Cell(currentCell.x + adjacentCell.x, currentCell.y + adjacentCell.y);

                if (nextCell.isValid(minX, maxX, minY, maxY)
                        && nextCell.isValid(model)
                        && nextCell.isPassable(model)
                        && !visited.contains(nextCell.toString())) {

                    if (!distances.containsKey(nextCell.toString())) {
                        distances.put(nextCell.toString(), currentDistance + 1);
                        cellsToVisit.add(nextCell);
                    }

                    limitedDfs(
                            nextCell,
                            minX,
                            maxX,
                            minY,
                            maxY,
                            currentDistance,
                            cellsToVisit,
                            distances,
                            visited,
                            model
                    );
                }
            }
        }

        private static Cell findSpawnPoint(Cell searchStart, MarioLevelModel model) {
            Cell spawnPoint = new Cell(searchStart.x, searchStart.y);
            while (spawnPoint.y > 0 && !spawnPoint.isStandable(model)) {
                spawnPoint.y--;
            }

            return spawnPoint;
        }

        private static class Cell {
            public int x;
            public int y;

            public Cell(int x, int y) {
                this.x = x;
                this.y = y;
            }

            public boolean isPassable(MarioLevelModel model) {
                char tile = model.getBlock(x, y);
                return !MarioLevelModel.getSolidTilesList().contains(tile);
            }

            public boolean isStandable(MarioLevelModel model) {
                char tile = model.getBlock(x, y);
                char tileBelow = model.getBlock(x, y + 1);
                return !MarioLevelModel.getSolidTilesList().contains(tile)
                        && MarioLevelModel.getSolidTilesList().contains(tileBelow);
            }

            @Override
            public String toString() {
                return "(" + x + " " + y + ")";
            }

            public boolean isValid(MarioLevelModel model) {
                return x >= 0 && x < model.getWidth()
                        && y >= 0 && y < model.getHeight();
            }

            public boolean isValid(int xMin, int xMax, int yMin, int yMax) {
                return x >= xMin && x <= xMax
                        && y >= yMin && y <= yMax;
            }
        }
    }


    @Override
    public String getGeneratorName() {
        return "SamplerLevelGenerator";
    }
}
