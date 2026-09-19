package com.picpuzzle.game.game

import android.graphics.Rect
import kotlin.random.Random

data class Tile(
    val id: Int,
    val correctX: Int,
    val correctY: Int,
    var currentX: Int,
    var currentY: Int,
    val sourceRect: Rect,
    val isEmpty: Boolean = false,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var scale: Float = 1f,
    var elevation: Boolean = false
)

object PuzzleEngine {
    fun generateSolvablePuzzle(imageWidth: Int, imageHeight: Int, gridSize: Int, difficultyShuffles: Int): Pair<Array<Tile>, Pair<Int, Int>> {
        val tiles = mutableListOf<Tile>()
        val tileW = imageWidth / gridSize
        val tileH = imageHeight / gridSize
        
        var id = 0
        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                val isEmpty = (x == gridSize - 1 && y == gridSize - 1)
                val srcRect = Rect(x * tileW, y * tileH, (x + 1) * tileW, (y + 1) * tileH)
                
                tiles.add(Tile(
                    id = id++,
                    correctX = x,
                    correctY = y,
                    currentX = x,
                    currentY = y,
                    sourceRect = srcRect,
                    isEmpty = isEmpty
                ))
            }
        }
        
        var emptyX = gridSize - 1
        var emptyY = gridSize - 1
        var prevX = -1
        var prevY = -1
        
        for (i in 0 until difficultyShuffles) {
            val possibleMoves = mutableListOf<Pair<Int, Int>>()
            if (emptyX > 0) possibleMoves.add(Pair(emptyX - 1, emptyY))
            if (emptyX < gridSize - 1) possibleMoves.add(Pair(emptyX + 1, emptyY))
            if (emptyY > 0) possibleMoves.add(Pair(emptyX, emptyY - 1))
            if (emptyY < gridSize - 1) possibleMoves.add(Pair(emptyX, emptyY + 1))
            
            possibleMoves.removeAll { it.first == prevX && it.second == prevY }
            
            val move = possibleMoves.random(Random(System.currentTimeMillis()))
            val tileToMove = tiles.find { it.currentX == move.first && it.currentY == move.second }
            
            tileToMove?.let {
                it.currentX = emptyX
                it.currentY = emptyY
                prevX = emptyX
                prevY = emptyY
                emptyX = move.first
                emptyY = move.second
            }
        }
        return Pair(tiles.toTypedArray(), Pair(emptyX, emptyY))
    }
}
