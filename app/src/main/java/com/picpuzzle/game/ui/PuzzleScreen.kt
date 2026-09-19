package com.picpuzzle.game.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as GColor
import android.graphics.Paint as GPaint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.picpuzzle.game.game.PuzzleBoardView
import com.picpuzzle.game.game.PuzzleEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class AppScreen {
    SPLASH, UPLOAD, DIFFICULTY, GAME, VICTORY, LEVELS, FRIENDS, PROFILE
}

@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var selectedGridSize by remember { mutableStateOf(4) }
    var maxMoves by remember { mutableStateOf(50) }
    var currentLevel by remember { mutableStateOf(1) }
    var movesUsed by remember { mutableStateOf(0) }
    var timeSeconds by remember { mutableStateOf(0) }
    
    val defaultPhotoFile = remember {
        val file = File(context.filesDir, "active_puzzle.webp")
        if (!file.exists()) {
            val bmp = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            val p = GPaint().apply { color = GColor.parseColor("#303F9F") }
            cv.drawRect(0f, 0f, 800f, 800f, p)
            p.color = GColor.parseColor("#FF4081")
            cv.drawCircle(400f, 400f, 250f, p)
            p.color = GColor.WHITE
            p.textSize = 80f
            p.isFakeBoldText = true
            cv.drawText("PicPuzzle", 210f, 430f, p)
            FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, out) }
        }
        file
    }
    
    var activeImageFile by remember { mutableStateOf(defaultPhotoFile) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(BitmapFactory.decodeFile(activeImageFile.absolutePath)) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val input = context.contentResolver.openInputStream(it)
                    val original = BitmapFactory.decodeStream(input)
                    input?.close()
                    if (original != null) {
                        val size = original.width.coerceAtMost(original.height)
                        val x = (original.width - size) / 2
                        val y = (original.height - size) / 2
                        val squared = Bitmap.createBitmap(original, x, y, size, size)
                        
                        val newFile = File(context.filesDir, "active_puzzle.webp")
                        FileOutputStream(newFile).use { out ->
                            squared.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, out)
                        }
                        activeImageFile = newFile
                        previewBitmap = squared
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val darkGradient = Brush.verticalGradient(listOf(Color(0xFF090D16), Color(0xFF13192B)))

    Scaffold(
        bottomBar = {
            if (currentScreen != AppScreen.SPLASH && currentScreen != AppScreen.GAME) {
                BottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkGradient)
                .padding(padding)
        ) {
            when (currentScreen) {
                AppScreen.SPLASH -> SplashScreen(
                    onGetStarted = { currentScreen = AppScreen.UPLOAD }
                )
                AppScreen.UPLOAD -> UploadScreen(
                    bitmap = previewBitmap,
                    onPickGallery = { galleryLauncher.launch("image/*") },
                    onNext = { currentScreen = AppScreen.DIFFICULTY }
                )
                AppScreen.DIFFICULTY -> DifficultyScreen(
                    onSelect = { size, moves ->
                        selectedGridSize = size
                        maxMoves = moves
                        currentScreen = AppScreen.GAME
                    },
                    onBack = { currentScreen = AppScreen.UPLOAD }
                )
                AppScreen.GAME -> GamePlayScreen(
                    imageFile = activeImageFile,
                    gridSize = selectedGridSize,
                    maxMoves = maxMoves,
                    level = currentLevel,
                    onSolved = { m, t ->
                        movesUsed = m
                        timeSeconds = t
                        currentScreen = AppScreen.VICTORY
                    },
                    onBack = { currentScreen = AppScreen.DIFFICULTY }
                )
                AppScreen.VICTORY -> VictoryScreen(
                    bitmap = previewBitmap,
                    moves = movesUsed,
                    maxMoves = maxMoves,
                    time = timeSeconds,
                    level = currentLevel,
                    onNextLevel = {
                        currentLevel++
                        currentScreen = AppScreen.GAME
                    },
                    onPlayAgain = { currentScreen = AppScreen.GAME }
                )
                AppScreen.LEVELS -> LevelsScreen(
                    currentLevel = currentLevel,
                    onSelectLevel = { lvl ->
                        currentLevel = lvl
                        currentScreen = AppScreen.GAME
                    }
                )
                AppScreen.FRIENDS -> FriendsScreen()
                AppScreen.PROFILE -> ProfileScreen()
            }
        }
    }
}

// 1. SPLASH SCREEN
@Composable
fun SplashScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Row {
            Box(modifier = Modifier.size(36.dp).background(Color(0xFFE91E63), RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.size(36.dp).background(Color(0xFF2196F3), RoundedCornerShape(8.dp)))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            Box(modifier = Modifier.size(36.dp).background(Color(0xFF9C27B0), RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.size(36.dp).background(Color(0xFFFF9800), RoundedCornerShape(8.dp)))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("PicPuzzle", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Turn Your Photos\nInto Puzzles", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.LightGray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Upload • Solve • Challenge • Fun", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFFE91E63)))),
                contentAlignment = Alignment.Center
            ) {
                Text("Get Started  ›", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Every Picture Tells a Puzzle", fontSize = 12.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 2. UPLOAD SCREEN
@Composable
fun UploadScreen(bitmap: Bitmap?, onPickGallery: () -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("Upload Your Photo", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Choose a photo from your gallery and\nturn it into a puzzle", fontSize = 14.sp, color = Color.LightGray, textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E2638))
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onPickGallery,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF9C27B0)))),
                contentAlignment = Alignment.Center
            ) {
                Text("🖼  Choose from Gallery", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF3F51B5))
        ) {
            Text("Create Puzzle  ›", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.weight(1f))
        Text("Your photos are private and only visible to you.", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(10.dp))
    }
}

// 3. DIFFICULTY SCREEN
@Composable
fun DifficultyScreen(onSelect: (size: Int, moves: Int) -> Unit, onBack: () -> Unit) {
    var selected by remember { mutableStateOf(1) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 32.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Select Difficulty", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Bigger grid, bigger challenge!", fontSize = 13.sp, color = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        DifficultyCard("Easy", "3 × 3 (9 pieces)", "30 Moves", Color(0xFF4CAF50), selected == 0) { selected = 0 }
        Spacer(modifier = Modifier.height(14.dp))
        DifficultyCard("Medium", "5 × 5 (25 pieces)", "50 Moves", Color(0xFF2196F3), selected == 1) { selected = 1 }
        Spacer(modifier = Modifier.height(14.dp))
        DifficultyCard("Hard", "8 × 8 (64 pieces)", "80 Moves", Color(0xFFFF9800), selected == 2) { selected = 2 }
        Spacer(modifier = Modifier.height(14.dp))
        DifficultyCard("Expert", "10 × 10 (100 pieces)", "100 Moves", Color(0xFF9C27B0), selected == 3) { selected = 3 }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                when (selected) {
                    0 -> onSelect(3, 30)
                    1 -> onSelect(5, 50)
                    2 -> onSelect(8, 80)
                    3 -> onSelect(10, 100)
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFFE91E63)))),
                contentAlignment = Alignment.Center
            ) {
                Text("Start Puzzle  ›", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun DifficultyCard(title: String, subtitle: String, moves: String, accentColor: Color, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF1E2842) else Color(0xFF141926)),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accentColor else Color(0xFF2A344D))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Text("🧩", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
            }
            Text(moves, color = accentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 4. GAMEPLAY SCREEN
@Composable
fun GamePlayScreen(
    imageFile: File,
    gridSize: Int,
    maxMoves: Int,
    level: Int,
    onSolved: (moves: Int, time: Int) -> Unit,
    onBack: () -> Unit
) {
    var moves by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }
    var showPreview by remember { mutableStateOf(false) }
    var boardKey by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            seconds++
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", fontSize = 30.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Level $level", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Solve the puzzle in $maxMoves moves", fontSize = 12.sp, color = Color.Gray)
            }
            Text("⏸", fontSize = 20.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF151928))
        ) {
            key(boardKey) {
                AndroidView(
                    factory = { ctx ->
                        PuzzleBoardView(ctx).apply {
                            val bmp = BitmapFactory.decodeFile(imageFile.absolutePath)
                            val (tiles, empty) = PuzzleEngine.generateSolvablePuzzle(bmp.width, bmp.height, gridSize, gridSize * gridSize * 7)
                            initialize(bmp, gridSize, tiles, empty.first, empty.second)
                            onMoveCompleted = { m -> moves = m }
                            onPuzzleSolved = { onSolved(moves, seconds) }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showPreview) {
                val fullBmp = remember { BitmapFactory.decodeFile(imageFile.absolutePath) }
                Image(bitmap = fullBmp.asImageBitmap(), contentDescription = "Preview", modifier = Modifier.fillMaxSize())
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Moves", color = Color.Gray, fontSize = 13.sp)
            Text("$moves / $maxMoves", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = (moves.toFloat() / maxMoves.toFloat()).coerceIn(0f, 1f),
            color = Color(0xFFE91E63),
            trackColor = Color(0xFF222B42),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(4.dp))
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Time", color = Color.Gray, fontSize = 13.sp)
            Text(String.format("%02d:%02d", seconds / 60, seconds % 60), color = Color.White, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            GameBottomButton("💡", "Hint") { }
            GameBottomButton("🔄", "Shuffle") { boardKey++ }
            GameBottomButton("👁", if (showPreview) "Hide" else "Preview") { showPreview = !showPreview }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun GameBottomButton(icon: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.size(54.dp).background(Color(0xFF1A2136), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = Color.LightGray, fontSize = 12.sp)
    }
}

// 5. VICTORY SCREEN
@Composable
fun VictoryScreen(bitmap: Bitmap?, moves: Int, maxMoves: Int, time: Int, level: Int, onNextLevel: () -> Unit, onPlayAgain: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.size(180.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E2840))) {
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Solved", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Awesome!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("You Solved It!", fontSize = 18.sp, color = Color.LightGray)

        Spacer(modifier = Modifier.height(28.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBox("Moves", "$moves / $maxMoves")
            StatBox("Time", String.format("%02d:%02d", time / 60, time % 60))
            StatBox("Level", "$level")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNextLevel,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFFE91E63)))),
                contentAlignment = Alignment.Center
            ) {
                Text("Next Level  ›", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "I just solved PicPuzzle Level $level in $moves moves! Can you beat me?")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Result"))
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(1.dp, Color(0xFF3F51B5))
        ) {
            Text("Share Result", color = Color.White, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF182035))) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 6. LEVELS SCREEN
@Composable
fun LevelsScreen(currentLevel: Int, onSelectLevel: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
        Text("Level Selection", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Keep solving and unlock new levels", fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(12) { index ->
                val lvl = index + 1
                val isUnlocked = lvl <= currentLevel
                Card(
                    modifier = Modifier.aspectRatio(1f).clickable(enabled = isUnlocked) { onSelectLevel(lvl) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isUnlocked) Color(0xFF1E2840) else Color(0xFF101420)),
                    border = BorderStroke(1.dp, if (lvl == currentLevel) Color(0xFFE91E63) else Color(0xFF263352))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isUnlocked) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$lvl", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (lvl < currentLevel) Text("✓", color = Color(0xFF4CAF50), fontSize = 16.sp)
                            }
                        } else {
                            Text("🔒", fontSize = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

// 7. FRIENDS SCREEN
@Composable
fun FriendsScreen() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("Challenge Friends", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Share your puzzle and challenge your\nfriends to beat your score!", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(30.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192138)), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("https://pixpuzzle.app/ch/AB12CD", color = Color.LightGray, fontSize = 13.sp)
                Text("📋", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SocialShareIcon("WhatsApp", Color(0xFF25D366)) { shareText(context, "Challenge me on PicPuzzle!") }
            SocialShareIcon("Instagram", Color(0xFFE1306C)) { shareText(context, "Challenge me on PicPuzzle!") }
            SocialShareIcon("Telegram", Color(0xFF0088CC)) { shareText(context, "Challenge me on PicPuzzle!") }
        }
    }
}

@Composable
fun SocialShareIcon(name: String, bg: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(54.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
            Text("🚀", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(name, color = Color.Gray, fontSize = 12.sp)
    }
}

fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Challenge"))
}

// 8. PROFILE SCREEN
@Composable
fun ProfileScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).background(Color(0xFF3F51B5), CircleShape), contentAlignment = Alignment.Center) {
                Text("👑", fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("RaviK", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Puzzle Lover", fontSize = 13.sp, color = Color(0xFFFFC107))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatBox("Levels", "12")
            StatBox("Completed", "5")
            StatBox("Best Time", "02:30")
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text("Leaderboard (Today)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(14.dp))

        val leaders = listOf(
            Triple("1 👑", "Aryan", "42 moves"),
            Triple("2 🥈", "Sneha", "46 moves"),
            Triple("3 🥉", "RaviK", "51 moves"),
            Triple("4", "Aditya", "58 moves"),
            Triple("5", "Pooja", "62 moves")
        )

        leaders.forEach { (rank, name, score) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rank, color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.width(40.dp))
                    Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Text(score, color = Color(0xFF2196F3), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Divider(color = Color(0xFF1E2840), thickness = 0.5.dp)
        }
    }
}

// 9. BOTTOM NAVIGATION
@Composable
fun BottomBar(currentScreen: AppScreen, onNavigate: (AppScreen) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0D121F)) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.UPLOAD,
            onClick = { onNavigate(AppScreen.UPLOAD) },
            icon = { Text("🏠", fontSize = 18.sp) },
            label = { Text("Home", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.LEVELS,
            onClick = { onNavigate(AppScreen.LEVELS) },
            icon = { Text("🎮", fontSize = 18.sp) },
            label = { Text("Levels", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = false,
            onClick = { onNavigate(AppScreen.UPLOAD) },
            icon = {
                Box(modifier = Modifier.size(44.dp).background(Color(0xFF2196F3), CircleShape), contentAlignment = Alignment.Center) {
                    Text("➕", color = Color.White, fontSize = 20.sp)
                }
            },
            label = {}
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.FRIENDS,
            onClick = { onNavigate(AppScreen.FRIENDS) },
            icon = { Text("👥", fontSize = 18.sp) },
            label = { Text("Friends", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.PROFILE,
            onClick = { onNavigate(AppScreen.PROFILE) },
            icon = { Text("👤", fontSize = 18.sp) },
            label = { Text("Profile", fontSize = 10.sp) }
        )
    }
}
