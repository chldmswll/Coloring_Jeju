package com.example.coloringjeju.ui.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.components.BottomSheetHeader
import com.example.coloringjeju.ui.components.CircularProgress
import com.example.coloringjeju.ui.components.ColorSwatchPicker
import com.example.coloringjeju.ui.components.ColoringCard
import com.example.coloringjeju.ui.components.ColoringCardVariant
import com.example.coloringjeju.ui.components.ColoringIconButton
import com.example.coloringjeju.ui.components.ColoringIconButtonVariant
import com.example.coloringjeju.ui.components.ColoringTabItem
import com.example.coloringjeju.ui.components.ColoringTag
import com.example.coloringjeju.ui.components.FilterChipRow
import com.example.coloringjeju.ui.components.LinearProgress
import com.example.coloringjeju.ui.components.MapPinRow
import com.example.coloringjeju.ui.components.PrimaryButton
import com.example.coloringjeju.ui.components.SearchInput
import com.example.coloringjeju.ui.components.SecondaryButton
import com.example.coloringjeju.ui.components.SegmentedControl
import com.example.coloringjeju.ui.components.StampCard
import com.example.coloringjeju.ui.components.StatusBanner
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * Living style guide — a Compose port of `style-guide.html`. Every section below renders the
 * real production components from `ui/components`, so this screen doubles as a visual regression
 * check: if a component's look drifts from the tokens, it drifts here too.
 */
@Composable
fun DesignSystemShowcaseScreen(modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    val spacing = ColoringTheme.spacing

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.offWhite),
        contentPadding = PaddingValues(horizontal = spacing.space6, vertical = spacing.space8),
        verticalArrangement = Arrangement.spacedBy(spacing.space10),
    ) {
        item {
            Column {
                Text(
                    "Coloring Korea · Design System",
                    style = ColoringTheme.typography.caption,
                    color = colors.primaryDark,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(spacing.space2))
                Text("Components", style = ColoringTheme.typography.display, color = colors.textPrimary)
            }
        }

        item { ColorsSection() }
        item { TypographySection() }
        item { ButtonsSection() }
        item { ChipsAndSegmentedSection() }
        item { TabBarSection() }
        item { AppHeaderSection() }
        item { StampCardSection() }
        item { ProgressSection() }
        item { ColorSwatchPickerSection() }
        item { StatusAndTagSection() }
        item { SearchInputSection() }
        item { BottomSheetSection() }
        item { MapPinSection() }
        item { CardContainerSection() }
    }
}

@Composable
private fun SectionTitle(title: String) {
    val colors = ColoringTheme.colors
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border),
        )
        Spacer(Modifier.height(ColoringTheme.spacing.space5))
        Text(title, style = ColoringTheme.typography.title, color = colors.textPrimary, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text.uppercase(),
        style = ColoringTheme.typography.caption,
        color = ColoringTheme.colors.textTertiary,
        modifier = Modifier.padding(bottom = 10.dp, top = 4.dp),
    )
}

// ---------- Colors ----------
private data class Swatch(val name: String, val color: Color)

@Composable
private fun ColorsSection() {
    val colors = ColoringTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle("Colors")
        Spacer(Modifier.height(8.dp))

        GroupLabel("Brand")
        SwatchGrid(
            listOf(
                Swatch("Primary", colors.primary),
                Swatch("Primary Dark", colors.primaryDark),
                Swatch("Primary Deepest", colors.primaryDeepest),
                Swatch("Forest", colors.forest),
            ),
        )
        GroupLabel("Accent")
        SwatchGrid(
            listOf(
                Swatch("Yellow", colors.yellow),
                Swatch("Teal", colors.teal),
                Swatch("Teal Light", colors.tealLight),
                Swatch("Pink", colors.pink),
                Swatch("Orange", colors.orange),
            ),
        )
        GroupLabel("Surface")
        SwatchGrid(
            listOf(
                Swatch("White", colors.white),
                Swatch("Cream", colors.cream),
                Swatch("Mint", colors.mint),
                Swatch("Off White", colors.offWhite),
                Swatch("Border", colors.border),
            ),
        )
        GroupLabel("Text")
        SwatchGrid(
            listOf(
                Swatch("Primary", colors.textPrimary),
                Swatch("Secondary", colors.textSecondary),
                Swatch("Tertiary", colors.textTertiary),
            ),
        )
    }
}

@Composable
private fun SwatchGrid(swatches: List<Swatch>) {
    val colors = ColoringTheme.colors
    val rows = swatches.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { swatch ->
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(ColoringTheme.shapes.md)
                                .background(swatch.color)
                                .border(1.dp, colors.border, ColoringTheme.shapes.md),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(swatch.name, style = ColoringTheme.typography.caption, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            "#%06X".format(0xFFFFFF and swatch.color.toArgb()),
                            style = ColoringTheme.typography.caption,
                            color = colors.textTertiary,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---------- Typography ----------
@Composable
private fun TypographySection() {
    val colors = ColoringTheme.colors
    Column {
        SectionTitle("Typography")
        Spacer(Modifier.height(16.dp))
        TypeRow("Display 24/800") { Text("제주 컬러 지도", style = ColoringTheme.typography.display, color = colors.textPrimary) }
        TypeRow("Title 18/700") { Text("위치 인증", style = ColoringTheme.typography.title, color = colors.textPrimary) }
        TypeRow("Subtitle 14/600") { Text("이번 여행의 무지개를 채워보세요", style = ColoringTheme.typography.subtitle, color = colors.textPrimary) }
        TypeRow("Body 14/400") { Text("현재 위치 · 목적지에서 약 40m", style = ColoringTheme.typography.body, color = colors.textSecondary) }
        TypeRow("Button 16/700") { Text("카메라 열기", style = ColoringTheme.typography.button, color = colors.textPrimary) }
        TypeRow("Caption 12/500") { Text("반경 200m", style = ColoringTheme.typography.caption, color = colors.textTertiary) }
    }
}

@Composable
private fun TypeRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            label,
            style = ColoringTheme.typography.caption,
            color = ColoringTheme.colors.textTertiary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 20.dp),
        )
        content()
    }
}

// ---------- Buttons ----------
@Composable
private fun ButtonsSection() {
    Column {
        SectionTitle("Buttons")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(text = "카메라 열기", onClick = {})
            SecondaryButton(text = "다시 찍기", onClick = {})
            PrimaryButton(text = "미션 완료", onClick = {}, enabled = false)
            ColoringIconButton(onClick = {}, variant = ColoringIconButtonVariant.Yellow) {
                Text("+", style = ColoringTheme.typography.title, color = ColoringTheme.colors.forest)
            }
            ColoringIconButton(onClick = {}, variant = ColoringIconButtonVariant.Primary) {
                Text("✓", style = ColoringTheme.typography.title, color = ColoringTheme.colors.white)
            }
            ColoringIconButton(onClick = {}, variant = ColoringIconButtonVariant.Outline) {
                Text("‹", style = ColoringTheme.typography.title, color = ColoringTheme.colors.textSecondary)
            }
        }
    }
}

// ---------- Chips & Segmented ----------
@Composable
private fun ChipsAndSegmentedSection() {
    var segment by remember { mutableStateOf("추천 지도") }
    var chip by remember { mutableStateOf("전체") }
    Column {
        SectionTitle("Chips & Segmented")
        Spacer(Modifier.height(16.dp))
        GroupLabel("Segmented control")
        SegmentedControl(
            options = listOf("추천 지도", "MY 지도"),
            selected = segment,
            onSelect = { segment = it },
            label = { it },
        )
        Spacer(Modifier.height(20.dp))
        GroupLabel("Filter chips")
        FilterChipRow(
            options = listOf("전체", "자연", "카페", "맛집"),
            selected = chip,
            onSelect = { chip = it },
            label = { it },
        )
    }
}

// ---------- Bottom Tab Bar ----------
@Composable
private fun TabBarSection() {
    var selected by remember { mutableStateOf(0) }
    Column {
        SectionTitle("Bottom Tab Bar")
        Spacer(Modifier.height(16.dp))
        BottomTabBar(
            items = listOf("홈", "스탬프", "그룹", "조각", "마이").map { ColoringTabItem(it) },
            selectedIndex = selected,
            onSelect = { selected = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------- App Header ----------
@Composable
private fun AppHeaderSection() {
    val colors = ColoringTheme.colors
    Column {
        SectionTitle("App Header")
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .clip(ColoringTheme.shapes.lg)
                .background(colors.white)
                .border(1.dp, colors.border, ColoringTheme.shapes.lg),
        ) {
            AppHeader(title = "위치 인증", subtitle = "천지연폭포", onBack = {})
        }
    }
}

// ---------- Stamp Card ----------
@Composable
private fun StampCardSection() {
    val colors = ColoringTheme.colors
    Column {
        SectionTitle("List / Stamp Card")
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StampCard(
                title = "성산일출봉",
                subtitle = "주황·붉은 (일출) · 인증완료",
                isDone = true,
                icon = { Text("⛰️", style = ColoringTheme.typography.title) },
            )
            StampCard(
                title = "천지연폭포",
                subtitle = "청록 계열 · 미방문",
                isDone = false,
                icon = { Text("💧", style = ColoringTheme.typography.title) },
            )
        }
    }
}

// ---------- Progress ----------
@Composable
private fun ProgressSection() {
    Column {
        SectionTitle("Progress")
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            LinearProgress(progress = 0.5f, caption = "선형 진행바 (3/6)", modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgress(progress = 0.78f)
                Spacer(Modifier.height(8.dp))
                Text("원형 게이지 (78%)", style = ColoringTheme.typography.caption, color = ColoringTheme.colors.textSecondary)
            }
        }
    }
}

// ---------- Color Swatch Picker ----------
@Composable
private fun ColorSwatchPickerSection() {
    val colors = ColoringTheme.colors
    var selected by remember { mutableStateOf(colors.primaryDeepest) }
    Column {
        SectionTitle("Color Swatch Picker")
        Spacer(Modifier.height(16.dp))
        ColorSwatchPicker(
            options = listOf(colors.tealLight, colors.teal, colors.primaryDeepest),
            selected = selected,
            onSelect = { selected = it },
        )
    }
}

// ---------- Status & Tag ----------
@Composable
private fun StatusAndTagSection() {
    Column {
        SectionTitle("Status & Tag")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusBanner("✓ 인증 성공!")
            ColoringTag("자연")
        }
    }
}

// ---------- Search Input ----------
@Composable
private fun SearchInputSection() {
    var query by remember { mutableStateOf("") }
    Column {
        SectionTitle("Search Input")
        Spacer(Modifier.height(16.dp))
        SearchInput(value = query, onValueChange = { query = it }, placeholder = "장소 검색 (예: 우도, 카페)")
    }
}

// ---------- Bottom Sheet ----------
@Composable
private fun BottomSheetSection() {
    val colors = ColoringTheme.colors
    Column {
        SectionTitle("Bottom Sheet")
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .clip(ColoringTheme.shapes.lg)
                .background(colors.cream)
                .border(1.dp, colors.border, ColoringTheme.shapes.lg),
        ) {
            BottomSheetHeader(title = "내 지도에 여행지 추가하기", subtitle = "위로 당겨서 추천 목록 보기 · 총 1곳 추가됨")
        }
    }
}

// ---------- Map Pin ----------
@Composable
private fun MapPinSection() {
    val colors = ColoringTheme.colors
    Column {
        SectionTitle("Map Pin")
        Spacer(Modifier.height(16.dp))
        MapPinRow(
            items = listOf(
                "협재" to colors.teal,
                "한라산" to null,
                "성산일출봉" to colors.orange,
            ),
        )
    }
}

// ---------- Card Container ----------
@Composable
private fun CardContainerSection() {
    Column {
        SectionTitle("Card Container")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ColoringCard(
                title = "기본 카드",
                subtitle = "흰 배경 + 연한 보더",
                variant = ColoringCardVariant.Default,
                modifier = Modifier.weight(1f),
            )
            ColoringCard(
                title = "민트 카드",
                subtitle = "지도/인증 안내에 사용",
                variant = ColoringCardVariant.Mint,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
