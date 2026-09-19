package com.picpuzzle.game.storage

import androidx.room.*

@Entity(tableName = "puzzles")
data class PuzzleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val gridSize: Int,
    val maxMoves: Int,
    val currentMoves: Int,
    val timeElapsed: Int,
    val isCompleted: Boolean,
    val tileStateJson: String
)

@Dao
interface PuzzleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePuzzle(puzzle: PuzzleEntity)

    @Query("SELECT * FROM puzzles WHERE id = :id")
    suspend fun getPuzzle(id: Int): PuzzleEntity?

    @Query("SELECT * FROM puzzles ORDER BY id DESC")
    suspend fun getAllPuzzles(): List<PuzzleEntity>
}

@Database(entities = [PuzzleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun puzzleDao(): PuzzleDao
}
