package com.luc4n3x.levyra.architecture.mobius

data class LevyraNext<Model, Effect>(
    val model: Model,
    val effects: Set<Effect> = emptySet()
) {
    companion object {
        fun <Model, Effect> next(model: Model): LevyraNext<Model, Effect> = LevyraNext(model)

        fun <Model, Effect> dispatch(model: Model, vararg effects: Effect): LevyraNext<Model, Effect> =
            LevyraNext(model, effects.toSet())
    }
}

fun interface LevyraInit<Model, Effect> {
    fun init(model: Model): LevyraNext<Model, Effect>
}

fun interface LevyraUpdate<Model, Event, Effect> {
    fun update(model: Model, event: Event): LevyraNext<Model, Effect>
}

fun interface LevyraViewMapper<Model, ViewData> {
    fun map(model: Model): ViewData
}
