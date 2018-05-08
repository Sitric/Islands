package com.sitric;

import java.util.*;

/*
  Программа вычисляет количество островов (как общее количество, так и с уникальной структурой)
  в заданном двумерном массиве @param field.
  Островом считается любое количество единиц (>= 1), определенных отношением 4-связности.
  С целью демонстрации многопоточного выполнения поиска был реализован частный случай - исполнение в 2 потока.

@version 3.0 08-05-2018
@author Dmitry Syusin

 */

public class Solution{
    public static void main(String[] args) throws InterruptedException{
        IslandLogic islandLogic = new IslandLogic();

        myThread t1 = new myThread(0, IslandLogic.maxX/2);
        myThread t2 = new myThread(IslandLogic.maxX/2 + 1, IslandLogic.maxX);
        t1.setIslandLogic(islandLogic);
        t2.setIslandLogic(islandLogic);
        t1.start();
        t2.start();

        t1.join();
        t2.join();
        System.out.println("Количество островов: " + IslandLogic.getIslandCounts());
        System.out.println("Количество островов c уникальной структурой: " + IslandLogic.getUniqueIslands().size() + " " + IslandLogic.getUniqueIslands());

    }
}

class myThread extends Thread {
    private int fromIndex;
    private int toIndex;

    private int getFromIndex() {
        return fromIndex;
    }


    private int getToIndex() {
        return toIndex;
    }

    myThread(int from, int to) {
        this.fromIndex = from;
        this.toIndex = to;
    }
    private IslandLogic islandLogic;

    void setIslandLogic(IslandLogic islandLogic) {
        this.islandLogic = islandLogic;
    }

    @Override
    public void run() {
        try {
            islandLogic.findIslands(getFromIndex(),getToIndex());


        } catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            Thread.currentThread().interrupt();
        }

    }
}

class IslandLogic {
    private static int[][] field = {
            {1, 1, 0, 0, 0, 1},
            {0, 0, 0, 1, 0, 0},
            {0, 1, 1, 1, 1, 0},
            {1, 0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0, 0},
            {1, 1, 0, 1, 0, 1}
    };

    //Варианты смещения: UP, RIGHT, DOWN, LEFT
    private static final int[] dy = {-1, 0, 1, 0};
    private static final int[] dx = {0, 1, 0, -1};

    //поскольку поле делится по вертикали, то изменяться будет значение по X
    //ширина:
    static final int maxX = field[field.length - 1].length - 1;

    //высота:
    private static final int maxY = field.length - 1;

    private static int islandCounts = 0;
    private static HashSet<String> uniqueIslands = new HashSet<>();

    static int getIslandCounts() {
        return islandCounts;
    }

    private synchronized static void setIslandCounts(int islandCounts) {
        IslandLogic.islandCounts = islandCounts;
    }

    static HashSet<String> getUniqueIslands() {
        return uniqueIslands;
    }

    private StringBuilder islandName;

    private StringBuilder getIslandName() {
        return islandName;
    }

    private void setIslandName(StringBuilder islandName) {
        this.islandName = islandName;
    }

    // helper используется в методе проверки на пересечение острова границы полей
    private static List<String> helper;

    // для хранения данных о верней левой точке острова
    private static int[] topLeftPoint; // два значения: Y, X



    void findIslands(int fromIndex, int toIndex){

        int borderColumnFrom = (fromIndex == 0)? -1: fromIndex; // граничное значение для начала отрезка,
        //int borderColumnTo = (toIndex == maxX)? -1: toIndex;   // граничное значение для конца отрезка

        for (int i = 0; i <= maxY; i++) {
            for (int j = fromIndex; j <= toIndex; j++) {
                switch (field[i][j]) {
                    case -1:
                        break;
                    case 0:
                        field[i][j] = -1;
                        break;
                    case 1:
                        synchronized (this) {
                            exploreLandPoint(i,j, borderColumnFrom);
                            if (getIslandName() != null) {
                                uniqueIslands.add(getIslandName().toString());
                            }
                            break;
                        }
                }
            }
        }
    }

    private void exploreLandPoint (int y, int x, int borderColumnFrom){
        helper = new ArrayList<>();
        topLeftPoint = new int[]{-1, -1};

        int[] coords = getTopLeftPoint(y, x, borderColumnFrom);

        // если мы получили реальные координаты левой верхней точки, то начинаем описывать структуру острова
        if (coords[0] != -1 && coords[1] != -1) {
            setIslandCounts(getIslandCounts() + 1); // увеличим количество островов на 1
            setIslandName(new StringBuilder("I"));
            describeIslandStructure(coords[0], coords[1]);
        }
    }

    // возвращает координаты верхней левой точки острова (y, x) или (-1, -1) в случае, если текущий остров
    // на поле, которое обрабатывает поток слева.
    private int[] getTopLeftPoint (int y, int x, int borderColumnFrom) {
        if (topLeftPoint[0] < 0 | (topLeftPoint[0] >= 0 & topLeftPoint[0] > y)) {
            topLeftPoint[0] = y;
            topLeftPoint[1] = x;
        }
        else if (topLeftPoint[0] >= 0 & topLeftPoint[0] == y){
            topLeftPoint[1] = topLeftPoint[1] > x ? x : topLeftPoint[1];
        }

        helper.add("(" + x + "," + y + ")");

        for (int i = 0; i < 4; i++) {
            int x1 = x + dx[i];
            int y1 = y + dy[i];

            if (y1 <= maxY && y1 >= 0 && x1 <= maxX && x1 >= 0) {
                if (field[y1][x1] == 1) {
                    if (x1 < borderColumnFrom) {
                        topLeftPoint[0] = -1;
                        topLeftPoint[1] = -1;
                        break;
                    } else {
                        if (helper.indexOf("(" + x1 + "," + y1 + ")") == -1) {
                            getTopLeftPoint(y1, x1, borderColumnFrom);
                        }
                    }
                }
            }
        }
        return topLeftPoint;
    }

    private void describeIslandStructure (int y, int x){
        field[y][x] = -1;

        for (int i = 0; i < 4; i++) {
            int x1 = x + dx[i];
            int y1 = y + dy[i];
            if (y1 <= maxY && y1 >= 0 && x1 <= maxX && x1 >= 0) {
                if (field[y1][x1] == 1) {
                    switch (i){
                        case 0:
                            setIslandName(getIslandName().append("U"));
                            break;
                        case 1:
                            setIslandName(getIslandName().append("R"));
                            break;
                        case 2:
                            setIslandName(getIslandName().append("D"));
                            break;
                        case 3:
                            setIslandName(getIslandName().append("L"));
                            break;
                    }
                    field[y1][x1] = -1;
                    describeIslandStructure(y1, x1);
                }
            }
        }
    }
}





