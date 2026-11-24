package com.example

import com.example.data.DefaultSampleData
import com.example.data.network.AuthSession
import com.example.data.network.SubscriptionAccessResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun defaultSampleData_hasProfileAndPosts() {
        val state = DefaultSampleData.createInitialState()
        assertEquals("themovietimez", state.profile.username)
        assertTrue(state.posts.isNotEmpty())
        assertTrue(state.dmThreads.any { it.isMetaAi })
    }

    @Test
    fun authSession_storesSessionDetailsCorrectly() {
        val session = AuthSession(
            userId = "user-12345",
            email = "test@instagram.com",
            username = "cinephile",
            accessToken = "jwt.token.here",
            refreshToken = "refresh.token",
            expiresAt = 1890000000000L,
        )

        assertEquals("user-12345", session.userId)
        assertEquals("test@instagram.com", session.email)
        assertEquals("cinephile", session.username)
        assertEquals("jwt.token.here", session.accessToken)
    }

    @Test
    fun subscriptionAccessResult_evaluatesCorrectly() {
        val activeAccess = SubscriptionAccessResult(
            isAllowed = true,
            accountStatus = "active",
            subscriptionStatus = "active",
            expiryDate = "2027-01-01T00:00:00Z",
            productName = "Insight Editor",
            message = "Access granted.",
        )
        assertTrue(activeAccess.isAllowed)

        val expiredAccess = SubscriptionAccessResult(
            isAllowed = false,
            accountStatus = "active",
            subscriptionStatus = "expired",
            expiryDate = "2025-01-01T00:00:00Z",
            productName = "Insight Editor",
            message = "Subscription has expired.",
        )
        assertFalse(expiredAccess.isAllowed)
    }
}
