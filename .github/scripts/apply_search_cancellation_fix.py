from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
text = path.read_text()
old = '''        }.onFailure { error ->
            _state.update {
                it.copy(
                    isSearching = false,
                    searchError = error.message ?: "Ricerca non riuscita"
                )
            }
        }
'''
new = '''        }.onFailure { error ->
            if (error is CancellationException) throw error
            _state.update {
                it.copy(
                    isSearching = false,
                    searchError = error.message ?: "Ricerca non riuscita"
                )
            }
        }
'''
count = text.count(old)
if count != 1:
    raise SystemExit(f"Expected one runSearch failure block, found {count}")
path.write_text(text.replace(old, new))
