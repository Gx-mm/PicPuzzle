package com.picpuzzle.game.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.abs

class PuzzleBoardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var puzzleBitmap: Bitmap? = null
    private var gridSize: Int = 4
    private var tiles = emptyArray<Tile>()
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        setShadowLayer(18f, 0f, 10f, Color.argb(120, 0, 0, 0))
    }
    private val destRectF = RectF()
    private val backgroundPaint = Paint().apply { color = Color.parseColor("#0B0F19") }
    private val emptySlotPaint = Paint().apply { color = Color.parseColor("#151A2C") }

    private var tileSize = 0f
    private var activeTile: Tile? = null
    private var activeTileAnimator: ValueAnimator? = null
    
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var emptyCol = -1
    private var emptyRow = -1

    var onMoveCompleted: ((moves: Int) -> Unit)? = null
    var onPuzzleSolved: (() -> Unit)? = null
    private var movesUsed = 0

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun initialize(bitmap: Bitmap, size: Int, initialTiles: Array<Tile>, emptyX: Int, emptyY: Int) {
        this.puzzleBitmap = bitmap
        this.gridSize = size
        this.tiles = initialTiles
        this.emptyCol = emptyX
        this.emptyRow = emptyY
        this.movesUsed = 0
        calculateDimensions()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions()
    }

    private fun calculateDimensions() {
        val boardSize = width.coerceAtMost(height)
        if (gridSize > 0) {
            tileSize = boardSize / gridSize.toFloat()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (puzzleBitmap == null) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                val col = (event.x / tileSize).toInt().coerceIn(0, gridSize - 1)
                val row = (event.y / tileSize).toInt().coerceIn(0, gridSize - 1)

                if (isAdjacentToEmpty(col, row)) {
                    activeTile = getTileAt(col, row)
                    activeTile?.let {
                        lastTouchX = event.x
                        lastTouchY = event.y
                        activeTileAnimator?.cancel()
                        it.scale = 1.04f
                        it.elevation = true
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                activeTile?.let { tile ->
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    
                    if (tile.currentX == emptyCol) {
                        val limit = if (tile.currentY < emptyRow) tileSize else -tileSize
                        tile.offsetY = (tile.offsetY + dy).coerceIn(Math.min(0f, limit), Math.max(0f, limit))
                    } else {
                        val limit = if (tile.currentX < emptyCol) tileSize else -tileSize
                        tile.offsetX = (tile.offsetX + dx).coerceIn(Math.min(0f, limit), Math.max(0f, limit))
                    }
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeTile?.let { tile ->
                    val moved = abs(tile.offsetX) > tileSize / 2 || abs(tile.offsetY) > tileSize / 2
                    if (moved) {
                        val startX = tile.offsetX
                        val startY = tile.offsetY
                        val targetX = if (tile.currentX < emptyCol) tileSize else if (tile.currentX > emptyCol) -tileSize else 0f
                        val targetY = if (tile.currentY < emptyRow) tileSize else if (tile.currentY > emptyRow) -tileSize else 0f
                        animateTile(tile, startX, startY, targetX, targetY, true)
                    } else {
                        animateTile(tile, tile.offsetX, tile.offsetY, 0f, 0f, false)
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isAdjacentToEmpty(col: Int, row: Int): Boolean {
        return (abs(col - emptyCol) == 1 && row == emptyRow) || 
               (abs(row - emptyRow) == 1 && col == emptyCol)
    }

    private fun getTileAt(col: Int, row: Int): Tile? {
        return tiles.find { it.currentX == col && it.currentY == row && !it.isEmpty }
    }

    private fun animateTile(tile: Tile, startX: Float, startY: Float, endX: Float, endY: Float, isValidMove: Boolean) {
        activeTileAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 140
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                tile.offsetX = startX + (endX - startX) * fraction
                tile.offsetY = startY + (endY - startY) * fraction
                tile.scale = 1.04f - (0.04f * fraction)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tile.scale = 1.0f
                    tile.elevation = false
                    tile.offsetX = 0f
                    tile.offsetY = 0f
                    
                    if (isValidMove) {
                        val tempX = tile.currentX
                        val tempY = tile.currentY
                        tile.currentX = emptyCol
                        tile.currentY = emptyRow
                        emptyCol = tempX
                        emptyRow = tempY
                        
                        movesUsed++
                        onMoveCompleted?.invoke(movesUsed)
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        if (tiles.all { it.currentX == it.correctX && it.currentY == it.correctY }) {
                            onPuzzleSolved?.invoke()
                        }
                    }
                    activeTile = null
                    invalidate()
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        destRectF.set(emptyCol * tileSize, emptyRow * tileSize, (emptyCol + 1) * tileSize, (emptyRow + 1) * tileSize)
        canvas.drawRect(destRectF, emptySlotPaint)

        tiles.forEach { tile ->
            if (!tile.isEmpty && tile != activeTile) {
                drawTile(canvas, tile, paint)
            }
        }
        activeTile?.let { drawTile(canvas, it, shadowPaint) }
    }

    private fun drawTile(canvas: Canvas, tile: Tile, currentPaint: Paint) {
        val left = tile.currentX * tileSize + tile.offsetX
        val top = tile.currentY * tileSize + tile.offsetY
        val padding = 3f
        val centerOffset = (tileSize * (1f - tile.scale)) / 2f

        destRectF.set(
            left + padding + centerOffset,
            top + padding + centerOffset,
            left + tileSize - padding - centerOffset,
            top + tileSize - padding - centerOffset
        )
        puzzleBitmap?.let { bmp ->
            canvas.drawBitmap(bmp, tile.sourceRect, destRectF, currentPaint)
        }
    }
}
