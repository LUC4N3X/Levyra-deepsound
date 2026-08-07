from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly 1 match, found {count}")
    return text.replace(old, new, 1)


vm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
vm = vm_path.read_text()

if "internal fun shouldReuseFreshCurrentsRequest(" not in vm:
    marker = "internal fun shouldDispatchPlaybackStartSideEffects(startPaused: Boolean): Boolean = !startPaused\n"
    helper = (
        marker
        + "\ninternal fun shouldReuseFreshCurrentsRequest(\n"
        + "    activeRequestLanguage: String,\n"
        + "    requestedLanguage: String,\n"
        + "    force: Boolean\n"
        + "): Boolean = !force && activeRequestLanguage == requestedLanguage\n"
    )
    vm = replace_once(vm, marker, helper, "fresh-current request policy helper")

vm = replace_once(
    vm,
    '    private var freshCurrentsLoadedLanguage = ""\n'
    '    private var freshCurrentsRequestGeneration = 0L\n',
    '    private var freshCurrentsLoadedLanguage = ""\n'
    '    private var freshCurrentsRequestLanguage = ""\n'
    '    private var freshCurrentsRequestGeneration = 0L\n',
    "fresh-current request language state",
)

vm = replace_once(
    vm,
    "        val snapshot = _state.value\n"
    "        val languageCode = snapshot.languageCode\n"
    "        if (freshCurrentsJob?.isActive == true) {\n"
    "            if (!force && freshCurrentsLoadedLanguage == languageCode) return\n"
    "            freshCurrentsJob?.cancel()\n"
    "        }\n",
    "        val snapshot = _state.value\n"
    "        val languageCode = snapshot.languageCode\n"
    "        if (freshCurrentsJob?.isActive == true) {\n"
    "            if (shouldReuseFreshCurrentsRequest(freshCurrentsRequestLanguage, languageCode, force)) return\n"
    "            freshCurrentsJob?.cancel()\n"
    "        }\n",
    "fresh-current active request ownership",
)

vm = replace_once(
    vm,
    "        val requestGeneration = ++freshCurrentsRequestGeneration\n"
    "        _state.update { current ->\n",
    "        val requestGeneration = ++freshCurrentsRequestGeneration\n"
    "        freshCurrentsRequestLanguage = languageCode\n"
    "        _state.update { current ->\n",
    "fresh-current request language capture",
)

vm_path.write_text(vm)


test_path = Path("app/src/test/java/com/luc4n3x/levyra/viewmodel/FreshCurrentsRequestPolicyTest.kt")
test_path.write_text('''package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshCurrentsRequestPolicyTest {
    @Test
    fun sameLanguageActiveRequestCanBeReused() {
        assertTrue(
            shouldReuseFreshCurrentsRequest(
                activeRequestLanguage = "it",
                requestedLanguage = "it",
                force = false
            )
        )
    }

    @Test
    fun rapidLanguageRoundTripDoesNotReuseWrongActiveRequest() {
        assertFalse(
            shouldReuseFreshCurrentsRequest(
                activeRequestLanguage = "en",
                requestedLanguage = "it",
                force = false
            )
        )
    }

    @Test
    fun forcedRefreshNeverReusesTheActiveRequest() {
        assertFalse(
            shouldReuseFreshCurrentsRequest(
                activeRequestLanguage = "it",
                requestedLanguage = "it",
                force = true
            )
        )
    }
}
''')

assert 'freshCurrentsRequestLanguage = languageCode' in vm
assert 'freshCurrentsLoadedLanguage == languageCode) return' not in vm
assert 'shouldReuseFreshCurrentsRequest(freshCurrentsRequestLanguage, languageCode, force)' in vm
print("PR313_FRESH_REQUEST_LANGUAGE_FIX_OK")
