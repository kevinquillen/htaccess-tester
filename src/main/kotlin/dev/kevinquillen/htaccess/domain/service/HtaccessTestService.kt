package dev.kevinquillen.htaccess.domain.service

import dev.kevinquillen.htaccess.domain.model.ShareResult
import dev.kevinquillen.htaccess.domain.model.TestRequest
import dev.kevinquillen.htaccess.domain.model.TestResult

interface HtaccessTestService {
    suspend fun test(request: TestRequest): TestResult
    suspend fun share(request: TestRequest): ShareResult
}
