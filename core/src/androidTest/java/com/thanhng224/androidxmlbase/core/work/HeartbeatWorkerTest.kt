package com.thanhng224.androidxmlbase.core.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeartbeatWorkerTest {
    @Test
    fun doWork_returnsSuccess() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val worker = TestListenableWorkerBuilder<HeartbeatWorker>(context).build()

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
        }
}
