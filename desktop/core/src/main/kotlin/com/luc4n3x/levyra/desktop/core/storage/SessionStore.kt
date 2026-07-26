package com.luc4n3x.levyra.desktop.core.storage

class SessionStore(private val store: JsonFileStore<SessionData>) {

    fun read(): SessionData = store.read()

    fun write(data: SessionData) = store.write(data)

    fun clear() = store.write(SessionData())

    companion object {
        fun create(paths: AppPaths): SessionStore = SessionStore(
            JsonFileStore(
                file = paths.sessionFile,
                serializer = SessionData.serializer(),
                defaultValue = { SessionData() }
            )
        )
    }
}

class WindowPlacementStore(private val store: JsonFileStore<WindowPlacement>) {

    fun read(): WindowPlacement = store.read()

    fun write(placement: WindowPlacement) = store.write(placement)

    companion object {
        fun create(paths: AppPaths): WindowPlacementStore = WindowPlacementStore(
            JsonFileStore(
                file = paths.windowFile,
                serializer = WindowPlacement.serializer(),
                defaultValue = { WindowPlacement() }
            )
        )
    }
}
