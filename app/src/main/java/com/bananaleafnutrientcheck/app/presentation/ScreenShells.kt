package com.bananaleafnutrientcheck.app.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bananaleafnutrientcheck.app.R

private val CautionContainer = Color(0xFFFFF8E1)
private val CautionContent = Color(0xFF8A5000)
private val CardShape = RoundedCornerShape(8.dp)

@Composable
fun HomeScreen(
    onStartScan: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContent(modifier = modifier) {
        ScreenHeading(text = stringResource(R.string.home_heading))
        BodyText(text = stringResource(R.string.home_body))

        CautionCard(
            title = stringResource(R.string.home_caution_title),
            body = stringResource(R.string.home_caution_body),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onStartScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = CardShape,
            ) {
                Text(text = stringResource(R.string.home_scan_action))
            }
            OutlinedButton(
                onClick = onOpenAbout,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = CardShape,
            ) {
                Text(text = stringResource(R.string.home_about_action))
            }
        }
    }
}

@Composable
fun ScanScreen(modifier: Modifier = Modifier) {
    ScreenContent(modifier = modifier) {
        ScreenHeading(text = stringResource(R.string.scan_heading))
        BodyText(text = stringResource(R.string.scan_intro))

        InfoCard(
            title = stringResource(R.string.scan_guidance_title),
            body = stringResource(R.string.scan_guidance_body),
        )

        PlaceholderCard(
            title = stringResource(R.string.scan_take_photo_title),
            body = stringResource(R.string.scan_take_photo_body),
        )
        PlaceholderCard(
            title = stringResource(R.string.scan_choose_image_title),
            body = stringResource(R.string.scan_choose_image_body),
        )

        CautionCard(
            title = stringResource(R.string.scan_not_active_title),
            body = stringResource(R.string.scan_not_active_body),
        )
    }
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    ScreenContent(modifier = modifier) {
        ScreenHeading(text = stringResource(R.string.about_heading))
        BodyText(text = stringResource(R.string.about_intro))

        InfoCard(title = stringResource(R.string.about_limits_title)) {
            BulletText(text = stringResource(R.string.about_limit_uncertainty))
            BulletText(text = stringResource(R.string.about_limit_overlap))
            BulletText(text = stringResource(R.string.about_limit_testing))
            BulletText(text = stringResource(R.string.about_limit_no_rates))
        }

        InfoCard(
            title = stringResource(R.string.about_classes_title),
            body = stringResource(R.string.about_classes_body),
        )

        CautionCard(
            title = stringResource(R.string.about_dataset_title),
            body = stringResource(R.string.about_dataset_body),
        )
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun ScreenHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    InfoCard(
        title = title,
        modifier = modifier,
    ) {
        BodyText(text = body)
    }
}

@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitle(text = title)
            content()
        }
    }
}

@Composable
private fun PlaceholderCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitle(text = title)
            BodyText(text = body)
        }
    }
}

@Composable
private fun CautionCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = CautionContainer,
            contentColor = CautionContent,
        ),
        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitle(text = title, color = CautionContent)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = CautionContent,
            )
        }
    }
}

@Composable
private fun CardTitle(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
private fun BulletText(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
