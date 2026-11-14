package com.ubahkan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.ui.components.UbahkanLogo
import kotlinx.coroutines.launch

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// Onboarding pertama buka: 4 halaman geser dengan indikator titik,
// tombol Lewati yang selalu terlihat, Lanjut, dan Mulai di halaman
// terakhir. Tampil menggantikan konten utama sampai flag
// onboardingDone disetel selesai oleh pemanggil.
private data class OnboardPage(
    val icon: ImageVector?,
    val title: String,
    val desc: String
)

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val pages = remember(strings) {
        listOf(
            OnboardPage(null, strings.onboardPage1Title, strings.onboardPage1Desc),
            OnboardPage(Icons.Outlined.FileUpload, strings.onboardPage2Title, strings.onboardPage2Desc),
            OnboardPage(Icons.Outlined.Tune, strings.onboardPage3Title, strings.onboardPage3Desc),
            OnboardPage(Icons.Outlined.DocumentScanner, strings.onboardPage4Title, strings.onboardPage4Desc)
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp)
        ) {
            // Lewati selalu terlihat di kanan atas
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = onGetStarted) {
                    Text(
                        text = strings.onboardSkip,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (page.icon == null) {
                        // Halaman sambutan memakai logo aplikasi
                        UbahkanLogo(size = 96.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = page.desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Indikator titik halaman
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (selected) 22.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isLastPage) {
                        onGetStarted()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text(
                    text = if (isLastPage) strings.btnGetStarted else strings.onboardNext,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
