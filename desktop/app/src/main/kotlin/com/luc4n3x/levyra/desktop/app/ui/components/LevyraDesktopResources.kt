package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.luc4n3x.levyra.desktop.app.generated.resources.Res
import com.luc4n3x.levyra.desktop.app.generated.resources.levyra
import com.luc4n3x.levyra.desktop.app.generated.resources.levyra_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun levyraIconPainter(): Painter = painterResource(Res.drawable.levyra)

@Composable
fun levyraLogoPainter(): Painter = painterResource(Res.drawable.levyra_logo)
