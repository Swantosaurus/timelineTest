package com.kobera.test.testlazydevelopers

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.animation.addListener
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kobera.ui.theme.TestLazyDevelopersTheme
import com.kobera.ui.toPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.absoluteValue
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

val FMP4_SEGMENT_DURATION = 2.seconds
val PLAYER_UPDATE_INTERVAL = 100.milliseconds
val GENERATED_EVENTS = 5_000

class MainActivity : ComponentActivity() {
    val cl = Calendar.getInstance()
    private var start: MutableStateFlow<Long>
    private var end: MutableStateFlow<Long>
    val fakeStart: MutableStateFlow<Long>
    val fakeEnd: MutableStateFlow<Long>
    val events: MutableList<TimeRange>
    val currentTime: MutableStateFlow<Long>
    val isPlaying = MutableStateFlow(true)


    val fakeCurrent: MutableStateFlow<Long>

    private val flingAnimator = FlingAnimator()

    private inner class FlingAnimator : ValueAnimator() {
        var timeVelocity = 0.0
        var currentTimeMem = 0L

        /**
         * integration of 1 - x --> linear velocity slowdown from 100% to 0%
         * Riemann integral (from 0 to x) of this  is distance traveled in time
         */
        private fun curve(x: Float): Float {
            return (x * x * -1 / 2) + x
        }

        init {
            setFloatValues(0f, 1f)
            setEvaluator { fraction, _, _ ->
                curve(fraction)
            }
            addListener(
                onEnd = {
                   Log.d("onEnd", "onEnd")
                    //TODO call exoPlayer toSeek
                }
            )
            addUpdateListener {
                val value = it.animatedValue as Float
                val seekTime = currentTimeMem + (value * timeVelocity).toLong()
                seek(seekTime)
            }
        }

        fun startWithVelocity(timeVelocity: Double, animationStartTime: Long) {
            duration = sqrt(timeVelocity.absoluteValue/2).toLong()
            this.timeVelocity = timeVelocity
            currentTimeMem = animationStartTime
            start()
        }
    }


    init {
        cl.set(
            /* year = */ 2000,
            /* month = */ 1,
            /* date = */ 1,
            /* hourOfDay = */ 0,
            /* minute = */ 0,
            /* second = */ 20
        )

        start = MutableStateFlow(cl.timeInMillis)
        currentTime = MutableStateFlow(cl.timeInMillis)
        cl.add(Calendar.MONTH, 1)

        end = MutableStateFlow(cl.timeInMillis)


        fakeStart = MutableStateFlow(start.value)
        fakeEnd = MutableStateFlow(end.value)
        fakeCurrent = MutableStateFlow(currentTime.value)


        start.value.let { start ->
            events = mutableListOf(
                TimeRange(start, start + 200_000)
            )
            for (i in (1..GENERATED_EVENTS *2).step(2)) {
                val nextEventStart = start + 500_000 + i * 10_000
                events.add(TimeRange(nextEventStart, nextEventStart + 10_000))
            }
        }

        /**
         * simulatingPlayer
         */
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(PLAYER_UPDATE_INTERVAL)
                if (isDragging.value) continue
                if (!isPlaying.value) continue
                currentTime.value += PLAYER_UPDATE_INTERVAL.toLong(DurationUnit.MILLISECONDS) //+ (-3..3).random()
            }
        }


        /**
         * simulating Backend updates
         */
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(FMP4_SEGMENT_DURATION)
                start.value += FMP4_SEGMENT_DURATION.toLong(DurationUnit.MILLISECONDS) + (-30..30).random()
                end.value += FMP4_SEGMENT_DURATION.toLong(DurationUnit.MILLISECONDS) + (-30..30).random()

                fakeStart.value = start.value
                fakeEnd.value = end.value
            }
        }
    }

    fun setPlaying(to: Boolean) {
        isPlaying.value = to
    }

    fun seek(to: Long) {
        currentTime.value = to
    }


    fun flingSeeking(timeVelocity: Double) {
        if (timeVelocity !in (-30_000.0..30_000.0)) {
            flingAnimator.startWithVelocity(
                timeVelocity = timeVelocity,
                animationStartTime = currentTime.value
            )
        } else {
            //TODO call exoPlayer to seek
        }
    }


    fun dragSeeking(dragOffset: Long) {
        currentTime.value += dragOffset
    }

    val isDragging = MutableStateFlow(false)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TestLazyDevelopersTheme {
                Scaffold {
                    val startState by fakeStart.collectAsStateWithLifecycle()
                    val endState by fakeEnd.collectAsStateWithLifecycle()
                    val currentState by currentTime.collectAsStateWithLifecycle()
                    val isPlayingState by isPlaying.collectAsStateWithLifecycle()

                    val segmentSize = 10.dp.toPx().toInt()


                    var prevousX by remember {
                        mutableStateOf(0f)
                    }
                    var velocityPxPerSecond by remember {
                        mutableStateOf(0f)
                    }

                    Column(
                        Modifier
                            .padding(it)
                            .fillMaxSize()
                    ) {
                        /**
                         * Main Timeline UI Logic is in this part
                         */
                        Box(Modifier
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        flingSeeking(timeVelocity = (velocityPxPerSecond / 4 / segmentSize * 10_000).toDouble())
                                        velocityPxPerSecond = 0f
                                        isDragging.value = false
                                    }
                                ) { change, dragAmountPx ->

                                    isDragging.value = true
                                    val dragSeekAmount =
                                        (-dragAmountPx.toDouble()) / segmentSize * 10_000
                                    dragSeeking(dragSeekAmount.toLong())
                                    val timeDelta =
                                        change.uptimeMillis - change.previousUptimeMillis

                                    if(!(timeDelta <= 0)){
                                        velocityPxPerSecond =
                                            (change.position.x - prevousX).unaryMinus() * (1000f / timeDelta)

                                        prevousX = change.position.x
                                    }
                                }
                            }) {

                            EventsJustPart(
                                start = { startState },
                                end = { endState },
                                currentTime = { currentState },
                                segmentDuration = 10_000,
                                events = events,
                                color = Color.Magenta
                            )
                            TimelineJustPart(
                                start = { startState },
                                end = { endState },
                                currentTime = { currentState },
                                segmentDuration = 10_000
                            )

                            TimelineCurrent(
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .alpha(0.3f)
                            )
                        }

                        /**
                         * just buttons and utils extra to test
                         */
                        JustUselessHelperUtils(isPlayingState = isPlayingState, setPlaying = ::setPlaying, currentTime = currentState)
                    }
                }
            }
        }
    }
}

val dateFormat = DateFormat.getDateTimeInstance()
@Composable
private fun JustUselessHelperUtils(isPlayingState: Boolean, setPlaying: (Boolean) -> Unit, currentTime : Long) {
    val date = Date(currentTime)
    val currentString = dateFormat.format(date)
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("IsPlaying: $isPlayingState")
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = {
            setPlaying(true)
        }) {
            Text(text = "Play")
        }
        TextButton(onClick = {
            setPlaying(false)
        }) {
            Text(text = "Stop")
        }
    }

    Text(currentString, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
fun TimelineCurrent(modifier: Modifier = Modifier){
    Spacer(
        Modifier
            .alpha(0.3f)
            .size(width = 4.dp, height = 48.dp)
            .background(
                color = Color.Blue,
                shape = RoundedCornerShape(2.dp)
            )
            .then(modifier)
    )
}
