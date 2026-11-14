package com.ubahkan.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ubahkan.app.R
import com.ubahkan.app.ui.theme.UbahkanDarkBackground

/**
 * Logo Ubahkan  -  "U Panah": huruf U geometris dengan mata panah
 * biru di ujung batang kanan. Varian gelap dipilih saat skema warna
 * aktif berlatar gelap, mengikuti pola deteksi tema BottomSheetConverter.
 */
@Composable
fun UbahkanLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val isDark = MaterialTheme.colorScheme.background == UbahkanDarkBackground
    Box(
        modifier = modifier.size(size)
    ) {
        Image(
            painter = painterResource(
                if (isDark) R.drawable.ubahkan_logo_dark else R.drawable.ubahkan_logo
            ),
            contentDescription = "Ubahkan",
            modifier = Modifier.fillMaxSize()
        )
    }
}
