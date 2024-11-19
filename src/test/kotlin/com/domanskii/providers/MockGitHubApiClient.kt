package com.domanskii.providers

class MockGitHubApiClient : GitHubApiClient {
    override fun getRepository(repoName: String): Repository {
        return object : Repository {
            override fun getCommit(sha: String): Commit {
                return object : Commit {
                    override fun listPullRequests(): List<PullRequest> {
                        return listOf(
                            object : PullRequest {
                                override val number: Int
                                    get() = 1
                                override var body: String = ""
                                    get() = "Hello, World!"
                                override val base: Base
                                    get() = object : Base {
                                        override val ref: String
                                            get() = "main"
                                    }
                                override val isMerged: Boolean
                                    get() = true
                            }
                        )
                    }
                }
            }
        }
    }
}