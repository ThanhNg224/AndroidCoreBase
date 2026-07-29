package com.thanhng224.androidcorebase.core.architecture

public interface UseCase<in P, R> {
    public suspend operator fun invoke(params: P): R
}
