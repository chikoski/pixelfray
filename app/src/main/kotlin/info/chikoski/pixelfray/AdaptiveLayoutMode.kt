package info.chikoski.pixelfray

enum class PixelfrayLayoutMode {
    COMPACT,
    MEDIUM_PLUS,
    TABLETOP,
}

data class TabletopSplitPx(
    val topPanePx: Int,
    val hingeGapPx: Int,
    val bottomPanePx: Int,
)

const val MEDIUM_PLUS_MIN_WIDTH_DP = 600f
const val TABLETOP_MIN_HINGE_GAP_DP = 24f
const val TABLETOP_MIN_PANE_HEIGHT_DP = 240f

/**
 * Computes top, hinge keep-clear, and bottom pane heights in pixels for a half-opened horizontal
 * fold hinge, or returns `null` if the hinge falls outside the content bounds or leaves either
 * pane smaller than [minPaneHeightPx].
 */
fun computeTabletopSplitPx(
    contentTopPx: Int,
    contentHeightPx: Int,
    hingeTopPx: Int,
    hingeBottomPx: Int,
    minHingeGapPx: Int,
    minPaneHeightPx: Int,
): TabletopSplitPx? {
    if (contentHeightPx <= 0 || minHingeGapPx < 0 || minPaneHeightPx < 0) return null
    if (hingeBottomPx < hingeTopPx) return null

    val rawTop = hingeTopPx - contentTopPx
    val rawBottom = hingeBottomPx - contentTopPx
    if (rawBottom <= 0 || rawTop >= contentHeightPx) return null

    val clampedTop = rawTop.coerceIn(0, contentHeightPx)
    val clampedBottom = rawBottom.coerceIn(clampedTop, contentHeightPx)
    val rawGap = clampedBottom - clampedTop

    val adjustedTop: Int
    val adjustedBottom: Int
    if (rawGap >= minHingeGapPx) {
        adjustedTop = clampedTop
        adjustedBottom = clampedBottom
    } else {
        val clampedMinGap = minHingeGapPx.coerceAtMost(contentHeightPx)
        val mid = (clampedTop + clampedBottom) / 2
        val halfGap = clampedMinGap / 2
        adjustedTop = (mid - halfGap).coerceIn(0, (contentHeightPx - clampedMinGap).coerceAtLeast(0))
        adjustedBottom = (adjustedTop + clampedMinGap).coerceAtMost(contentHeightPx)
    }

    val topPanePx = adjustedTop
    val hingeGapPx = adjustedBottom - adjustedTop
    val bottomPanePx = contentHeightPx - adjustedBottom

    if (topPanePx < minPaneHeightPx || bottomPanePx < minPaneHeightPx) {
        return null
    }
    return TabletopSplitPx(
        topPanePx = topPanePx,
        hingeGapPx = hingeGapPx,
        bottomPanePx = bottomPanePx,
    )
}

fun resolveLayoutMode(
    maxWidthDp: Float,
    tabletopSplit: TabletopSplitPx?,
): PixelfrayLayoutMode = when {
    tabletopSplit != null -> PixelfrayLayoutMode.TABLETOP
    maxWidthDp >= MEDIUM_PLUS_MIN_WIDTH_DP -> PixelfrayLayoutMode.MEDIUM_PLUS
    else -> PixelfrayLayoutMode.COMPACT
}
