package com.example

import com.example.data.DefaultSampleData
import org.junit.Assert.assertEquals
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
}
