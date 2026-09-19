package com.picpuzzle.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.picpuzzle.game.ui.PuzzleScreen
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fallback default puzzle image offline create karein
        val defaultImage = File(filesDir, "default_puzzle.webp")
        if (!defaultImage.exists()) {
            val bmp = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint().apply { color = Color.parseColor("#3F51B5") }
            canvas.drawRect(0f, 0f, 800f, 800f, paint)
            paint.color = Color.WHITE
            paint.textSize = 60f
            canvas.drawText("PicPuzzle", 250f, 420f, paint)
            FileOutputStream(defaultImage).use { out ->
                bmp.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, out)
            }
        }

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                PuzzleScreen(
                    imageFile = defaultImage,
                    gridSize = 4,
                    maxMoves = 40,
                    onBack = { finish() }
                )
            }
        }
    }
}
