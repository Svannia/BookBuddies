package com.appbuddies.bookbuddies.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.appbuddies.bookbuddies.R
import com.appbuddies.bookbuddies.navigation.NavigationActions
import com.appbuddies.bookbuddies.ui.SecondaryScreen
import com.appbuddies.bookbuddies.ui.theme.MyTypography

val SOCIALS = listOf(
    Social("Discord", "DiscordBuddies", "https://discord.gg/CvmW8R3Qwt"),
    Social("Instagram", "@arts_alice", "https://www.instagram.com/arts__alice"),
    Social("Tiktok", "@arts_alice", "https://www.tiktok.com/@arts_alice"),
)
@Composable
fun BehindTheScenes(navigationActions: NavigationActions) {
    SecondaryScreen(
        title = stringResource(R.string.dst_bts),
        navigationActions = navigationActions,
        navExtraActions = {},
        topBarIcons = {}
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // rounded header shape with sticker picture
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(ArcShape())
                            .background(color = MaterialTheme.colorScheme.primary),
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.size(60.dp))
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.Black, CircleShape)
                                .background(color = MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            RoundImage(
                                148.dp,
                                painterResource(R.drawable.alice_buddies),
                                stringResource(R.string.desc_devPic)
                            )
                        }
                    }
                }
            }
            // title
            item {
                Text(text = stringResource(R.string.title_dev), style = MyTypography.titleSmall)
            }
            // info text
            item {
                Text(
                    text = stringResource(R.string.txt_dev),
                    style = MyTypography.bodyLarge.copy(textAlign = TextAlign.Center)
                )
            }
            // socials
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SOCIALS.forEach { SocialDisplay(it) }
                }
            }
            // donation
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.txt_donation),
                        style = MyTypography.bodyMedium
                    )
                    val annotatedString = buildAnnotatedString {
                        pushLink(LinkAnnotation.Url("https://ko-fi.com/alicebuddies"))
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Ko-Fi")
                        }
                        pop()
                    }
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = annotatedString,
                        style = MyTypography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center),
                    )
                    Text(
                        text = stringResource(R.string.txt_donationThanks),
                        style = MyTypography.bodyMedium
                    )
                }
            }
        }
    }
}

data class Social(val platform: String, val social: String, val hyperlink: String)

@Composable
/**
 * A row with the name of the social's platform, and the social's value as clickable hyperlink.
 *
 * @param social data object Social
 */
fun SocialDisplay(social: Social) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${social.platform}: ",
            style = MyTypography.bodyMedium.copy(fontStyle = FontStyle.Italic)
        )
        val annotatedString = buildAnnotatedString {
            pushLink(LinkAnnotation.Url(social.hyperlink))
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append(social.social)
            }
            pop()
        }
        Text(
            text = annotatedString,
            style = MyTypography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline),
        )
    }
}

/**
 * Crops an image into a disk (e.g for profile pictures).
 *
 * @param size diameter of the round image
 * @param picture Uri of the picture to be cropped
 * @param contentDescription image description
 */
@Composable
fun RoundImage(size: Dp, picture: Painter, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Transparent)
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = picture,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds
        )
    }
}

class ArcShape(private val arcHeight: Float = 100f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, size.height - arcHeight)
            quadraticTo(
                size.width / 2, size.height + arcHeight,
                size.width, size.height - arcHeight
            )
            lineTo(size.width, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
