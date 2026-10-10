package com.luc4n3x.levyra.ui.i18n

internal val freshCurrentsKeyOrder = listOf(
    "freshScopeWorld",
    "freshMomentTitle"
)

private val freshCurrentsCatalog = """
@@en
Worldwide
Tracks of the moment
@@it
Nel mondo
I brani del momento
@@es
En el mundo
Las canciones del momento
@@fr
Dans le monde
Les titres du moment
@@de
Weltweit
Songs des Augenblicks
@@pt
No mundo
As músicas do momento
@@nl
Wereldwijd
Nummers van het moment
@@pl
Na świecie
Utwory chwili
@@ro
În lume
Piesele momentului
@@el
Παγκοσμίως
Τα τραγούδια της στιγμής
@@sv
I världen
Låtarna just nu
@@da
I verden
Numrene lige nu
@@cs
Ve světě
Skladby okamžiku
@@uk
У світі
Треки моменту
@@ru
В мире
Треки момента
@@tr
Dünyada
Anın parçaları
@@ar
حول العالم
أغاني اللحظة
@@zh
全球
此刻热曲
@@ja
世界
いま注目の曲
@@ko
전 세계
지금의 트랙
@@hi
दुनिया भर में
इस पल के गाने
@@id
Seluruh dunia
Lagu saat ini
@@vi
Toàn cầu
Bài hát của khoảnh khắc
@@th
ทั่วโลก
เพลงแห่งช่วงเวลานี้
@@fil
Sa buong mundo
Mga kantang uso ngayon
@@he
בעולם
הלהיטים של הרגע
@@fi
Maailmalla
Hetken kappaleet
@@et
Maailmas
Hetke lood
""".trimIndent()

internal val freshCurrentsEntries = buildMap {
    var activeCode = ""
    val values = ArrayList<String>(freshCurrentsKeyOrder.size)

    fun storeActiveBundle() {
        if (activeCode.isBlank()) return
        require(values.size == freshCurrentsKeyOrder.size) {
            "Invalid fresh currents bundle for $activeCode: ${values.size}/${freshCurrentsKeyOrder.size}"
        }
        put(activeCode, freshCurrentsKeyOrder.zip(values).toMap())
        values.clear()
    }

    freshCurrentsCatalog.lineSequence().forEach { line ->
        if (line.startsWith("@@")) {
            storeActiveBundle()
            activeCode = line.removePrefix("@@")
        } else {
            values += line
        }
    }
    storeActiveBundle()
}

internal fun freshCurrentsLocalizationEntries(code: String): Map<String, String> {
    return freshCurrentsEntries[code] ?: freshCurrentsEntries.getValue("en")
}
