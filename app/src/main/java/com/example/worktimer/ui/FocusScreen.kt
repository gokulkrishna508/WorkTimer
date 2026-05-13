package com.example.worktimer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worktimer.data.TimerState
import com.example.worktimer.viewmodel.TimeTrackerUiState
import com.example.worktimer.viewmodel.TimeTrackerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────
// Colours
// ──────────────────────────────────────────
private val PrimaryBlue: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF6EA3FF) else Color(0xFF2962FF)
private val LightBlue: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF17345F) else Color(0xFFE3F2FD)
private val TrackGray: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF2C3442) else Color(0xFFE8EAF0)
private val SurfaceWhite: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF171A22) else Color(0xFFFFFFFF)
private val BackgroundGray: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF0F1117) else Color(0xFFF4F6FA)
private val TextPrimary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFF4F7FB) else Color(0xFF1A1D26)
private val TextSecondary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFA5ADBA) else Color(0xFF6B7280)
private val BreakAmber: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFFB85C) else Color(0xFFFFA726)
private val StopRed: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFF6B6B) else Color(0xFFEF5350)
private val OvertimeRose: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFF6B6B) else Color(0xFFEF5350)
private val TimerInnerSurface: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1D2430) else Color(0xFFF0F4FF)
private val ControlTrack: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF242B36) else Color(0xFFF0F2F5)

@Composable
fun FocusScreen(viewModel: TimeTrackerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val targetReachedEventTime by viewModel.targetReachedEventTime.collectAsState()
    var showTargetDialog by remember { mutableStateOf(false) }
    var shownTargetReachedEventTime by rememberSaveable { mutableLongStateOf(0L) }
    var showTargetReachedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(targetReachedEventTime) {
        if (targetReachedEventTime > 0L && targetReachedEventTime != shownTargetReachedEventTime) {
            shownTargetReachedEventTime = targetReachedEventTime
            showTargetReachedDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Circular Timer ──
        CircularTimerSection(uiState)

        Spacer(modifier = Modifier.height(36.dp))

        // ── Session Control Card ──
        SessionControlCard(
            uiState = uiState,
            onWorkClick = { viewModel.onWorkingClicked() },
            onBreakClick = { viewModel.onBreakClicked() },
            onTargetClick = { showTargetDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Today's Focus Card ──
        TodayFocusCard(uiState)

        Spacer(modifier = Modifier.height(20.dp))

        // ── Stop Session Button ──
        if (uiState.state != TimerState.STOPPED && uiState.hasReachedTarget) {
            Button(
                onClick = { viewModel.onStopClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StopRed,
                    contentColor = Color.White
                )
            ) {
                Text("Stop & Save Session", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }

    // ── Edit Target Dialog ──
    if (showTargetDialog) {
        EditTargetDialog(
            currentTarget = uiState.targetHours,
            onDismiss = { showTargetDialog = false },
            onConfirm = { hours ->
                viewModel.onTargetHoursChanged(hours)
                showTargetDialog = false
            }
        )
    }

    if (showTargetReachedDialog) {
        DailyTargetReachedDialog(
            targetHours = uiState.targetHours,
            onDismiss = {
                viewModel.onTargetReachedDialogDismissed(shownTargetReachedEventTime)
                showTargetReachedDialog = false
            }
        )
    }
}

// ══════════════════════════════════════════
// Circular Timer
// ══════════════════════════════════════════
@Composable
private fun CircularTimerSection(uiState: TimeTrackerUiState) {
    val activeColor = when {
        uiState.isOvertime -> OvertimeRose
        uiState.state == TimerState.BREAK -> BreakAmber
        else -> PrimaryBlue
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(260.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = activeColor.copy(alpha = 0.08f),
                spotColor = activeColor.copy(alpha = 0.12f)
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(SurfaceWhite, TimerInnerSurface)
                )
            )
    ) {
        // Track
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(230.dp),
            color = TrackGray,
            strokeWidth = 10.dp,
            strokeCap = StrokeCap.Round
        )
        // Progress arc
        CircularProgressIndicator(
            progress = { uiState.progress },
            modifier = Modifier.size(230.dp),
            color = activeColor,
            strokeWidth = 10.dp,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val targetMs = (uiState.targetHours * 3600 * 1000).toLong()
            val remainingMs = (targetMs - uiState.totalWorkTodayMillis).coerceAtLeast(0L)
            
            Text(
                text = if (uiState.isOvertime) "OVERTIME" else "REMAINING TIME",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            val timeToDisplay = if (uiState.isOvertime) uiState.overtimeMillis else remainingMs
            val timeText = if (uiState.isOvertime) "+${formatTime(timeToDisplay)}" else formatTime(timeToDisplay)
            Text(
                text = timeText,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = if (uiState.isOvertime) OvertimeRose else TextPrimary,
                letterSpacing = (-1).sp
            )
            
            if (uiState.completionTimeMillis > 0) {
                Text(
                    text = if (uiState.isTargetCompletionTimeEstimated) 
                        "Finishes at ${formatClockTime(uiState.completionTimeMillis)}"
                        else "Goal reached at ${formatClockTime(uiState.completionTimeMillis)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (uiState.isOvertime) OvertimeRose.copy(alpha = 0.8f) else PrimaryBlue.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                uiState.isOvertime -> OvertimeRose
                                uiState.state == TimerState.WORKING -> PrimaryBlue
                                uiState.state == TimerState.BREAK -> BreakAmber
                                else -> TextSecondary
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        uiState.isOvertime -> "Overtime"
                        uiState.state == TimerState.WORKING -> "Deep Work"
                        uiState.state == TimerState.BREAK -> "On Break"
                        else -> "Idle"
                    },
                    color = when {
                        uiState.isOvertime -> OvertimeRose
                        uiState.state == TimerState.WORKING -> PrimaryBlue
                        uiState.state == TimerState.BREAK -> BreakAmber
                        else -> TextSecondary
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ══════════════════════════════════════════
// Session Control Card
// ══════════════════════════════════════════
@Composable
private fun SessionControlCard(
    uiState: TimeTrackerUiState,
    onWorkClick: () -> Unit,
    onBreakClick: () -> Unit,
    onTargetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Session Control",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                val targetLabel = uiState.targetHours.toString().removeSuffix(".0")
                Text(
                    text = "${targetLabel}h Target",
                    fontSize = 12.sp,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTargetClick() }
                        .background(LightBlue)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Toggle buttons ──
            ToggleButtonRow(
                isWorking = uiState.state == TimerState.WORKING,
                isBreak = uiState.state == TimerState.BREAK,
                isOvertime = uiState.isOvertime,
                onWorkClick = onWorkClick,
                onBreakClick = onBreakClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Elapsed / Break info ──
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoChip(
                    label = if (uiState.isOvertime) "OVERTIME" else "ELAPSED",
                    value = formatTime(
                        if (uiState.isOvertime) uiState.overtimeMillis else uiState.elapsedWorkMillis
                    ),
                    accentColor = if (uiState.isOvertime) OvertimeRose else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                InfoChip(
                    label = "BREAK",
                    value = formatTime(uiState.elapsedBreakMillis),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ══════════════════════════════════════════
// Toggle Button Row  (Working | On Break)
// ══════════════════════════════════════════
@Composable
private fun ToggleButtonRow(
    isWorking: Boolean,
    isBreak: Boolean,
    isOvertime: Boolean,
    onWorkClick: () -> Unit,
    onBreakClick: () -> Unit
) {
    val workBg by animateColorAsState(
        if (isWorking && isOvertime) OvertimeRose else if (isWorking) PrimaryBlue else ControlTrack,
        animationSpec = tween(250), label = "workBg"
    )
    val workFg by animateColorAsState(
        if (isWorking) Color.White else TextSecondary,
        animationSpec = tween(250), label = "workFg"
    )
    val breakBg by animateColorAsState(
        if (isBreak) BreakAmber else ControlTrack,
        animationSpec = tween(250), label = "breakBg"
    )
    val breakFg by animateColorAsState(
        if (isBreak) Color.White else TextSecondary,
        animationSpec = tween(250), label = "breakFg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(ControlTrack)
    ) {
        // Working toggle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(25.dp))
                .background(workBg)
                .clickable { onWorkClick() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Work,
                    contentDescription = "Working",
                    tint = workFg,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOvertime) "Overtime" else "Working",
                    color = workFg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        // Break toggle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(25.dp))
                .background(breakBg)
                .clickable { onBreakClick() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Coffee,
                    contentDescription = "On Break",
                    tint = breakFg,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "On Break",
                    color = breakFg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ══════════════════════════════════════════
// Info Chip
// ══════════════════════════════════════════
@Composable
private fun InfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BackgroundGray)
            .padding(14.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (label == "OVERTIME") accentColor else TextSecondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

// ══════════════════════════════════════════
// Today's Focus Card
// ══════════════════════════════════════════
@Composable
private fun TodayFocusCard(uiState: TimeTrackerUiState) {
    val totalHours = uiState.totalWorkTodayMillis / (1000f * 3600f)
    val progressPercent = (uiState.progress * 100).toInt()
    val graphColor = if (uiState.isOvertime) OvertimeRose else PrimaryBlue

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Today's Focus",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", totalHours),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isOvertime) OvertimeRose else TextPrimary
                )
                Text(
                    text = " hours",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = graphColor,
                trackColor = LightBlue,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(6.dp))
            val targetLabel = uiState.targetHours.toString().removeSuffix(".0")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isOvertime) {
                        "Overtime ${formatTime(uiState.overtimeMillis)} beyond ${targetLabel}h target"
                    } else {
                        "$progressPercent% of ${targetLabel}h target completed"
                    },
                    fontSize = 12.sp,
                    color = if (uiState.isOvertime) OvertimeRose else TextSecondary
                )
            }
        }
    }
}

// ══════════════════════════════════════════
// Edit Target Dialog
// ══════════════════════════════════════════
@Composable
private fun EditTargetDialog(
    currentTarget: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var text by remember { mutableStateOf(currentTarget.toString().removeSuffix(".0")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Target", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter your daily work target in hours (e.g. 0.1 for 6m):", color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hours = text.toFloatOrNull() ?: currentTarget
                onConfirm(hours.coerceIn(0.01f, 24f))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DailyTargetReachedDialog(
    targetHours: Float,
    onDismiss: () -> Unit
) {
    val targetLabel = targetHours.toString().removeSuffix(".0")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily target reached", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "You completed your ${targetLabel}h work target. The timer has been stopped and saved.",
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

// ══════════════════════════════════════════
// Formatter
// ══════════════════════════════════════════
fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

fun formatClockTime(millis: Long): String {
    if (millis <= 0) return "--:--"
    val date = Date(millis)
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}
