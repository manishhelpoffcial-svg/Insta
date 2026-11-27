package com.example.data.model

data class ProfileData(
    val username: String = "themovietimez",
    val displayName: String = "Movie Timez | Cinema",
    val isVerified: Boolean = true,
    val avatarUrl: String? = null,
    val avatarDrawableKey: String = "themovietimez",
    val postsCount: String = "109",
    val followersCount: String = "316",
    val followingCount: String = "6",
    val bio: String = "🎬 Your daily dose of cinema.\n🍿 Movies • Iconic Scenes • Entertainment\nStay tuned for your next obsession.",
    val externalUrl: String = "",
    val privacyStatus: String = "",
    val dashboardSubtitle: String = "17.7T views in the last 30 days.",
    val noteText: String = "Unpopular opinion...",
)

data class ImpactMetric(
    val id: String,
    val label: String,
    val percentage: String,
    val status: String, // "Higher" or "Lower"
    val iconType: String, // "skip", "share", "like", "save", "repost", "comment"
)

data class SourceShare(
    val label: String,
    val percentage: Double,
)

data class DemographicItem(
    val label: String,
    val percentage: Double,
)

object ViewsCurvePresets {
    const val VIRAL_SPIKE = "Viral Launch Spike"
    const val STEADY_GROWTH = "Steady Growth"
    const val DELAYED_SPIKE = "Delayed Viral Spike"
    const val CUSTOM = "Custom Points"

    val allPresets = listOf(VIRAL_SPIKE, STEADY_GROWTH, DELAYED_SPIKE, CUSTOM)

    fun pointsForPreset(preset: String, series: String = "All"): List<Float> {
        val base = when (preset) {
            STEADY_GROWTH -> listOf(4f, 18f, 34f, 52f, 68f, 82f, 94f)
            DELAYED_SPIKE -> listOf(3f, 5f, 8f, 14f, 72f, 91f, 96f)
            else -> listOf(0f, 94f, 97f, 98f, 98.5f, 99f, 99.5f) // Reference Viral Launch Spike
        }
        return when (series) {
            "Followers" -> base.map { (it * 0.42f).coerceIn(0f, 100f) }
            "Non-followers" -> base.map { (it * 0.98f).coerceIn(0f, 100f) }
            else -> base
        }
    }
}

data class ReelInsights(
    val views: String = "15,915",
    val viewers: String = "10,619",
    val averageWatchTime: String = "37s",
    val follows: String = "10",
    val likes: String = "139",
    val comments: String = "0",
    val reposts: String = "1",
    val shares: String = "1",
    val saves: String = "7",
    val profileVisits: String = "13",
    val engagementFollows: String = "10",
    val impacts: List<ImpactMetric> = defaultImpacts(),
    val retentionUnavailable: Boolean = false,
    val retentionCurve: List<Float> = listOf(100f, 84f, 79f, 75f, 71f, 67f, 63f, 60f, 58f, 57f, 56f, 55f, 50f),
    val topSources: List<SourceShare> = defaultTopSources(),
    val likeCurvePoints: List<Float> = listOf(5f, 22f, 12f, 2f, 1f, 0.5f, 1.5f, 0.5f, 2f, 0.5f, 0.5f, 10f),
    val durationLabel: String = "0:58",
    val followersViewPercent: Double = 0.3,
    val nonFollowersViewPercent: Double = 99.7,
    val ageBreakdown: List<DemographicItem> = defaultAgeBreakdown(),
    val countryBreakdown: List<DemographicItem> = defaultCountryBreakdown(),
    val genderBreakdown: List<DemographicItem> = defaultGenderBreakdown(),
    // Views over time chart fields (10963.jpg)
    val viewsCurvePreset: String = ViewsCurvePresets.VIRAL_SPIKE,
    val viewsStartDate: String = "26 Sept",
    val viewsMidDate: String = "6 Oct",
    val viewsEndDate: String = "17 Oct",
    val viewsYAxisMax: String = "",
    val viewsOverTimeAll: List<Float> = ViewsCurvePresets.pointsForPreset(ViewsCurvePresets.VIRAL_SPIKE, "All"),
    val viewsOverTimeFollowers: List<Float> = ViewsCurvePresets.pointsForPreset(ViewsCurvePresets.VIRAL_SPIKE, "Followers"),
    val viewsOverTimeNonFollowers: List<Float> = ViewsCurvePresets.pointsForPreset(ViewsCurvePresets.VIRAL_SPIKE, "Non-followers"),
)

fun defaultImpacts(): List<ImpactMetric> = listOf(
    ImpactMetric("skip", "Skip rate", "15.7%", "Lower", "skip"),
    ImpactMetric("share", "Share rate", "0.2%", "Lower", "share"),
    ImpactMetric("like", "Like rate", "4.2%", "Higher", "like"),
    ImpactMetric("save", "Save rate", "0.3%", "Lower", "save"),
    ImpactMetric("repost", "Repost rate", "0.0%", "Lower", "repost"),
    ImpactMetric("comment", "Comment rate", "0.0%", "Typical", "comment"),
)

fun defaultTopSources(): List<SourceShare> = listOf(
    SourceShare("Reels tab", 84.1),
    SourceShare("Explore", 15.3),
    SourceShare("Feed", 0.5),
    SourceShare("Profile", 0.1),
)

fun defaultAgeBreakdown(): List<DemographicItem> = listOf(
    DemographicItem("13-17", 0.0),
    DemographicItem("18-24", 5.1),
    DemographicItem("25-34", 24.2),
    DemographicItem("35-44", 26.8),
    DemographicItem("45-54", 18.9),
    DemographicItem("55-64", 14.4),
    DemographicItem("65+", 10.7),
)

fun defaultCountryBreakdown(): List<DemographicItem> = listOf(
    DemographicItem("United States", 23.6),
    DemographicItem("Germany", 8.7),
    DemographicItem("India", 7.1),
    DemographicItem("United Kingdom", 6.0),
    DemographicItem("Indonesia", 5.0),
)

fun defaultGenderBreakdown(): List<DemographicItem> = listOf(
    DemographicItem("Men", 10.4),
    DemographicItem("Women", 89.6),
)

data class AgeRangeGenderShare(
    val label: String,
    val totalPct: Double,
    val womenShareRatio: Double = 0.74,
)

data class AccountInsightsData(
    val dateRange: String = "16 Oct-14 Nov",
    val timeframeLabel: String = "30 days",
    val dashboardViews: String = "17.7T",
    val newFollowers: String = "6",
    val contentShared: String = "2",
    // Overview Tab
    val overviewViews: String = "17,666",
    val netFollowers: String = "-3",
    val interactions: String = "123",
    val followersPct: Double = 0.5,
    val nonFollowersPct: Double = 99.5,
    val viewersTotal: String = "12,418",
    val reelsViews: String = "17.6K",
    val postsViews: String = "0",
    val storiesViews: String = "0",
    val liveViews: String = "0",
    val interactionsReels: String = "123",
    val interactionsLikes: String = "98",
    val interactionsComments: String = "12",
    val interactionsReposts: String = "13",
    val profileVisits: String = "10",
    val bioLinkTaps: String = "0",
    val businessAddressTaps: String = "0",
    val overviewChartStartDate: String = "16 Oct",
    val overviewChartMidDate: String = "31 Oct",
    val overviewChartEndDate: String = "14 Nov",
    val overviewViewsYAxisMax: String = "5800",
    val overviewViewsCurve: List<Float> = listOf(2f, 1f, 2f, 2f, 5f, 99f, 38f, 18f, 13f, 12f, 9f, 5f, 3f, 2f, 2f),
    val overviewFollowersCurve: List<Float> = listOf(45f, 52f, 40f, 65f, 50f, 90f, 88f, 52f, 48f, 25f, 26f, 45f, 32f, 48f, 30f),
    val overviewInteractionsCurve: List<Float> = listOf(4f, 3f, 5f, 6f, 12f, 94f, 42f, 22f, 18f, 14f, 10f, 7f, 6f, 4f, 5f),
    // Audience Tab
    val audienceFollowersTotal: String = "316",
    val audienceChangeSubtitle: String = "-0.9% since 17 Oct",
    val followerGrowthOverall: List<Float> = listOf(-1f, 0f, -1f, 0f, 1f, 0f, -1f, 0f, 0f, 2f, 2f, 0f, 0f, 0f, -1f, -1f, 0f, 0f, -1f, 0f, 0f, -1f),
    val followerGrowthFollows: List<Float> = listOf(0f, 1f, 0f, 1f, 2f, 1f, 0f, 1f, 1f, 2f, 2f, 1f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 1f),
    val followerGrowthUnfollows: List<Float> = listOf(-1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, 0f, 0f, -1f, -1f, -1f, -1f, -2f, -1f, 0f, -2f, -1f, 0f, -2f),
    val genderWomenPct: Double = 75.4,
    val genderMenPct: Double = 24.6,
    val ageRanges: List<AgeRangeGenderShare> = defaultAccountAgeRanges(),
    val topCountries: List<DemographicItem> = defaultAccountCountries(),
    val topCities: List<DemographicItem> = defaultAccountCities(),
    val activeTimesByDay: Map<String, List<Float>> = defaultActiveTimesByDay(),
)

fun defaultAccountAgeRanges(): List<AgeRangeGenderShare> = listOf(
    AgeRangeGenderShare("13-17", 0.4, 0.70),
    AgeRangeGenderShare("18-24", 17.0, 0.72),
    AgeRangeGenderShare("25-34", 30.7, 0.70),
    AgeRangeGenderShare("35-44", 19.7, 0.76),
    AgeRangeGenderShare("45-54", 7.6, 0.78),
    AgeRangeGenderShare("55-64", 14.0, 0.79),
    AgeRangeGenderShare("65+", 10.6, 0.82),
)

fun defaultAccountCountries(): List<DemographicItem> = listOf(
    DemographicItem("United States", 33.9),
    DemographicItem("United Kingdom", 8.2),
    DemographicItem("India", 5.7),
    DemographicItem("Germany", 4.4),
    DemographicItem("Canada", 3.8),
)

fun defaultAccountCities(): List<DemographicItem> = listOf(
    DemographicItem("Los Angeles", 1.3),
    DemographicItem("New York", 1.3),
    DemographicItem("Suva", 0.9),
    DemographicItem("Chennai", 0.9),
    DemographicItem("Labasa, Northern", 0.9),
)

fun defaultActiveTimesByDay(): Map<String, List<Float>> = mapOf(
    "Su" to listOf(0.98f, 0.80f, 0.67f, 0.61f, 0.61f, 0.70f, 0.83f, 0.92f),
    "M" to listOf(1.00f, 0.82f, 0.64f, 0.56f, 0.58f, 0.66f, 0.79f, 0.88f),
    "Tu" to listOf(0.88f, 0.72f, 0.59f, 0.54f, 0.60f, 0.71f, 0.84f, 0.86f),
    "W" to listOf(0.95f, 0.79f, 0.62f, 0.57f, 0.59f, 0.69f, 0.81f, 0.89f),
    "Th" to listOf(0.86f, 0.70f, 0.61f, 0.58f, 0.63f, 0.74f, 0.82f, 0.87f),
    "F" to listOf(0.84f, 0.73f, 0.60f, 0.55f, 0.62f, 0.75f, 0.86f, 0.91f),
    "Sa" to listOf(0.91f, 0.77f, 0.65f, 0.60f, 0.64f, 0.72f, 0.85f, 0.93f),
)

data class CommentItem(
    val id: String,
    val username: String,
    val text: String,
    val timeAgo: String,
    val likes: Int = 0,
)

data class MediaPost(
    val id: String,
    val shortCode: String = "",
    val postUrl: String = "",
    val authorUsername: String,
    val authorAvatarUrl: String? = null,
    val authorAvatarKey: String = "themovietimez",
    val subtitle: String = "Suggested for you",
    val isSuggested: Boolean = false,
    val isFollowing: Boolean = false,
    val mediaUrl: String? = null,
    val videoUrl: String? = null,
    val drawableKey: String = "beth",
    val overlayTopCaption: String = "",
    val overlaySubtitle: String = "",
    val durationText: String = "0:58",
    val caption: String = "",
    val dateLabel: String = "15 September",
    val likedByText: String = "",
    val viewsDisplay: String = "15.9K",
    val likesCount: Int = 139,
    val commentsCount: Int = 0,
    val repostsCount: Int = 1,
    val sharesCount: Int = 1,
    val savesCount: Int = 7,
    val likesUnavailable: Boolean = false,
    val commentsUnavailable: Boolean = false,
    val repostsUnavailable: Boolean = false,
    val sharesUnavailable: Boolean = false,
    val savesUnavailable: Boolean = false,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isReposted: Boolean = false,
    val isReel: Boolean = true,
    val isOwnPost: Boolean = true,
    val showInFeed: Boolean = false,
    val showInExplore: Boolean = false,
    val commentsList: List<CommentItem> = emptyList(),
    val insights: ReelInsights = ReelInsights(),
)

data class StoryItem(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
    val avatarKey: String = "themovietimez",
    val isOwnStory: Boolean = false,
    val hasActiveRing: Boolean = true,
    val showAddFriendBadge: Boolean = false,
    val storyImageKey: String = "feed_cinema",
    val storyCaption: String = "",
    val timeAgo: String = "2h",
)

data class ChatMessage(
    val id: String,
    val senderIsMe: Boolean,
    val text: String,
    val timestamp: String,
)

data class DmThread(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val avatarKey: String = "placeholder",
    val isVerifiedBlue: Boolean = false,
    val isMetaAi: Boolean = false,
    val isOnline: Boolean = false,
    val lastMessagePreview: String,
    val category: String = "Primary", // "Primary", "Requests", "General"
    val messages: List<ChatMessage> = emptyList(),
)

data class AppDataState(
    val profile: ProfileData = ProfileData(),
    val posts: List<MediaPost> = emptyList(),
    val stories: List<StoryItem> = emptyList(),
    val dmThreads: List<DmThread> = emptyList(),
    val lastFetchedUsername: String? = null,
    val accountInsights: AccountInsightsData = AccountInsightsData(),
)
