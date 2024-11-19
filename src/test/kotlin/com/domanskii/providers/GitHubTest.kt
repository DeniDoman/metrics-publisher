package com.domanskii.providers

import org.junit.jupiter.api.Test

class GitHubTest {

    @Test
    fun isReferenceCommit() {
        val ghClientApi = MockGitHubApiClient()
        val ghClient = GitHub("", "", ghClientApi)
        val isReferenceCommit = ghClient.isReferenceCommit("123")
    }

    @Test
    fun publishMetrics() {
    }
}