package com.kobera.test.testlazydevelopers

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kobera.ui.toPx
import java.util.Calendar

@Composable
fun EventsJustPart(
    start: () -> Long,
    end: () -> Long,
    currentTime: () -> Long,
    segmentSizePx: Int = 10.dp.toPx().toInt(),
    segmentDuration: Long = 10_000,
    events: List<TimeRange>,
    color: Color
) {
    val visibleEvents = mutableListOf<TimeRange>()

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(48.dp)

    ) {
        val fromCurrentToStartOfEvents =
            (maxWidth.toPx() / segmentSizePx) * segmentDuration
        val startOfCanvas = currentTime() - fromCurrentToStartOfEvents.toLong()
        val endOfCanvas = currentTime() + fromCurrentToStartOfEvents.toLong()

        val visibleTimeRange = TimeRange(startOfCanvas, endOfCanvas)

        for(event in events) {
            if(event.overlaps(visibleTimeRange)){
                visibleEvents.add(event)
            }
        }


        Canvas(
            modifier = Modifier
                .width(maxWidth * 2)
                .height(48.dp)
                .offset(x = -(maxWidth) / 2)

        ) {
            for(event in visibleEvents) {
                drawEvent(
                    color = color,
                    event = event,
                    segmentDuration = segmentDuration,
                    segmentSizePx = segmentSizePx,
                    canvasStartTime = startOfCanvas,
                    canvasEndTime = endOfCanvas,
                    timelineStart = start(),
                    timelineEnd = end()
                )
            }
        }
    }
}

fun DrawScope.drawEvent(
    color: Color,
    event: TimeRange,
    segmentDuration: Long,
    segmentSizePx: Int,
    canvasStartTime: Long,
    canvasEndTime: Long,
    timelineStart: Long,
    timelineEnd: Long,
) {
    val startBarrier = maxOf(timelineStart, canvasStartTime)
    val endBarrier = minOf(timelineEnd, canvasEndTime)
    val eventTrimmed = event.trimmedBy(TimeRange(startBarrier, endBarrier))

    val eventWidthPx = (segmentSizePx * (eventTrimmed.getDuration().toFloat() / segmentDuration))
    val eventStartOffsetPx =
        segmentSizePx * ((eventTrimmed.start - canvasStartTime).toFloat() / segmentDuration)
    Log.d("drawEvent", "eventStartOffsetPx: $eventStartOffsetPx")
    val lineWidth = 12.dp

    if (eventWidthPx < lineWidth.toPx()) {
        drawCircle(
            color = color,
            center = Offset( eventStartOffsetPx + eventWidthPx / 2, center.y),
            radius = eventWidthPx / 2,
        )
    } else {
        drawLine(
            color = color,
            start = Offset(eventStartOffsetPx + (lineWidth / 2).toPx(), center.y),
            end = Offset(
                eventStartOffsetPx + eventWidthPx - (lineWidth / 2).toPx(),
                center.y
            ),
            strokeWidth = 12.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Preview
@Composable
fun EventsJustPartPreview() {
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
    val currentTime = start + 50_000

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
    ) {
        EventsJustPart(
            start = { start },
            end = { end },
            currentTime = { currentTime },
            events = events,
            color = Color.Red
        )
        TimelineJustPart(
            start = { start },
            end = { end },
            currentTime = { currentTime },
        )
        TimelineCurrent(Modifier.align(Alignment.Center))
    }
}