package dev.olog.feature.search.widget

import androidx.annotation.StringRes
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import dev.olog.compose.text.Header

@Composable
fun SearchHeader(
    @StringRes stringRes: Int,
    itemsCount: Int,
    modifier: Modifier = Modifier,
) {
    Header(
        text = stringResource(stringRes),
        modifier = modifier,
        endContent = {
            Text(
                text = pluralStringResource(
                    localization.R.plurals.search_xx_results,
                    itemsCount,
                    itemsCount,
                ),
                color = MaterialTheme.colors.secondary,
                fontSize = 14.sp
            )
        }
    )
}