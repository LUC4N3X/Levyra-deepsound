from pathlib import Path

PATH = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")

text = PATH.read_text(encoding="utf-8")

start_marker = "@Composable\nprivate fun PlayerModeSwitch("
end_marker = "\n@Composable\nprivate fun LevyraControlPulseHandle("
start = text.index(start_marker)
end = text.index(end_marker, start)

replacement = '''@Composable
private fun PlayerModeSwitch(
    isVideoMode: Boolean,
    activeColor: Color,
    activeColorTarget: Color,
    enabled: Boolean,
    onSong: () -> Unit,
    onVideo: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val selectedContent = remember(activeColorTarget) {
        Color.White.playerContentColor(
            listOf(activeColorTarget.copy(alpha = 0.42f).playerCompositeOver(PlayerDarkSurface))
        )
    }
    Row(
        modifier = Modifier
            .selectableGroup()
            .playerGlass(
                shape = CircleShape,
                fill = Color.Black.copy(alpha = 0.45f),
                borderTop = Color.White.copy(alpha = 0.22f),
                borderBottom = Color.White.copy(alpha = 0.12f)
            )
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = activeColorTarget.copy(alpha = 0.30f),
                spotColor = activeColorTarget.copy(alpha = 0.50f)
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        PlayerModeSwitchTab(
            label = strings.song,
            selected = !isVideoMode,
            isVideoTab = false,
            activeColor = activeColor,
            selectedContent = selectedContent,
            enabled = enabled,
            onClick = onSong
        )
        PlayerModeSwitchTab(
            label = strings.video,
            selected = isVideoMode,
            isVideoTab = true,
            activeColor = activeColor,
            selectedContent = selectedContent,
            enabled = enabled,
            onClick = onVideo
        )
    }
}

@Composable
private fun PlayerModeSwitchTab(
    label: String,
    selected: Boolean,
    isVideoTab: Boolean,
    activeColor: Color,
    selectedContent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tabSpec: AnimationSpec<Color> = if (LocalAnimationsEnabled.current) {
        LevyraPlayerDesign.standardTween(180)
    } else {
        snap()
    }
    val background by animateColorAsState(
        targetValue = if (selected) activeColor.copy(alpha = 0.42f) else Color.Transparent,
        animationSpec = tabSpec,
        label = "player-mode-tab-background"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContent else LevyraPlayerDesign.TextSecondary.copy(alpha = 0.82f),
        animationSpec = tabSpec,
        label = "player-mode-tab-content"
    )
    Box(
        modifier = Modifier
            .height(LevyraPlayerDesign.MinimumTouchTarget)
            .semantics {
                this.selected = selected
                role = Role.Tab
            }
            .pressable(enabled = enabled && !selected, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (selected) {
                        Modifier
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                clip = false,
                                ambientColor = activeColor.copy(alpha = 0.30f),
                                spotColor = activeColor.copy(alpha = 0.46f)
                            )
                            .background(background, CircleShape)
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)), CircleShape)
                    } else {
                        Modifier.background(background, CircleShape)
                    }
                )
                .padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        if (selected) contentColor.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVideoTab) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
'''

text = text[:start] + replacement.rstrip() + text[end:]

old_args = (
    "                            activeColorTarget = primaryTarget,\n"
    "                            onSong = viewModel::toggleVideoMode,"
)
new_args = (
    "                            activeColorTarget = primaryTarget,\n"
    "                            enabled = !state.isResolving,\n"
    "                            onSong = viewModel::toggleVideoMode,"
)
if text.count(old_args) != 1:
    raise RuntimeError(
        f"Expected one PlayerModeSwitch argument block, found {text.count(old_args)}"
    )
text = text.replace(old_args, new_args, 1)

PATH.write_text(text, encoding="utf-8")
