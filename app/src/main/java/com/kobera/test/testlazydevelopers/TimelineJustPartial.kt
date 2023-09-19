package com.kobera.test.testlazydevelopers

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kobera.ui.toPx
import java.util.Calendar

@Composable
fun TimelineJustPart(
    start: () -> Long,
    end: () -> Long,
    currentTime: () -> Long,
    segmentSizePx: Int = 10.dp.toPx().toInt(), //size in Pixels as Int works best because we have to init bitmap Int wide
    segmentDuration: Long = 10_000,
) {
    val density = LocalDensity.current
    val millisInMinute = 60_000
    val minuteWidth = segmentSizePx * 6

    val currentProgressPercentage =
        ((currentTime() % millisInMinute).toFloat() / millisInMinute).let {
            if (it <= 0) 1.toFloat() else it
        }

    val timelineDifferenceFromStart = currentTime() - start()
    val timelineDifferenceToEnd = end() - currentTime()


    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val screenWidth = maxWidth
        //in center of screen is current time
        val screenSizeToCenterPx = maxWidth.toPx() / 2

        val fromSideToCurrentTime =
            ((maxWidth / 2).toPx() / segmentSizePx) * segmentDuration

        //move 0 seconds to center of screen (long line is in spot on currentTime)
        val offsetToCenter = screenSizeToCenterPx % minuteWidth

        //hide left side of timeline (at start of timeline)
        val leftHide: Dp = if (timelineDifferenceFromStart < fromSideToCurrentTime) {
            val percentageOfSide =
                1.0 - (timelineDifferenceFromStart.toDouble() / fromSideToCurrentTime)

            (percentageOfSide * maxWidth.value / 2).dp
        } else 0.dp

        //hide right side of timeline (at end of timeline)
        val rightHide: Dp = if (timelineDifferenceToEnd < fromSideToCurrentTime) {
            val percentageOfSide =
                1.0 - (timelineDifferenceToEnd.toDouble() / fromSideToCurrentTime)

            (percentageOfSide * maxWidth.value / 2).dp
        } else 0.dp

        Canvas(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(48.dp)
            .graphicsLayer {
                //moves timeline around
                translationX =
                    (((minuteWidth.unaryMinus()) + // bitmap move 1 minute to left
                            offsetToCenter - // move so long line on in center of screen
                            (currentProgressPercentage * minuteWidth))) + //current minute progress move
                            0.5.dp.toPx() // bitmap move 0.5px to right (cuz line is 1dp wide)
            }
        ) {
            //2 extra minutes so it can move around
            drawLines(density, segmentSizePx, width = (screenWidth).toPx() + minuteWidth * 2)
        }
        Spacer(
            Modifier
                .background(Color.Yellow)
                .size(width = leftHide, height = 48.dp)
                .align(Alignment.CenterStart),
        )
        Spacer(
            modifier = Modifier
                .background(Color.Red)
                .size(width = rightHide, height = 48.dp)
                .align(Alignment.CenterEnd),
        )
    }
}

fun DrawScope.drawLines(density: Density, segmentSizePx: Int, width: Float) {
    val bitmap = bitmapOfMinute(density, segmentSizePx)
    drawRect(
        size = size.copy(width = width),
        brush = ShaderBrush(
            ImageShader(
                bitmap,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Decal
            )
        )
    )
}

/**
 * creates 1 minute bitmap with 6 segments
 */
private fun bitmapOfMinute(density: Density, segmentSizePx: Int): ImageBitmap {
    val height = 24.dp.toPx(density)
    val bitmap = Bitmap.createBitmap(
        /*width = */(segmentSizePx * 6), //has to be Int !! (rounding Problems when using floating point)
        /* height = */height.toInt(),
        /* config = */Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()

    paint.style = android.graphics.Paint.Style.STROKE
    paint.isAntiAlias = true
    paint.isFilterBitmap = true
    paint.isDither = true
    paint.color = android.graphics.Color.BLACK
    paint.strokeWidth = 1.dp.toPx(density)

    canvas.drawLine(0f, 0f, 0f, height, paint)
    for (i in 1..5) {
        canvas.drawLine(
            i * segmentSizePx.toFloat(),
            (height / 2) - 6.dp.toPx(density), //center - half size (smaller line)
            i * segmentSizePx.toFloat(),
            (height / 2) + 6.dp.toPx(density), //center + half size (smaller line)
            paint
        )
    }
    //draws 2nd half of line at the end of bitmap
    canvas.drawLine(6 * segmentSizePx.toFloat(), 0f, 6 * segmentSizePx.toFloat(), height, paint)


    return bitmap.asImageBitmap()
}


@Preview
@Composable
fun PreviewTimelineJustPart() {

    val cl = Calendar.getInstance()

    cl.set(
        /* year = */ 2000,
        /* month = */1,
        /* date = */1,
        /* hourOfDay = */0,
        /* minute = */0,
        /* second = */60
    )

    val start = cl.timeInMillis

    cl.add(Calendar.MINUTE, 2)

    val end = cl.timeInMillis

    val events = listOf(
        TimeRange(start + 10_000, start + 20_000),
        TimeRange(start + 30_000, start + 100_000),
        TimeRange(start + 110_000, start + 120_000)
    )

    //val currentTime by animateFloatAsState(targetValue = start.toFloat(), label = "")
    Column {
        Box {
            TimelineJustPart(
                start = { start },
                end = { end },
                currentTime = { start + 19_000 },
                segmentDuration = 10_000
            )
            TimelineCurrent(
                Modifier
                    .align(Alignment.TopCenter)
                    .alpha(0.3f)
            )
        }
    }
}

