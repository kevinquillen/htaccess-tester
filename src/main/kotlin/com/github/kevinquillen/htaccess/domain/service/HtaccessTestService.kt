package com.github.kevinquillen.htaccess.domain.service

import com.github.kevinquillen.htaccess.domain.model.TestRequest
import com.github.kevinquillen.htaccess.domain.model.TestResult

interface HtaccessTestService {
    suspend fun test(request: TestRequest): TestResult
}
