package com.thanhng224.androidxmlbase.core.architecture

public interface UseCase<in P, R> {
    public suspend operator fun invoke(params: P): R
}
