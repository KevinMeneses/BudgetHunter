@file:OptIn(KoinInternalApi::class)

package com.meneses.budgethunter.di

import com.meneses.budgethunter.auth.data.TokenStorage
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetMetrics.BudgetMetricsViewModel
import com.meneses.budgethunter.collaborator.CollaboratorsViewModel
import com.meneses.budgethunter.commons.data.FileData
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.GoogleSignInManager
import com.meneses.budgethunter.commons.platform.GoogleSignInResult
import com.meneses.budgethunter.commons.platform.KeychainStore
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.ShareManager
import kotlinx.coroutines.test.runTest
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatformTools
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Starts Koin the way the iOS entry points do, with fakes in place of the Swift managers, and
 * checks the whole graph can be built: a definition left without what it needs fails here
 * instead of when the app reaches that screen.
 *
 * Koin is started once for the whole run and never stopped: the DataStore it builds cannot be
 * created twice for the same file in one process.
 */
class IosKoinGraphTest {

    private val koin: Koin
        get() {
            startKoinIfNeeded { fakeNativeDependencies }
            return KoinPlatformTools.defaultContext().get()
        }

    @Test
    fun everyDefinitionCanBeCreated() {
        val koin = koin
        val definitions = koin.instanceRegistry.instances.values
            .map { it.beanDefinition }
            .distinct()
            .filter { it.scopeQualifier == koin.scopeRegistry.rootScope.scopeQualifier }

        val failures = definitions.mapNotNull { definition ->
            val parameters = parametersFor(definition.primaryType)
            try {
                koin.get<Any>(
                    clazz = definition.primaryType,
                    qualifier = definition.qualifier,
                    parameters = parameters?.let { holder -> { holder } }
                )
                null
            } catch (e: Throwable) {
                val name = definition.primaryType.simpleName + (definition.qualifier?.let { " ($it)" } ?: "")
                "$name: ${e.message}" + (e.cause?.let { " <- ${it.message}" } ?: "")
            }
        }

        assertTrue(definitions.isNotEmpty(), "No definitions found in Koin")
        assertTrue(
            failures.isEmpty(),
            "Could not create:\n" + failures.joinToString("\n") +
                "\n(a definition declared with parameters needs them in parametersFor)"
        )
    }

    @Test
    fun theSwiftManagersAreTheOnesKoinHandsOut() {
        val koin = koin
        assertSame(fakeNativeDependencies.cameraManager, koin.get<CameraManager>())
        assertSame(fakeNativeDependencies.filePickerManager, koin.get<FilePickerManager>())
        assertSame(fakeNativeDependencies.shareManager, koin.get<ShareManager>())
        assertSame(fakeNativeDependencies.notificationManager, koin.get<NotificationManager>())
        assertSame(fakeNativeDependencies.googleSignInManager, koin.get<GoogleSignInManager>())
    }

    @Test
    fun tokensAreKeptInTheSwiftKeychain() = runTest {
        koin.get<TokenStorage>().saveRefreshToken("refresh-token")

        assertTrue(fakeKeychain.values.containsValue("refresh-token"))
    }

    @Test
    fun startingKoinAgainDoesNotCreateNewDependencies() {
        val koin = koin
        var created = false

        startKoinIfNeeded {
            created = true
            FakeNativeDependencies(FakeKeychainStore())
        }

        assertFalse(created, "The dependencies were created although Koin was running")
        assertSame(fakeNativeDependencies.cameraManager, koin.get<CameraManager>())
        assertSame(koin, KoinPlatformTools.defaultContext().get())
    }

    /** Definitions declared with `factory { (x: X) -> ... }` need their parameters. */
    private fun parametersFor(type: KClass<*>) = when (type) {
        BudgetMetricsViewModel::class -> parametersOf(Budget())
        CollaboratorsViewModel::class -> parametersOf(1L)
        else -> null
    }

    private companion object {
        val fakeKeychain = FakeKeychainStore()
        val fakeNativeDependencies = FakeNativeDependencies(fakeKeychain)
    }
}

private class FakeNativeDependencies(override val keychainStore: KeychainStore) : NativeDependencies {
    override val cameraManager: CameraManager = object : CameraManager {
        override fun takePhoto(onResult: (FileData?) -> Unit) = onResult(null)
    }
    override val filePickerManager: FilePickerManager = object : FilePickerManager {
        override fun pickFile(mimeTypes: Array<String>, onResult: (FileData?) -> Unit) = onResult(null)
    }
    override val shareManager: ShareManager = object : ShareManager {
        override fun shareFile(filePath: String, mimeTypes: Array<String>) = Unit
        override fun openFile(filePath: String, mimeType: String) = false
    }
    override val notificationManager: NotificationManager = object : NotificationManager {
        override fun showToast(message: String) = Unit
        override fun showNotification(title: String, message: String) = Unit
        override fun showEntryAddedNotification(title: String, message: String) = Unit
        override fun showOpenSettingsNotification(title: String, message: String) = Unit
    }
    override val googleSignInManager: GoogleSignInManager = object : GoogleSignInManager {
        override val isAvailable = false
        override fun signIn(onResult: (GoogleSignInResult) -> Unit) = onResult(GoogleSignInResult.Cancelled)
        override fun signOut() = Unit
    }
}

private class FakeKeychainStore : KeychainStore {
    val values = mutableMapOf<String, String>()

    override fun save(key: String, value: String) {
        values[key] = value
    }

    override fun read(key: String): String? = values[key]

    override fun delete(key: String) {
        values.remove(key)
    }
}
