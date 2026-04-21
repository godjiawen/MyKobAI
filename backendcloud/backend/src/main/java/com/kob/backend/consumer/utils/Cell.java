package com.kob.backend.consumer.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single grid cell in the game map with row and column coordinates.
 * 表示游戏地图中的单个网格格子，包含行列坐标。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Cell {
    int x, y;
}
