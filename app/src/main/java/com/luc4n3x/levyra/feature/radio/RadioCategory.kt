package com.luc4n3x.levyra.feature.radio

internal enum class RadioCategory(val apiTag: String?) {
    Popular(null),
    Music("music"),
    Pop("pop"),
    Rock("rock"),
    HipHop("hip hop"),
    Electronic("electronic"),
    Dance("dance"),
    Chill("chillout"),
    Jazz("jazz"),
    Classical("classical"),
    News("news"),
    Talk("talk"),
    Sport("sports"),
    Local("local"),
    Worldwide(null)
}
