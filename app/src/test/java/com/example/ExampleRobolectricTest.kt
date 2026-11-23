package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.InstagramStorage
import com.example.data.network.ApifyRepository
import com.example.ui.components.FetchInstagramAccountModal
import com.example.ui.components.PROFILE_LOADING_MESSAGES
import com.example.ui.screens.AccountFetcherScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FetchAccountUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Instagram", appName)
    }

    @Test
    fun `storage persists edited profile and insights`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = InstagramStorage(context)
        val initial = storage.loadState()
        val updated = initial.copy(
            profile = initial.profile.copy(username = "updated_cinema", followersCount = "999K")
        )
        storage.saveState(updated)

        val reloaded = InstagramStorage(context).loadState()
        assertEquals("updated_cinema", reloaded.profile.username)
        assertEquals("999K", reloaded.profile.followersCount)
        assertTrue(reloaded.posts.isNotEmpty())
    }

    @Test
    fun `apifyParser parses profile and latestPosts`() {
        val sampleJson = """
            [
              {
                "username": "cinema_club",
                "fullName": "Cinema Club",
                "biography": "Daily film frames",
                "postsCount": 42,
                "followersCount": 12500,
                "followsCount": 15,
                "verified": true,
                "profilePicUrlHD": "https://example.com/avatar.jpg",
                "latestPosts": [
                  {
                    "id": "101",
                    "caption": "Iconic scene",
                    "displayUrl": "https://example.com/thumb.jpg",
                    "videoUrl": "https://example.com/video.mp4",
                    "videoViewCount": 15915,
                    "likesCount": 139,
                    "commentsCount": 4,
                    "type": "Video",
                    "timestamp": "2026-10-01T12:00:00.000Z"
                  }
                ]
              }
            ]
        """.trimIndent()

        val repo = ApifyRepository()
        val result = repo.parseApifyResponseJson(sampleJson)
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals("cinema_club", data.profile.username)
        assertEquals("12.5K", data.profile.followersCount)
        assertEquals(1, data.posts.size)
        assertEquals("15.9K", data.posts.first().viewsDisplay)
        assertEquals("15,915", data.posts.first().insights.views)
    }

    @Test
    fun `openPostDetail preserves grid post ids for scrollable Posts feed`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.InstagramViewModel(app)
        val gridIds = listOf("post_own_1", "post_own_3", "post_own_5")
        vm.openPostDetail("post_own_3", gridIds)

        val route = vm.backStack.value.lastOrNull() as? com.example.ui.viewmodel.OverlayRoute.PostDetail
        assertEquals("post_own_3", route?.postId)
        assertEquals(gridIds, route?.feedPostIds)
    }

    @Test
    fun `demoAccounts stories and bundled raw videos are all wired in initial state`() {
        val state = com.example.data.DefaultSampleData.createInitialState()
        assertEquals(7, com.example.data.DefaultSampleData.demoAccounts.size)
        assertEquals(7, state.stories.size)
        assertTrue(com.example.data.DefaultSampleData.isVerifiedCreator("cineverxhub"))
        assertTrue(com.example.data.DefaultSampleData.isVerifiedCreator("themovietimez"))
        assertTrue(!com.example.data.DefaultSampleData.isVerifiedCreator("blox.ibakke"))

        val videoReels = state.posts.filter { it.videoUrl?.startsWith("raw://") == true }
        assertEquals(5, videoReels.size)
        assertTrue(state.posts.any { it.id == "photo_aarav_rides" && !it.isReel })
        assertTrue(state.posts.any { it.id == "photo_nightvibe_rbx" && !it.isReel })
    }

    @Test
    fun `likePostIfUnliked likes once without unliking and openCreatorProfile routes cleanly`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.InstagramViewModel(app)

        val beforeLikes = vm.appState.value.posts.first { it.id == "reel_1_cineverx" }.likesCount
        vm.likePostIfUnliked("reel_1_cineverx")
        vm.likePostIfUnliked("reel_1_cineverx")
        val after = vm.appState.value.posts.first { it.id == "reel_1_cineverx" }
        assertTrue(after.isLiked)
        assertEquals(beforeLikes + 1, after.likesCount)

        vm.openCreatorProfile("cineverxhub")
        val creatorRoute = vm.backStack.value.lastOrNull() as? com.example.ui.viewmodel.OverlayRoute.CreatorProfile
        assertEquals("cineverxhub", creatorRoute?.username)
    }

    @Test
    fun `viewsCurvePresets and formatAxisLabels generate expected curves and ticks`() {
        val viralAll = com.example.data.model.ViewsCurvePresets.pointsForPreset(
            com.example.data.model.ViewsCurvePresets.VIRAL_SPIKE,
            "All",
        )
        assertEquals(7, viralAll.size)
        assertTrue(viralAll[1] > 85f)

        val steadyFollowers = com.example.data.model.ViewsCurvePresets.pointsForPreset(
            com.example.data.model.ViewsCurvePresets.STEADY_GROWTH,
            "Followers",
        )
        assertEquals(7, steadyFollowers.size)
        assertTrue(steadyFollowers.last() > steadyFollowers.first())

        val (max4k, mid2k) = com.example.ui.components.formatAxisLabels("4000", "15,915")
        assertEquals("4K", max4k)
        assertEquals("2K", mid2k)

        val (max58, mid29) = com.example.ui.components.formatAxisLabels("5800", "17,666")
        assertEquals("5.8K", max58)
        assertEquals("2.9K", mid29)
    }

    @Test
    fun `dashboard and accountInsights routes update state and persist across storage reloads`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.InstagramViewModel(app)

        vm.openProfessionalDashboard()
        assertTrue(vm.backStack.value.lastOrNull() is com.example.ui.viewmodel.OverlayRoute.ProfessionalDashboard)

        vm.openAccountInsights(2)
        val acctRoute = vm.backStack.value.lastOrNull() as? com.example.ui.viewmodel.OverlayRoute.AccountInsights
        assertEquals(2, acctRoute?.initialTab)

        val nextAccountInsights = vm.appState.value.accountInsights.copy(
            dashboardViews = "25.4K",
            overviewViews = "25,400",
            audienceFollowersTotal = "420",
        )
        vm.updateAccountInsights(nextAccountInsights)

        assertEquals("25.4K views in the last 30 days.", vm.appState.value.profile.dashboardSubtitle)
        assertEquals("420", vm.appState.value.profile.followersCount)

        val reloaded = InstagramStorage(app).loadState()
        assertEquals("25,400", reloaded.accountInsights.overviewViews)
        assertEquals("420", reloaded.accountInsights.audienceFollowersTotal)
        assertEquals(7, reloaded.accountInsights.ageRanges.size)
    }

    @Test
    fun `formatRelativeDate and resolveLikerUsername format natural Instagram social proof`() {
        val now = java.time.Instant.parse("2026-11-16T12:00:00Z").toEpochMilli()
        assertEquals("3 days ago", ApifyRepository.formatRelativeDate("2026-11-13", now))
        assertEquals("5 hours ago", ApifyRepository.formatRelativeDate("2026-11-16T07:00:00Z", now))
        assertEquals("2 weeks ago", ApifyRepository.formatRelativeDate("2026-11-01", now))
        assertEquals("15 September", ApifyRepository.formatRelativeDate("15 September", now))

        val scrapedPost = com.example.data.model.MediaPost(
            id = "p_test",
            authorUsername = "watchhhtthis.co",
            likedByText = "Liked by 1 people",
            likesCount = 1,
        )
        assertEquals(38, com.example.ui.screens.LIKER_USERNAMES_POOL.size)
        val firstTen = (0 until 10).map { idx ->
            com.example.ui.screens.resolveLikerUsername(scrapedPost, idx, "watchhhtthis.co")
        }
        assertEquals("catchthis.us", firstTen.first())
        assertEquals(10, firstTen.distinct().size)
        for (i in 1 until firstTen.size) {
            assertTrue(firstTen[i] != firstTen[i - 1])
        }

        val commentedPost = com.example.data.model.MediaPost(
            id = "p_test_2",
            authorUsername = "themovietimez",
            likedByText = "Liked by custom.creator and others",
            likesCount = 139,
        )
        assertEquals("custom.creator", com.example.ui.screens.resolveLikerUsername(commentedPost, 3))

        assertEquals(
            "watchhhtthis.co · Original audio",
            com.example.ui.screens.resolveAudioSubtitle("Original audio", "watchhhtthis.co"),
        )
        assertEquals(
            "watchhhtthis.co · Original audio",
            com.example.ui.screens.resolveAudioSubtitle("old_handle · Original audio", "watchhhtthis.co"),
        )
        assertEquals(
            "LANY · Stuck",
            com.example.ui.screens.resolveAudioSubtitle("LANY · Stuck", "watchhhtthis.co"),
        )
    }

    @Test
    fun `updatePostDetails updates audio and liker override and persists`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.InstagramViewModel(app)
        val targetId = vm.appState.value.posts.first { it.isOwnPost }.id

        vm.updatePostDetails(targetId, "Tame Impala · Let It Happen", "rohan.vibes_")
        val updated = vm.appState.value.posts.first { it.id == targetId }
        assertEquals("Tame Impala · Let It Happen", updated.subtitle)
        assertEquals("Liked by rohan.vibes_", updated.likedByText)

        val reloaded = InstagramStorage(app).loadState().posts.first { it.id == targetId }
        assertEquals("Tame Impala · Let It Happen", reloaded.subtitle)
        assertEquals("Liked by rohan.vibes_", reloaded.likedByText)
    }

    @Test
    fun `apifyParser extracts external musicInfo and defaults original audio with fetched username`() {
        val sampleJson = """
            [
              {
                "username": "watchhhtthis.co",
                "fullName": "Watch This",
                "postsCount": 2,
                "followersCount": 1400,
                "followsCount": 20,
                "latestPosts": [
                  {
                    "id": "201",
                    "caption": "Concert night",
                    "displayUrl": "https://example.com/1.jpg",
                    "videoUrl": "https://example.com/1.mp4",
                    "likesCount": 5,
                    "musicInfo": {
                      "artist_name": "LANY",
                      "song_name": "Stuck"
                    }
                  },
                  {
                    "id": "202",
                    "caption": "Behind the scenes",
                    "displayUrl": "https://example.com/2.jpg",
                    "videoUrl": "https://example.com/2.mp4",
                    "likesCount": 3
                  }
                ]
              }
            ]
        """.trimIndent()

        val result = ApifyRepository().parseApifyResponseJson(sampleJson)
        assertTrue(result.isSuccess)
        val posts = result.getOrThrow().posts
        assertEquals("LANY · Stuck", posts[0].subtitle)
        assertEquals("N/A", posts[1].subtitle)
        assertEquals("", posts[0].likedByText)
    }

    @Test
    fun `formatInsightsIconCount formats 1000 and above in compact K and M notation`() {
        assertEquals("0", ApifyRepository.formatInsightsIconCount("0"))
        assertEquals("142", ApifyRepository.formatInsightsIconCount("142"))
        assertEquals("999", ApifyRepository.formatInsightsIconCount("999"))
        assertEquals("1K", ApifyRepository.formatInsightsIconCount("1000"))
        assertEquals("1K", ApifyRepository.formatInsightsIconCount("1,000"))
        assertEquals("1.1K", ApifyRepository.formatInsightsIconCount("1100"))
        assertEquals("1.1K", ApifyRepository.formatInsightsIconCount("1,100"))
        assertEquals("6.4K", ApifyRepository.formatInsightsIconCount("6,420"))
        assertEquals("1M", ApifyRepository.formatInsightsIconCount("1,000,000"))
        assertEquals("1.2M", ApifyRepository.formatInsightsIconCount("1,250,000"))
        assertEquals("6.4K", ApifyRepository.formatInsightsIconCount("6.4K"))
        assertEquals("N/A", ApifyRepository.formatInsightsIconCount("N/A"))
    }

    @Test
    fun `instagramScraper parses backend items, externalUrl, private status, dedupes shortCodes, and sets unavailable to NA`() {
        val repo = ApifyRepository()

        val scraperJson = """
            {
              "username": "humansofny",
              "items": [
                {
                  "type": "Profile",
                  "username": "humansofny",
                  "fullName": "Humans of New York",
                  "biography": "New York City stories",
                  "externalUrl": "https://www.humansofnewyork.com/",
                  "followersCount": 12400000,
                  "followsCount": 12,
                  "postsCount": 4200,
                  "verified": true,
                  "isPrivate": false,
                  "profilePicUrlHD": "https://example.com/hony_avatar.jpg",
                  "latestPosts": [
                    {
                      "id": "3456789012",
                      "shortCode": "DLNsnpUTdVS",
                      "url": "https://www.instagram.com/reel/DLNsnpUTdVS/",
                      "type": "Video",
                      "productType": "clips",
                      "caption": "Street portrait reel",
                      "displayUrl": "https://example.com/hony1.jpg",
                      "videoUrl": "https://example.com/hony1.mp4",
                      "videoViewCount": 674072,
                      "videoDuration": 45.2,
                      "likesCount": 6420,
                      "commentsCount": 14,
                      "sharesCount": 310,
                      "savesCount": 820,
                      "repostsCount": 44,
                      "timestamp": "2026-11-16T08:00:00.000Z"
                    }
                  ]
                },
                {
                  "id": "dup_1",
                  "shortCode": "DLNsnpUTdVS",
                  "type": "Video",
                  "displayUrl": "https://example.com/hony1.jpg"
                },
                {
                  "id": "3456789013",
                  "shortCode": "DLNsnpUTdVT",
                  "type": "Image",
                  "caption": "",
                  "displayUrl": "https://example.com/photo.jpg",
                  "likesCount": -1,
                  "commentsCount": -1
                }
              ]
            }
        """.trimIndent()

        val parsed = repo.parseApifyResponseJson(
            rawJson = scraperJson,
            fallbackUsername = "humansofny",
            isSinglePostTarget = false,
        )
        assertTrue(parsed.isSuccess)
        val data = parsed.getOrThrow()
        assertEquals("humansofny", data.profile.username)
        assertEquals("Humans of New York", data.profile.displayName)
        assertEquals("New York City stories", data.profile.bio)
        assertEquals("https://www.humansofnewyork.com/", data.profile.externalUrl)
        assertEquals("12.4M", data.profile.followersCount)
        assertEquals("12", data.profile.followingCount)
        assertEquals("4,200", data.profile.postsCount)
        assertEquals("Public", data.profile.privacyStatus)
        assertTrue(data.profile.isVerified)

        // Duplicate shortCode DLNsnpUTdVS is deduplicated
        assertEquals(2, data.posts.size)
        val reel = data.posts[0]
        assertEquals("DLNsnpUTdVS", reel.shortCode)
        assertEquals("674,072", reel.insights.views)
        assertEquals("6,420", reel.insights.likes)
        assertEquals("310", reel.insights.shares)
        assertEquals("820", reel.insights.saves)
        assertEquals("44", reel.insights.reposts)
        assertEquals("0:45", reel.durationText)
        assertEquals("N/A", reel.insights.viewers)
        assertEquals("N/A", reel.insights.averageWatchTime)
        assertTrue(reel.insights.impacts.isEmpty())

        val unavailablePost = data.posts[1]
        assertEquals("N/A", unavailablePost.caption)
        assertEquals("N/A", unavailablePost.insights.likes)
        assertEquals("N/A", unavailablePost.insights.comments)
        assertEquals("N/A", unavailablePost.insights.views)
        assertEquals("N/A", unavailablePost.viewsDisplay)
        assertEquals("N/A", unavailablePost.dateLabel)
        assertTrue(unavailablePost.likesUnavailable)
        assertEquals(0, unavailablePost.likesCount)

        val emptyResult = repo.parseApifyResponseJson("[]", "natgeo")
        assertTrue(emptyResult.isFailure)
        assertEquals("natgeo", ApifyRepository.extractUsernameFromTarget("@natgeo"))
        assertEquals("natgeo", ApifyRepository.extractUsernameFromTarget("https://www.instagram.com/natgeo/"))
    }

    @Test
    fun `viewModel validates empty username on Tab 3 and deduplicates fetched posts`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.InstagramViewModel(app)

        vm.fetchAccountFromApify("   @  ")
        val errState = vm.fetchUiState.value as? com.example.ui.viewmodel.FetchAccountUiState.Error
        assertTrue(errState != null)

        val dupPost = com.example.data.model.MediaPost(
            id = "p1",
            shortCode = "ABC123",
            authorUsername = "humansofny",
            isOwnPost = true,
        )
        val dupPost2 = dupPost.copy(id = "p2", shortCode = "ABC123")
        vm.applyFetchedAccount(
            profile = com.example.data.model.ProfileData(
                username = "humansofny",
                displayName = "Humans of New York",
                externalUrl = "N/A",
                privacyStatus = "Public",
            ),
            fetchedPosts = listOf(dupPost, dupPost2),
        )
        assertEquals(com.example.ui.viewmodel.MainTab.PROFILE, vm.activeTab.value)
        assertEquals("humansofny", vm.appState.value.profile.username)
        assertEquals(1, vm.appState.value.posts.count { it.shortCode == "ABC123" })
    }

    @Test
    fun `tab3 AccountFetcherScreen shows centered input and Enter button and strips leading @`() {
        var submittedUsername = ""
        composeRule.setContent {
            MyApplicationTheme {
                AccountFetcherScreen(
                    fetchState = FetchAccountUiState.Idle,
                    onFetchAccount = { submittedUsername = it },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tab3_username_input").assertIsDisplayed()
        composeRule.onNodeWithText("Enter Instagram username...").assertIsDisplayed()
        composeRule.onNodeWithTag("tab3_fetch_button").assertIsDisplayed()
        composeRule.onNodeWithText("Enter").assertIsDisplayed()

        // Blank submit shows error and does not invoke callback
        composeRule.onNodeWithTag("tab3_fetch_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tab3_error_text").assertIsDisplayed()
        assertEquals("", submittedUsername)

        // Username with @ strips @ cleanly
        composeRule.onNodeWithTag("tab3_username_input").performTextInput("@natgeo")
        composeRule.onNodeWithTag("tab3_fetch_button").performClick()
        composeRule.waitForIdle()
        assertEquals("natgeo", submittedUsername)
    }

    @Test
    fun `profileScraper payload matches apify instagram-profile-scraper and triple-tap small box uses Enter and progressive loading copy`() {
        assertEquals("apify/instagram-profile-scraper", ApifyRepository.APIFY_ACTOR_ID)
        assertEquals(
            """{"usernames":["humansofny"]}""",
            ApifyRepository.buildProfileScraperPayload("humansofny"),
        )
        assertTrue(PROFILE_LOADING_MESSAGES.contains("Loading..."))
        assertTrue(PROFILE_LOADING_MESSAGES.contains("Collecting posts..."))
        assertTrue(PROFILE_LOADING_MESSAGES.contains("Almost done..."))
        assertTrue(PROFILE_LOADING_MESSAGES.none { it.contains("fetch", ignoreCase = true) })

        var submitted = ""
        composeRule.setContent {
            MyApplicationTheme {
                FetchInstagramAccountModal(
                    fetchState = FetchAccountUiState.Idle,
                    onDismiss = {},
                    onSubmitUsername = { submitted = it },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("triple_tap_username_box").assertIsDisplayed()
        composeRule.onNodeWithTag("fetch_username_input").assertIsDisplayed()
        composeRule.onNodeWithText("Enter").assertIsDisplayed()

        composeRule.onNodeWithTag("fetch_username_input").performTextInput("@humansofny")
        composeRule.onNodeWithTag("fetch_submit_button").performClick()
        composeRule.waitForIdle()
        assertEquals("humansofny", submitted)
    }
}
