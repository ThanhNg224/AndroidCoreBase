package com.thanhng224.androidxmlbase.core.architecture

interface UseCase<in P, R> {
    suspend operator fun invoke(params: P): R
}
