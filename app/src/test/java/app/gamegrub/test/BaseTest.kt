package app.gamegrub.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseTest {

    protected val testDispatcher = TestCoroutineDispatcher()
    protected val testScope = TestCoroutineScope(testDispatcher)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cleanupTestCoroutines()
    }

    protected fun runTest(block: suspend TestCoroutineScope.() -> Unit) {
        testScope.launch(block = block)
        testScope.advanceUntilIdle()
    }
}
