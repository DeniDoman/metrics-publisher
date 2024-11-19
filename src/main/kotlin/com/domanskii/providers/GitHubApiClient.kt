package com.domanskii.providers


interface Base {
    val ref: String
}

interface PullRequest {
    val number: Int
    var body: String
    val base: Base
    val isMerged: Boolean
}

interface Commit {
    fun listPullRequests(): List<PullRequest>
}

interface Repository {
    fun getCommit(sha: String): Commit
}

interface GitHubApiClient {
    fun getRepository(repoName: String): Repository
}