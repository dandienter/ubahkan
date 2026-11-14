
package com.ubahkan.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubahkan.app.R
import com.ubahkan.app.ui.theme.UbahkanDarkBackground
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow
import com.ubahkan.app.core.FileCategory
import com.ubahkan.app.core.FileCategoryManager
import com.ubahkan.app.core.AppStrings

// Shared visual constants
private val CardRadius = 16.dp
private val ControlRadius = 12.dp
private val ButtonRadius = 14.dp
private val StandardBorder = 1.dp

// ============================================================
// HOME TOP BAR
// Sesuai UBAHKAN_UI_UX_SPEC.md Section 7.1
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbahkanHomeTopBar(
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null
) {
    TopAppBar(
        modifier = modifier,
        title = {
            // Baris merek sesuai mockup: kotak hitam berisi logo U Panah
            // terang, lalu teks "Ubahkan" berbobot Medium.
            val isDark = MaterialTheme.colorScheme.background == UbahkanDarkBackground
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.onBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (isDark) R.drawable.ubahkan_logo else R.drawable.ubahkan_logo_dark
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = "Ubahkan",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        actions = {
            if (onSettingsClick != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            CircleShape
                        )
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = AppStrings.current.tabSettings,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ============================================================
// SECTION LABEL  -  label kecil uppercase berjarak (mockup)
// ============================================================

@Composable
fun UbahkanSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        modifier = modifier
    )
}

// ============================================================
// SCREEN TITLE HEADER (Tools, Settings)
// ============================================================

@Composable
fun UbahkanScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --------------------------------------------------
// LIVE CONVERSION PROGRESS BANNER
// --------------------------------------------------

@Composable
fun ConversionBanner(
    format: String,
    progress: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    fileName: String? = null,
    queueInfo: String? = null,
    stepMessage: String? = null,
    onClick: (() -> Unit)? = null
) {
    val safeProgress = progress.coerceIn(0, 100)
    val strings = AppStrings.current

    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress / 100f,
        animationSpec = tween(durationMillis = 350),
        label = "conversion_progress"
    )

    Card(
        modifier = if (onClick != null) modifier.fillMaxWidth().clickable { onClick() } else modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.55f
            )
        ),
        border = BorderStroke(
            StandardBorder,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = queueInfo ?: strings.convertingTo(format),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (fileName != null) "$fileName → ${format.uppercase()}" else strings.conversionInProgress,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.cancelConversion,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Progress track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surface
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stepMessage ?: strings.runningInBackground,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "$safeProgress%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// --------------------------------------------------
// ERROR & RETRY CARD
// --------------------------------------------------

@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = BorderStroke(
            StandardBorder,
            MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(ControlRadius))
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Text(
                    text = strings.conversionFailed,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(
                    alpha = 0.9f
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = strings.btnClose,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                if (onRetry != null) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(ButtonRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = strings.btnRetry,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------
// FILE HISTORY ITEM CARD
// --------------------------------------------------

@Composable
fun FileHistoryItemCard(
    file: File,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    conversionLabel: String? = null,
    savedPercent: Int? = null,
    showDivider: Boolean = true
) {
    val strings = AppStrings.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = strings.deleteFileDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = strings.deleteFileDialogMessage(file.name),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text(
                        text = strings.btnDelete,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                    }
                ) {
                    Text(strings.btnCancel)
                }
            }
        )
    }

    val ext = file.extension.uppercase()

    // Baris riwayat minimalis sesuai mockup: tile ikon kategori netral,
    // nama + metadata, jam di kanan, pemisah garis tipis di bawah.
    val categoryIcon = when (FileCategoryManager.getCategory(file.extension)) {
        FileCategory.VIDEO -> Icons.Outlined.Movie
        FileCategory.AUDIO -> Icons.Outlined.Audiotrack
        FileCategory.IMAGE -> Icons.Outlined.Image
        FileCategory.PDF -> Icons.Outlined.PictureAsPdf
        FileCategory.EBOOK -> Icons.Outlined.Book
        FileCategory.SPREADSHEET -> Icons.Outlined.TableChart
        FileCategory.ARCHIVE -> Icons.Outlined.FolderZip
        FileCategory.CODE -> Icons.Outlined.Code
        else -> Icons.Outlined.Description
    }
    val timeText = remember(file.lastModified()) {
        val date = java.util.Date(file.lastModified())
        val now = java.util.Calendar.getInstance()
        val fileCal = java.util.Calendar.getInstance().apply { time = date }
        val sameDay = now.get(java.util.Calendar.YEAR) == fileCal.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == fileCal.get(java.util.Calendar.DAY_OF_YEAR)
        val pattern = if (sameDay) "HH.mm" else "dd MMM"
        java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(date)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 13.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tile ikon kategori
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(13.dp))

            // File information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (conversionLabel != null) {
                    // Meta hasil konversi sesi ini: label konversi + info hemat biru
                    val accentColor = MaterialTheme.colorScheme.primary
                    val metaText = remember(conversionLabel, savedPercent, file.length()) {
                        buildAnnotatedString {
                            append(conversionLabel)
                            append(" · ")
                            append(formatFileSize(file.length()))
                            if (savedPercent != null) {
                                append(" - ")
                                withStyle(SpanStyle(color = accentColor)) {
                                    append(strings.historySavedPercent(savedPercent))
                                }
                            }
                        }
                    }
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "$ext · ${formatFileSize(file.length())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = timeText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // File actions
            Box {
                IconButton(
                    onClick = {
                        menuExpanded = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = strings.menuAction,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(strings.actionShare)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = strings.btnDelete,
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// --------------------------------------------------
// EMPTY HISTORY STATE
// Sesuai UBAHKAN_UI_UX_SPEC.md Section 7.3
// Circular 124dp action element with pulse animation
// --------------------------------------------------

@Composable
fun EmptyHistoryState(
    onPickFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation untuk outer ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val strings = AppStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Circular action element  -  124dp sesuai spec §7.3
        Box(
            modifier = Modifier
                .size(124.dp)
                .scale(scale)
                .clip(CircleShape)
                .clickable { onPickFileClick() },
            contentAlignment = Alignment.Center
        ) {
            // Outer ring  -  border circle with subtle stroke
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primaryContainer),
                        CircleShape
                    )
            )

            // Inner filled circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                // Folder icon
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )

                // Plus badge  -  pojok kanan bawah folder icon
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.selectYourFile,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = strings.tapToSelectOrShare,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// --------------------------------------------------
// MOTION HELPERS  -  gerakan halus sesuai mockup UI 1.3.0
// Easing keluar cepat lalu melambat lembut (kurva mockup),
// durasi pendek, jeda berjenjang 55ms antar elemen.
// --------------------------------------------------

val UbahkanEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun StaggerReveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // rememberSaveable: animasi masuk hanya diputar sekali per sesi layar,
    // tidak berulang saat item daftar digulir keluar-masuk.
    var visible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            tween(durationMillis = 380, delayMillis = index * 55, easing = UbahkanEaseOut)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 380, delayMillis = index * 55, easing = UbahkanEaseOut)
        ) { it / 6 }
    ) {
        content()
    }
}

// Pasangan interactionSource + modifier skala tekan 0.98 untuk
// tombol utama: tombol terasa merespons tanpa animasi berlebihan.
@Composable
fun rememberPressScale(): Pair<MutableInteractionSource, Modifier> {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 140, easing = UbahkanEaseOut),
        label = "press_scale"
    )
    val modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    return interactionSource to modifier
}

// --------------------------------------------------
// CATEGORY FILTER CHIPS  -  pil: terpilih hitam, lainnya putih
// bergaris. Dipakai Beranda (saring alat populer) dan Riwayat.
// --------------------------------------------------

data class CategoryFilterItem(val id: String, val label: String)

@Composable
fun UbahkanCategoryFilter(
    categories: List<CategoryFilterItem>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(categories, key = { it.id }) { categoryItem ->
            val isSelected = selectedCategoryId == categoryItem.id
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(categoryItem.id) },
                label = {
                    Text(
                        text = categoryItem.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.onBackground,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}

// --------------------------------------------------
// TEXT TABS  -  tab teks bergaris bawah (UI 1.4.0)
// Dipakai Beranda (saring alat populer) dan Riwayat (saring
// kategori): tab aktif teks tinta + garis bawah tinta 2dp,
// tanpa chip yang terpotong di tepi layar.
// --------------------------------------------------

@Composable
fun UbahkanTextTabs(
    categories: List<CategoryFilterItem>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            categories.forEach { categoryItem ->
                val isSelected = selectedCategoryId == categoryItem.id
                Column(
                    modifier = Modifier
                        .clickable { onCategorySelected(categoryItem.id) }
                        .padding(top = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = categoryItem.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onBackground
                                else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

// --------------------------------------------------
// TOOL GRID CARD  -  kartu alat mode kotak (UI 1.4.0)
// Kartu putih melayang radius 20dp, tinggi seragam 128dp,
// ikon di ubin biru pucat, judul dan deskripsi maks 2 baris.
// Dipakai grid Alat Populer di Beranda dan mode Grid tab Alat.
// --------------------------------------------------

@Composable
fun ToolGridCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(128.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --------------------------------------------------
// FILE SIZE FORMATTER
// --------------------------------------------------

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (
        log10(bytes.toDouble()) / log10(1024.0)
    ).toInt().coerceIn(0, units.lastIndex)

    val size = bytes / 1024.0.pow(digitGroups.toDouble())

    return DecimalFormat("#,##0.#").format(size) + " " + units[digitGroups]
}
