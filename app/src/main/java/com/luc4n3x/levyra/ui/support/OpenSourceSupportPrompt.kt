package com.luc4n3x.levyra.ui.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val REPOSITORY_URL = "https://github.com/LUC4N3X/Levyra-deepsound"
private const val PROMPT_DELAY_MS = 800L
private const val SUPPORT_PREFERENCES = "levyra_support_prompt"
private const val KEY_PROMPT_SEEN = "open_source_support_prompt_v1_seen"

internal data class OpenSourceSupportCopy(
    val badge: String,
    val title: String,
    val body: String,
    val starAction: String,
    val continueAction: String
)

internal object OpenSourceSupportStrings {
    private val bundles = mapOf(
        "en" to OpenSourceSupportCopy(
            badge = "100% OPEN SOURCE",
            title = "Levyra is free. Truly.",
            body = "Levyra is an open-source project with no subscriptions, purchases or donation requests. If the app is useful to you, the best way to support the work behind it is to leave the project a star on GitHub. It costs nothing and helps more people discover Levyra.",
            starAction = "Star Levyra on GitHub",
            continueAction = "Continue"
        ),
        "it" to OpenSourceSupportCopy(
            badge = "100% OPEN SOURCE",
            title = "Levyra è gratuita. Davvero.",
            body = "Levyra è un progetto open source: niente abbonamenti, acquisti o richieste di donazioni. Se l’app ti è utile, il modo migliore per sostenere il lavoro che c’è dietro è lasciare una stella al progetto su GitHub. Non costa nulla e aiuta più persone a scoprire Levyra.",
            starAction = "Lascia una stella su GitHub",
            continueAction = "Continua"
        ),
        "es" to OpenSourceSupportCopy(
            badge = "100% CÓDIGO ABIERTO",
            title = "Levyra es gratis. De verdad.",
            body = "Levyra es un proyecto de código abierto: no hay suscripciones, compras ni solicitudes de donaciones. Si la aplicación te resulta útil, la mejor forma de apoyar el trabajo que hay detrás es dejar una estrella al proyecto en GitHub. No cuesta nada y ayuda a que más personas descubran Levyra.",
            starAction = "Dar una estrella en GitHub",
            continueAction = "Continuar"
        ),
        "fr" to OpenSourceSupportCopy(
            badge = "100% OPEN SOURCE",
            title = "Levyra est gratuite. Vraiment.",
            body = "Levyra est un projet open source, sans abonnement, achat ni demande de don. Si l’application vous est utile, la meilleure façon de soutenir le travail réalisé est d’attribuer une étoile au projet sur GitHub. Cela ne coûte rien et aide davantage de personnes à découvrir Levyra.",
            starAction = "Ajouter une étoile sur GitHub",
            continueAction = "Continuer"
        ),
        "de" to OpenSourceSupportCopy(
            badge = "100% OPEN SOURCE",
            title = "Levyra ist kostenlos. Wirklich.",
            body = "Levyra ist ein Open-Source-Projekt – ohne Abonnements, In-App-Käufe oder Spendenaufrufe. Wenn dir die App gefällt, unterstützt du die Arbeit dahinter am besten mit einem Stern für das Projekt auf GitHub. Das kostet nichts und hilft mehr Menschen, Levyra zu entdecken.",
            starAction = "Auf GitHub einen Stern geben",
            continueAction = "Weiter"
        ),
        "pt" to OpenSourceSupportCopy(
            badge = "100% CÓDIGO ABERTO",
            title = "Levyra é gratuita. De verdade.",
            body = "Levyra é um projeto de código aberto, sem assinaturas, compras ou pedidos de doação. Se o aplicativo for útil para você, a melhor forma de apoiar o trabalho por trás dele é dar uma estrela ao projeto no GitHub. Não custa nada e ajuda mais pessoas a descobrir Levyra.",
            starAction = "Dar uma estrela no GitHub",
            continueAction = "Continuar"
        ),
        "nl" to OpenSourceSupportCopy(
            badge = "100% OPEN SOURCE",
            title = "Levyra is gratis. Echt.",
            body = "Levyra is een opensourceproject zonder abonnementen, aankopen of verzoeken om donaties. Vind je de app nuttig, dan kun je het werk erachter het best steunen door het project een ster te geven op GitHub. Het kost niets en helpt meer mensen Levyra te ontdekken.",
            starAction = "Geef een ster op GitHub",
            continueAction = "Doorgaan"
        ),
        "pl" to OpenSourceSupportCopy(
            badge = "100% OPEN SOURCE",
            title = "Levyra jest bezpłatna. Naprawdę.",
            body = "Levyra to projekt open source — bez subskrypcji, zakupów i próśb o wpłaty. Jeśli aplikacja jest dla Ciebie przydatna, najlepszym sposobem na wsparcie pracy nad nią jest pozostawienie gwiazdki projektowi w serwisie GitHub. Nic to nie kosztuje, a pomaga większej liczbie osób odkryć Levyra.",
            starAction = "Zostaw gwiazdkę na GitHubie",
            continueAction = "Kontynuuj"
        ),
        "ro" to OpenSourceSupportCopy(
            badge = "100% SURSA DESCHISĂ",
            title = "Levyra este gratuită. Cu adevărat.",
            body = "Levyra este un proiect open-source, fără abonamente, achiziții sau solicitări de donații. Dacă aplicația îți este utilă, cel mai bun mod de a susține munca din spatele ei este să acorzi o stea proiectului pe GitHub. Nu costă nimic și ajută mai multe persoane să descopere Levyra.",
            starAction = "Oferă o stea pe GitHub",
            continueAction = "Continuă"
        ),
        "el" to OpenSourceSupportCopy(
            badge = "100% ΑΝΟΙΧΤΟΣ ΚΩΔΙΚΑΣ",
            title = "Το Levyra είναι δωρεάν. Πραγματικά.",
            body = "Το Levyra είναι έργο ανοικτού κώδικα, χωρίς συνδρομές, αγορές ή αιτήματα δωρεών. Αν η εφαρμογή σου είναι χρήσιμη, ο καλύτερος τρόπος να στηρίξεις τη δουλειά πίσω από αυτήν είναι να δώσεις ένα αστέρι στο έργο στο GitHub. Δεν κοστίζει τίποτα και βοηθά περισσότερους ανθρώπους να ανακαλύψουν το Levyra.",
            starAction = "Δώσε ένα αστέρι στο GitHub",
            continueAction = "Συνέχεια"
        ),
        "sv" to OpenSourceSupportCopy(
            badge = "100% ÖPPEN KÄLLKOD",
            title = "Levyra är gratis. På riktigt.",
            body = "Levyra är ett projekt med öppen källkod, utan abonnemang, köp eller önskemål om donationer. Om appen är användbar för dig är det bästa sättet att stödja arbetet bakom den att ge projektet en stjärna på GitHub. Det kostar ingenting och hjälper fler att upptäcka Levyra.",
            starAction = "Ge en stjärna på GitHub",
            continueAction = "Fortsätt"
        ),
        "da" to OpenSourceSupportCopy(
            badge = "100% OPEN SOURCE",
            title = "Levyra er gratis. Helt og holdent.",
            body = "Levyra er et open source-projekt uden abonnementer, køb eller anmodninger om donationer. Hvis appen er nyttig for dig, er den bedste måde at støtte arbejdet bag den på at give projektet en stjerne på GitHub. Det koster ingenting og hjælper flere med at opdage Levyra.",
            starAction = "Giv en stjerne på GitHub",
            continueAction = "Fortsæt"
        ),
        "cs" to OpenSourceSupportCopy(
            badge = "100% OTEVŘENÝ KÓD",
            title = "Levyra je zdarma. Opravdu.",
            body = "Levyra je open-source projekt bez předplatného, nákupů a žádostí o příspěvky. Pokud je pro vás aplikace užitečná, nejlépe podpoříte práci na ní udělením hvězdičky projektu na GitHubu. Nic to nestojí a pomůže to více lidem objevit Levyru.",
            starAction = "Udělit hvězdičku na GitHubu",
            continueAction = "Pokračovat"
        ),
        "uk" to OpenSourceSupportCopy(
            badge = "100% ВІДКРИТИЙ КОД",
            title = "Levyra безкоштовна. Справді.",
            body = "Levyra — проєкт із відкритим кодом без підписок, покупок і прохань про пожертви. Якщо застосунок корисний для вас, найкращий спосіб підтримати роботу над ним — поставити зірку проєкту на GitHub. Це нічого не коштує й допомагає більшій кількості людей дізнатися про Levyra.",
            starAction = "Поставити зірку на GitHub",
            continueAction = "Продовжити"
        ),
        "ru" to OpenSourceSupportCopy(
            badge = "100% ОТКРЫТЫЙ КОД",
            title = "Levyra бесплатна. Действительно.",
            body = "Levyra — проект с открытым исходным кодом без подписок, покупок и просьб о пожертвованиях. Если приложение вам полезно, лучший способ поддержать работу над ним — поставить проекту звезду на GitHub. Это ничего не стоит и помогает большему числу людей узнать о Levyra.",
            starAction = "Поставить звезду на GitHub",
            continueAction = "Продолжить"
        ),
        "tr" to OpenSourceSupportCopy(
            badge = "%100 AÇIK KAYNAK",
            title = "Levyra ücretsizdir. Gerçekten.",
            body = "Levyra; abonelik, satın alma veya bağış talebi içermeyen açık kaynaklı bir projedir. Uygulamayı faydalı buluyorsanız arkasındaki emeği desteklemenin en iyi yolu projeye GitHub’da yıldız vermektir. Hiçbir ücret gerektirmez ve daha fazla kişinin Levyra’yı keşfetmesine yardımcı olur.",
            starAction = "GitHub’da yıldız ver",
            continueAction = "Devam et"
        ),
        "ar" to OpenSourceSupportCopy(
            badge = "مفتوح المصدر 100٪",
            title = "⁨Levyra⁩ مجانية. فعلًا.",
            body = "⁨Levyra⁩ مشروع مفتوح المصدر بلا اشتراكات أو مشتريات أو طلبات تبرع. إذا كان التطبيق مفيدًا لك، فأفضل طريقة لدعم العمل المبذول فيه هي منح المشروع نجمة على ⁨GitHub⁩. لن يكلفك ذلك شيئًا، وسيساعد مزيدًا من الأشخاص على اكتشاف ⁨Levyra⁩.",
            starAction = "امنح ⁨Levyra⁩ نجمة على ⁨GitHub⁩",
            continueAction = "متابعة"
        ),
        "zh" to OpenSourceSupportCopy(
            badge = "100% 开源",
            title = "Levyra 完全免费。真的。",
            body = "Levyra 是一个开源项目，不设订阅、应用内购买，也不会请求捐赠。如果这款应用对你有帮助，支持这项工作的最佳方式就是在 GitHub 上为项目点一颗星。这不会产生任何费用，也能帮助更多人发现 Levyra。",
            starAction = "在 GitHub 上点星支持",
            continueAction = "继续"
        ),
        "ja" to OpenSourceSupportCopy(
            badge = "100% オープンソース",
            title = "Levyra は本当に無料です。",
            body = "Levyra は、サブスクリプション、アプリ内購入、寄付のお願いが一切ないオープンソースプロジェクトです。アプリが役に立ったと感じたら、GitHub でプロジェクトにスターを付けていただくことが、開発を応援する一番の方法です。費用はかからず、より多くの人に Levyra を知ってもらう助けになります。",
            starAction = "GitHub でスターを付ける",
            continueAction = "続ける"
        ),
        "ko" to OpenSourceSupportCopy(
            badge = "100% 오픈 소스",
            title = "Levyra는 완전히 무료입니다.",
            body = "Levyra는 구독, 인앱 구매, 기부 요청이 없는 오픈 소스 프로젝트입니다. 앱이 유용했다면 GitHub에서 프로젝트에 별을 남겨 주세요. 비용은 전혀 들지 않으며 더 많은 사람이 Levyra를 발견하는 데 도움이 됩니다.",
            starAction = "GitHub에서 별 남기기",
            continueAction = "계속"
        ),
        "hi" to OpenSourceSupportCopy(
            badge = "100% ओपन सोर्स",
            title = "Levyra पूरी तरह मुफ़्त है।",
            body = "Levyra एक ओपन-सोर्स परियोजना है—इसमें कोई सदस्यता, खरीदारी या दान का अनुरोध नहीं है। यदि यह ऐप आपके लिए उपयोगी है, तो इसके पीछे के काम का समर्थन करने का सबसे अच्छा तरीका GitHub पर परियोजना को एक स्टार देना है। इसमें कोई खर्च नहीं होता और इससे अधिक लोगों को Levyra के बारे में पता चलता है।",
            starAction = "GitHub पर स्टार दें",
            continueAction = "जारी रखें"
        ),
        "id" to OpenSourceSupportCopy(
            badge = "100% SUMBER TERBUKA",
            title = "Levyra gratis. Benar-benar gratis.",
            body = "Levyra adalah proyek sumber terbuka tanpa langganan, pembelian, atau permintaan donasi. Jika aplikasi ini bermanfaat, cara terbaik untuk mendukung pekerjaan di baliknya adalah memberi bintang pada proyek di GitHub. Tidak dipungut biaya dan hal ini membantu lebih banyak orang menemukan Levyra.",
            starAction = "Beri bintang di GitHub",
            continueAction = "Lanjutkan"
        ),
        "vi" to OpenSourceSupportCopy(
            badge = "100% MÃ NGUỒN MỞ",
            title = "Levyra hoàn toàn miễn phí.",
            body = "Levyra là một dự án mã nguồn mở, không có gói đăng ký, mua hàng hay lời kêu gọi quyên góp. Nếu ứng dụng hữu ích với bạn, cách tốt nhất để ủng hộ công sức phía sau là tặng dự án một ngôi sao trên GitHub. Việc này hoàn toàn miễn phí và giúp nhiều người biết đến Levyra hơn.",
            starAction = "Tặng sao trên GitHub",
            continueAction = "Tiếp tục"
        ),
        "th" to OpenSourceSupportCopy(
            badge = "โอเพนซอร์ส 100%",
            title = "Levyra ใช้งานฟรีจริง ๆ",
            body = "Levyra เป็นโครงการโอเพนซอร์ส ไม่มีค่าสมาชิก การซื้อภายในแอป หรือการขอรับบริจาค หากแอปนี้มีประโยชน์สำหรับคุณ วิธีที่ดีที่สุดในการสนับสนุนงานเบื้องหลังคือกดดาวให้โครงการบน GitHub โดยไม่มีค่าใช้จ่าย และยังช่วยให้ผู้คนค้นพบ Levyra มากขึ้น",
            starAction = "กดดาวให้บน GitHub",
            continueAction = "ดำเนินการต่อ"
        ),
        "fil" to OpenSourceSupportCopy(
            badge = "100% OPEN SOURCE",
            title = "Libre ang Levyra. Talagang libre.",
            body = "Ang Levyra ay isang open-source na proyekto na walang subscription, bayad na pagbili, o panghihingi ng donasyon. Kung kapaki-pakinabang sa iyo ang app, ang pinakamainam na paraan para suportahan ang gawaing nasa likod nito ay bigyan ng star ang proyekto sa GitHub. Wala itong bayad at nakatutulong para mas marami ang makatuklas sa Levyra.",
            starAction = "Bigyan ng star sa GitHub",
            continueAction = "Magpatuloy"
        ),
        "he" to OpenSourceSupportCopy(
            badge = "100% קוד פתוח",
            title = "⁨Levyra⁩ חינמית. באמת.",
            body = "⁨Levyra⁩ היא פרויקט קוד פתוח ללא מינויים, רכישות או בקשות לתרומה. אם האפליקציה שימושית עבורך, הדרך הטובה ביותר לתמוך בעבודה שמאחוריה היא לתת לפרויקט כוכב ב־⁨GitHub⁩. זה לא עולה דבר ועוזר ליותר אנשים לגלות את ⁨Levyra⁩.",
            starAction = "לתת כוכב ב־⁨GitHub⁩",
            continueAction = "המשך"
        )
    )

    fun forCode(code: String): OpenSourceSupportCopy =
        bundles[LevyraLanguageCatalog.normalize(code)] ?: bundles.getValue("en")

    fun supportedCodes(): Set<String> = bundles.keys
}

private object OpenSourceSupportPromptStore {
    fun hasSeen(context: Context): Boolean = preferences(context).getBoolean(KEY_PROMPT_SEEN, false)

    fun markSeen(context: Context) {
        preferences(context).edit().putBoolean(KEY_PROMPT_SEEN, true).apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(SUPPORT_PREFERENCES, Context.MODE_PRIVATE)
}

@Composable
fun OpenSourceSupportPromptGate(
    enabled: Boolean,
    languageCode: String
) {
    val context = LocalContext.current.applicationContext
    var visible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(enabled, languageCode) {
        visible = false
        if (!enabled) return@LaunchedEffect
        val eligible = withContext(Dispatchers.IO) {
            LevyraPreferences(context).isOnboarded() && !OpenSourceSupportPromptStore.hasSeen(context)
        }
        if (!eligible) return@LaunchedEffect
        delay(PROMPT_DELAY_MS)
        visible = withContext(Dispatchers.IO) { !OpenSourceSupportPromptStore.hasSeen(context) }
    }

    if (!visible) return

    val copy = remember(languageCode) { OpenSourceSupportStrings.forCode(languageCode) }
    val layoutDirection = if (LevyraLanguageCatalog.isRtl(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
    val dismiss = {
        OpenSourceSupportPromptStore.markSeen(context)
        visible = false
    }
    val openRepository = {
        dismiss()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        OpenSourceSupportPrompt(
            copy = copy,
            onStar = openRepository,
            onDismiss = dismiss
        )
    }
}

@Composable
private fun OpenSourceSupportPrompt(
    copy: OpenSourceSupportCopy,
    onStar: () -> Unit,
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(30.dp)
    val accentBrush = Brush.linearGradient(
        listOf(
            LevyraCyan.copy(alpha = 0.95f),
            LevyraViolet.copy(alpha = 0.95f),
            LevyraOrange.copy(alpha = 0.90f)
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .shadow(30.dp, shape),
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.99f),
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.99f)
                            )
                        ),
                        shape
                    )
                    .padding(horizontal = 24.dp, vertical = 26.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(accentBrush, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(50))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = copy.badge,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = copy.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = copy.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onStar,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = copy.starAction,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = copy.continueAction,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
