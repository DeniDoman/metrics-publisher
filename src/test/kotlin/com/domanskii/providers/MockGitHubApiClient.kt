package com.domanskii.providers

class MockGitHubApiClient(
    private val pullRequests: List<PullRequest>
) : GitHubApiClient {
    override fun getRepository(repoName: String): Repository {
        return object : Repository {
            override fun getCommit(sha: String): Commit {
                return object : Commit {
                    override fun listPullRequests(): List<PullRequest> {
                        return pullRequests
                    }
                }
            }
        }
    }
}
