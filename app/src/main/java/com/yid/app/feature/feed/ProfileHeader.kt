package com.yid.app.feature.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yid.app.core.model.Feed
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.Avatar
import com.yid.app.ui.component.GlassTarget
import com.yid.app.ui.component.VerificationCheck
import com.yid.app.ui.component.rememberMediaPolicy
import com.yid.app.ui.glass.LocalGlass
import com.yid.app.ui.glass.glassZone
import com.yid.app.ui.icon.YidIcons
import java.util.Locale

/**
 * A profile's header, compact as Bluesky's and LinkedOut's: the banner edge to edge from the window's top (the status
 * bar's shadow kept, a gradient of the phone's colours when the account has none), the avatar at the start overlapping
 * it in a ring of the page's colour, Follow at the end below the banner, then at the start the name with its verified
 * check, the handle, the counts on one line, the bio and Joined. The banner obeys the media policy (held: its ground
 * only). [LocalReadableInset] narrows the text part only. A tap on the banner or the avatar opens it at its original size.
 * Ports:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/feed/FeedScreen.kt#ProfileHeader
 * https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/ProfileHeaderStandard.tsx#ProfileHeaderStandard
 * not ported: LinkedOut's company kind, headline, currentCompany, location, school and website rows and AccountKind (LinkedIn's
 * fields: Bluesky's profile has none of them, its fields are the name, handle, counts, bio and Joined)
 * not ported: ProfileHeaderStandard's labeler, block, mute, subscribe and message buttons (a reader has no account)
 * not ported: onPressFollow (the follow runs through onToggleFollow)
 * not ported: onPressUnfollow (the existing Unfollow dialog in FeedScreen asks first)
 * not ported: onRequestHide (moderation: a reader reads no labels)
 * not ported: headerStandardButtons (FollowButton is its one button, a reader has no account for the others)
 * not ported: unblockAccount (a reader has no account)
 * not ported: pin (a reader has no account)
 * not ported: pinInitialHash (web page hash)
 * not ported: measureScrollMarginTop, watchScrollMarginTop (web scroll offset)
 * not ported: shouldUseProfileLightStatusBar (the status bar's icons are the app's, set in MainActivity)
 * not ported: profileBannerSafeAreaCoverHeight (web safe area, the inset is read from WindowInsets.statusBars)
 * not ported: sourceLine, "Read via host, updated ago" (the validated picture has no such line)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileHeader(
    handle: String,
    name: String?,
    feed: Feed?,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onOpenAvatar: (String) -> Unit,
    onOpenBanner: (String) -> Unit
) {
    val hold = rememberMediaPolicy().hold
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val readable = LocalReadableInset.current
    val banner = feed?.bannerUrl
    val avatar = feed?.avatarUrl
    Box(Modifier.fillMaxWidth()) {
        // Shell.tsx onPressBanner: only a banner that exists opens the lightbox (here MediaViewer). Without one, a
        // gradient of the phone's primary and tertiary stands in.
        Box(
            Modifier.fillMaxWidth().height(ProfileBannerHeight)
                .semantics { contentDescription = "Banner" }
                .clickable(enabled = banner != null, role = Role.Image) { banner?.let(onOpenBanner) }
                .background(
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                )
        ) {
            // The banner is a picture like any other, so Wi-Fi only holds it too.
            if (banner != null && !hold) {
                AsyncImage(
                    model = banner,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(ProfileBannerHeight)
                )
            }
        }
        // Bluesky's StatusBarShadow: rgba(0,0,0,0.5) to transparent, the height of the inset.
        Box(
            Modifier.fillMaxWidth().height(inset).background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))
            )
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                // GrowableAvatar: top 104, left 10, width 94, size 90: a ring of the page's colour around the avatar.
                Box(
                    Modifier.padding(top = AvatarTop, start = AvatarLeft + readable)
                        .size(AvatarSize + AvatarRing * 2)
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                        .clip(CircleShape)
                        .semantics { contentDescription = "Profile picture" }
                        // Shell.tsx onPressAvi: only an avatar that exists opens the lightbox.
                        .clickable(enabled = avatar != null, role = Role.Image) { avatar?.let(onOpenAvatar) },
                    contentAlignment = Alignment.Center
                ) {
                    Avatar(url = avatar, name = name ?: handle, size = AvatarSize)
                }
                // The buttons row: justify_end, px_lg 16, pt_md 12 below the banner.
                Row(
                    Modifier.align(Alignment.TopEnd)
                        .padding(top = ProfileBannerHeight + 8.dp, end = 16.dp + readable),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FollowButton(following = isFollowing, onClick = onToggleFollow)
                }
            }
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier.padding(start = 16.dp + readable, end = 16.dp + readable),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // DisplayName.tsx: the name wraps, capped here at 2 lines; the check follows outside the Text,
                    // so a long name ellipsizes without taking it along.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name ?: "@$handle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (feed?.verified == true) VerificationCheck(Modifier.padding(start = 6.dp), size = 22.dp)
                    }
                    // Handle.tsx: the handle in the contrast medium colour; without a name the first line is the handle.
                    if (name != null) {
                        Text(
                            "@$handle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                feed?.stats?.let { stats ->
                    ProfileHeaderMetrics(
                        followers = stats.followers?.let(::compactCount),
                        following = stats.following?.let(::compactCount),
                        posts = stats.posts?.let(::compactCount)
                    )
                }
                // RichText numberOfLines 15: the bio as plain text.
                feed?.bio?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 15, overflow = TextOverflow.Ellipsis) }
                feed?.joined?.let { ProfileItem(YidIcons.CalendarMonth, "Joined", it) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// An icon (16 dp here) then the text; one wrapping line.
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/feed/FeedScreen.kt#ProfileItem
// https://github.com/TwidereProject/TwidereX-Android/blob/develop/common/src/commonMain/kotlin/com/twidere/twiderex/component/UserComponent.kt#ProfileItem
@Composable
private fun ProfileItem(
    icon: ImageVector,
    description: String,
    text: String,
    textColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(4.dp))
        Text(
            text, style = MaterialTheme.typography.bodyMedium, color = textColor,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

// Metrics.tsx: followers, following and posts on one line, a flow so 200 % wraps. A count the profile did not give is left out.
// https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/Metrics.tsx#ProfileHeaderMetrics
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/feed/FeedScreen.kt#ProfileHeaderMetrics
// not ported: Metrics' links to the followers and the follows lists (the public profile gives their counts only), formatCount and plural (fixed words)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeaderMetrics(followers: String?, following: String?, posts: String?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        followers?.let { MetricsItem(it, "followers") }
        following?.let { MetricsItem(it, "following") }
        posts?.let { MetricsItem(it, "posts") }
    }
}

// The count bold, its word in the contrast medium colour.
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/feed/FeedScreen.kt#MetricsItem
@Composable
private fun MetricsItem(primaryText: String, secondaryText: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$primaryText ") }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(secondaryText) }
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

// layout.ts PROFILE_BANNER_HEIGHT, Shell.tsx GrowableAvatar (top 104, left 10, width 94, size 90)
private val ProfileBannerHeight: Dp = 150.dp
private val AvatarSize: Dp = 90.dp
private val AvatarRing: Dp = 2.dp
private val AvatarTop: Dp = 104.dp
private val AvatarLeft: Dp = 10.dp

// Follow as Material 3 Expressive's medium button with a leading icon, a clickable Row, no Material control: 56 dp high,
// its container a glass pane (BoldButton's own zone: GlassTarget, glassZone), the primary filling it for Follow and
// going out by alpha only for Following (a fade to Color.Transparent would pass through grey).
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/feed/FeedScreen.kt#FollowButton
// https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/samples/src/main/java/androidx/compose/animation/samples/AnimatedContentSamples.kt#SimpleAnimatedContentSample
// not ported: ContainerShapeSquare and the Selected shapes (no toggle here), disabled and elevation tokens.
// not ported: textStyleFor (its one case here, titleMedium from the medium height up, is the Text's style below)
// not ported: the outline (the glass pane draws its own rim)
// not ported: simpleAnimatedContentSample (the sample's function; its AnimatedContent is used here, inline)
// not ported: foo, bar, baz (the sample's own demo functions)
@Composable
private fun FollowButton(following: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val glass = LocalGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val density = LocalDensity.current
    val height = ButtonMediumContainerHeight
    // ContainerShapeRound (CornerFull) morphs to PressedContainerShape (CornerMedium = ShapeDefaults.Medium) with a spring.
    val full = with(density) { height.toPx() / 2 }
    val medium = ShapeDefaults.Medium.topStart.toPx(Size(full * 2, full * 2), density)
    val corner by animateFloatAsState(
        targetValue = if (pressed) medium else full,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "followCorner"
    )
    // The fill is read in the draw block: no recomposition per frame. Same hue, alpha only.
    val container = animateFloatAsState(if (following) 0f else 1f, tween(200), label = "followContainer")
    val contentColor by animateColorAsState(if (following) colors.primary else colors.onPrimary, tween(200), label = "followContent")
    GlassTarget(Modifier) {
        Row(
            Modifier
                .heightIn(min = height)
                .then(if (glass != null) Modifier.glassZone(CircleShape, glass, lens = 1f) else Modifier)
                .graphicsLayer {
                    this.shape = RoundedCornerShape(corner)
                    clip = true
                }
                .drawBehind {
                    val outline = RoundedCornerShape(corner).createOutline(size, layoutDirection, this)
                    drawOutline(outline, colors.primary, alpha = container.value)
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = contentColor),
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(horizontal = ButtonMediumLeadingSpace),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon and label fade through, the width follows without clipping (SizeTransform(clip = false)).
            AnimatedContent(
                targetState = following,
                transitionSpec = {
                    (fadeIn(tween(110, delayMillis = 90)) togetherWith fadeOut(tween(90)))
                        .using(SizeTransform(clip = false))
                },
                label = "followLabel"
            ) { isFollowing ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isFollowing) YidIcons.Check else YidIcons.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonMediumIconSize),
                        tint = contentColor
                    )
                    Spacer(Modifier.width(ButtonMediumIconLabelSpace))
                    // textStyleFor: titleMedium from the medium height up.
                    Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.titleMedium, color = contentColor)
                }
            }
        }
    }
}

// ButtonMediumTokens
private val ButtonMediumContainerHeight: Dp = 56.dp
private val ButtonMediumIconLabelSpace: Dp = 8.dp
private val ButtonMediumIconSize: Dp = 24.dp
private val ButtonMediumLeadingSpace: Dp = 24.dp

/** 870, 12,345, 566K, 53.2M: exact while short, rounded once it would not fit. */
private fun compactCount(value: Long): String = when {
    value < 10_000 -> String.format(Locale.getDefault(), "%,d", value)
    value < 1_000_000 -> "${value / 1_000}K"
    else -> {
        val millions = value / 100_000 / 10.0
        String.format(Locale.getDefault(), "%.1fM", millions).replace(".0M", "M").replace(",0M", "M")
    }
}
