package com.moviesnight.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.moviesnight.tv.R
import com.moviesnight.tv.ui.theme.Geist
import com.moviesnight.tv.ui.theme.TextPrimary
import androidx.compose.foundation.layout.padding
import com.moviesnight.tv.ui.preview.PreviewSurface
import com.moviesnight.tv.ui.preview.TvComponentPreview

/**
 * The website's header logo: popcorn icon + "movies" (Geist extra-light) "Night" (regular).
 */
@Composable
fun Logo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraLight)) { append("movies") }
                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append("Night") }
            },
            style = TextStyle(fontFamily = Geist, fontSize = 20.sp, letterSpacing = (-0.2).sp),
            color = TextPrimary,
        )
    }
}

@TvComponentPreview
@Composable
private fun LogoPreview() {
    PreviewSurface { Logo(Modifier.padding(24.dp)) }
}
