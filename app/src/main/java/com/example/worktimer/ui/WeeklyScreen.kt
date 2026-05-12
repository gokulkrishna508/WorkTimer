package com.example.worktimer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worktimer.viewmodel.DayData
import com.example.worktimer.viewmodel.TimeTrackerViewModel
import com.example.worktimer.viewmodel.WeeklyUiState
import java.text.SimpleDateFormat
import java.util.Locale

// ──────────────────────────────────────────
// Colours (shared palette)
// ──────────────────────────────────────────
private val PrimaryBlue = Color(0xFF2962FF)
private val LightBlue = Color(0xFFE3F2FD)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val BackgroundGray = Color(0xFFF4F6FA)
private val TextPrimary = Color(0xFF1A1D26)
private val TextSecondary = Color(0xFF6B7280)
private val BarGradientStart = Color(0xFFBBDEFB)
private val BarGradientEnd = Color(0xFF2962FF)
private val BreakBar = Color(0xFFFFA726)

@Composable
fun WeeklyScreen(viewModel: TimeTrackerViewModel) {
    val state by viewModel.weeklyState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 32.dp)
    ) {
        // ── Header ──
        Text(
            text = "Weekly\nInsights",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            lineHeight = 38.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = weekSummaryText(state),
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Total Productive Time Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL PRODUCTIVE TIME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = Icons.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big number
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.formattedTotalH}h",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.formattedTotalM}m",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Bar chart ──
                BarChart(days = state.days)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Break Summary Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "BREAK TIME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                val breakH = state.totalBreakHours.toInt()
                val breakM = ((state.totalBreakHours - breakH) * 60).toInt()
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${breakH}h ${breakM}m",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BreakBar
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "total breaks",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Day-by-day breakdown ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Daily Breakdown",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                state.days.forEach { day ->
                    DayRow(day)
                    if (day != state.days.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFFF0F2F5)
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════
// Bar Chart
// ══════════════════════════════════════════
@Composable
private fun BarChart(days: List<DayData>) {
    val maxHours = (days.maxOfOrNull { it.workHours } ?: 1f).coerceAtLeast(1f)
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, day ->
            val fraction = day.workHours / maxHours
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600, delayMillis = index * 80),
                label = "bar_$index"
            )
            val barHeight = (animatedFraction * 100).dp.coerceAtLeast(4.dp)
            val label = dayLabels.getOrElse(index) { "?" }
            val isToday = index == days.size - 1 ||
                    isDateToday(day.date)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Hours label above bar
                if (day.workHours > 0f) {
                    Text(
                        text = String.format("%.1f", day.workHours),
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Bar
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (isToday) {
                                Brush.verticalGradient(
                                    listOf(PrimaryBlue.copy(alpha = 0.7f), PrimaryBlue)
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(BarGradientStart, BarGradientEnd.copy(alpha = 0.6f))
                                )
                            }
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Day label
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) PrimaryBlue else TextSecondary
                )
            }
        }
    }
}

// ══════════════════════════════════════════
// Day Row (breakdown list)
// ══════════════════════════════════════════
@Composable
private fun DayRow(day: DayData) {
    val dayName = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outFmt = SimpleDateFormat("EEEE", Locale.getDefault())
        val date = sdf.parse(day.date)
        if (date != null) outFmt.format(date) else day.date
    } catch (_: Exception) { day.date }

    val shortDate = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outFmt = SimpleDateFormat("MMM d", Locale.getDefault())
        val date = sdf.parse(day.date)
        if (date != null) outFmt.format(date) else ""
    } catch (_: Exception) { "" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (day.workHours > 0f) PrimaryBlue else Color(0xFFD0D5DD))
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = shortDate,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Work hours
        val wH = day.workHours.toInt()
        val wM = ((day.workHours - wH) * 60).toInt()
        Text(
            text = "${wH}h ${wM}m",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (day.workHours > 0f) TextPrimary else TextSecondary
        )
    }
}

// ══════════════════════════════════════════
// Helpers
// ══════════════════════════════════════════
private fun isDateToday(dateStr: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(java.util.Date())
        dateStr == today
    } catch (_: Exception) { false }
}

private fun weekSummaryText(state: WeeklyUiState): String {
    val totalH = state.totalWorkHours
    return if (totalH > 0f) {
        val avgDaily = totalH / 7f
        "You've logged ${String.format("%.1f", totalH)} hours this week. " +
                "That's an average of ${String.format("%.1f", avgDaily)}h per day."
    } else {
        "No sessions recorded this week yet. Start tracking to see your weekly insights!"
    }
}
