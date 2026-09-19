package com.picpuzzle.game.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.picpuzzle.game.game.PuzzleBoardView
import com.picpuzzle.game.game.PuzzleEngine
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PuzzleScreen(imageFile: File, gridSize: Int, maxMoves: Int, onBack: () -> Unit) {
    var moves by remember { mutableStateOf(0) }
    var timeElapsed by remember { mutableStateOf(0) }
    var isSolved by remember { mutableStateOf(false) }

    LaunchedEffect(isSolved) {
        while (!isSolved) {
            delay(1000L)
            timeElapsed++
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B0F19), Color(0xFF141A2E))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF))) {
                Text("Back", color = Color.White)
            }
            Text("Level: ${gridSize}x${gridSize}", color = Color.White, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1F33))
        ) {
            AndroidView(
                factory = { context ->
                    PuzzleBoardView(context).apply {
                        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                        val shuffles = gridSize * gridSize * 8
                        val (tiles, emptyState) = PuzzleEngine.generateSolvablePuzzle(
                            bitmap.width, bitmap.height, gridSize, shuffles
                        )
                        initialize(bitmap, gridSize, tiles, emptyState.first, emptyState.second)
                        onMoveCompleted = { m -> moves = m }
                        onPuzzleSolved = { isSolved = true }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Moves", color = Color.LightGray)
                    Text("$moves / $maxMoves", color = Color.White)
                }
                LinearProgressIndicator(
                    progress = (moves.toFloat() / maxMoves.toFloat()).coerceIn(0f, 1f),
                    color = Color(0xFFE91E63),
                    trackColor = Color(0xFF333A52),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Time", color = Color.LightGray)
                    val minutes = timeElapsed / 60
                    val seconds = timeElapsed % 60
                    Text(String.format("%02d:%02d", minutes, seconds), color = Color.White)
                }
            }
        }
    }
}
