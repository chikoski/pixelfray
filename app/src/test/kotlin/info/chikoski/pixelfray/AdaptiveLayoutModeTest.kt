package info.chikoski.pixelfray

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutModeTest {
    @Test
    fun `resolveLayoutMode selects compact below 600dp and medium plus at or above 600dp`() {
        assertEquals(PixelfrayLayoutMode.COMPACT, resolveLayoutMode(380f, tabletopSplit = null))
        assertEquals(PixelfrayLayoutMode.COMPACT, resolveLayoutMode(599.9f, tabletopSplit = null))
        assertEquals(PixelfrayLayoutMode.MEDIUM_PLUS, resolveLayoutMode(600f, tabletopSplit = null))
        assertEquals(PixelfrayLayoutMode.MEDIUM_PLUS, resolveLayoutMode(960f, tabletopSplit = null))
    }

    @Test
    fun `resolveLayoutMode prioritizes tabletop when hinge split is active`() {
        val split = TabletopSplitPx(topPanePx = 350, hingeGapPx = 24, bottomPanePx = 350)
        assertEquals(PixelfrayLayoutMode.TABLETOP, resolveLayoutMode(380f, tabletopSplit = split))
        assertEquals(PixelfrayLayoutMode.TABLETOP, resolveLayoutMode(840f, tabletopSplit = split))
    }

    @Test
    fun `computeTabletopSplitPx maps physical hinge bounds relative to content top`() {
        val split = computeTabletopSplitPx(
            contentTopPx = 80,
            contentHeightPx = 820,
            hingeTopPx = 438,
            hingeBottomPx = 478,
            minHingeGapPx = 24,
            minPaneHeightPx = 180,
        )

        assertNotNull(split)
        requireNotNull(split)
        assertEquals(358, split.topPanePx)
        assertEquals(40, split.hingeGapPx)
        assertEquals(422, split.bottomPanePx)
        assertEquals(820, split.topPanePx + split.hingeGapPx + split.bottomPanePx)
    }

    @Test
    fun `computeTabletopSplitPx expands zero-width crease hinge to minHingeGapPx`() {
        val split = computeTabletopSplitPx(
            contentTopPx = 80,
            contentHeightPx = 820,
            hingeTopPx = 490,
            hingeBottomPx = 490,
            minHingeGapPx = 24,
            minPaneHeightPx = 180,
        )

        assertNotNull(split)
        requireNotNull(split)
        assertEquals(398, split.topPanePx)
        assertEquals(24, split.hingeGapPx)
        assertEquals(398, split.bottomPanePx)
        assertEquals(820, split.topPanePx + split.hingeGapPx + split.bottomPanePx)
    }

    @Test
    fun `computeTabletopSplitPx returns null for out-of-bounds, inverted, or undersized panes`() {
        // Hinge above content
        assertNull(
            computeTabletopSplitPx(
                contentTopPx = 80,
                contentHeightPx = 820,
                hingeTopPx = 20,
                hingeBottomPx = 60,
                minHingeGapPx = 24,
                minPaneHeightPx = 180,
            ),
        )

        // Hinge below content
        assertNull(
            computeTabletopSplitPx(
                contentTopPx = 80,
                contentHeightPx = 820,
                hingeTopPx = 900,
                hingeBottomPx = 924,
                minHingeGapPx = 24,
                minPaneHeightPx = 180,
            ),
        )

        // Inverted hinge bounds
        assertNull(
            computeTabletopSplitPx(
                contentTopPx = 80,
                contentHeightPx = 820,
                hingeTopPx = 500,
                hingeBottomPx = 480,
                minHingeGapPx = 24,
                minPaneHeightPx = 180,
            ),
        )

        // Top pane smaller than minimum
        assertNull(
            computeTabletopSplitPx(
                contentTopPx = 80,
                contentHeightPx = 820,
                hingeTopPx = 200,
                hingeBottomPx = 224,
                minHingeGapPx = 24,
                minPaneHeightPx = 180,
            ),
        )

        // Bottom pane smaller than minimum
        assertNull(
            computeTabletopSplitPx(
                contentTopPx = 80,
                contentHeightPx = 820,
                hingeTopPx = 760,
                hingeBottomPx = 784,
                minHingeGapPx = 24,
                minPaneHeightPx = 180,
            ),
        )

        // Zero or negative content height (initial layout pass)
        assertNull(
            computeTabletopSplitPx(
                contentTopPx = 0,
                contentHeightPx = 0,
                hingeTopPx = 400,
                hingeBottomPx = 424,
                minHingeGapPx = 24,
                minPaneHeightPx = 240,
            ),
        )

        // minHingeGapPx larger than content height
        assertNull(
            computeTabletopSplitPx(
                contentTopPx = 80,
                contentHeightPx = 200,
                hingeTopPx = 180,
                hingeBottomPx = 180,
                minHingeGapPx = 240,
                minPaneHeightPx = 50,
            ),
        )
    }

    @Test
    fun `computeTabletopSplitPx handles exact minimum panes, top-edge overlap, and clamped gap expansion`() {
        // Exact minimum pane boundary (240 + 24 + 240 = 504)
        val exactMin = computeTabletopSplitPx(
            contentTopPx = 50,
            contentHeightPx = 504,
            hingeTopPx = 290,
            hingeBottomPx = 314,
            minHingeGapPx = 24,
            minPaneHeightPx = 240,
        )
        assertNotNull(exactMin)
        requireNotNull(exactMin)
        assertEquals(240, exactMin.topPanePx)
        assertEquals(24, exactMin.hingeGapPx)
        assertEquals(240, exactMin.bottomPanePx)

        // Partial overlap at top edge (rawTop < 0 < rawBottom) yields topPanePx = 0 < minPaneHeightPx
        val partialTop = computeTabletopSplitPx(
            contentTopPx = 100,
            contentHeightPx = 600,
            hingeTopPx = 80,
            hingeBottomPx = 160,
            minHingeGapPx = 24,
            minPaneHeightPx = 180,
        )
        assertNull(partialTop)

        // Hinge gap expansion clamped near content bottom edge when minPaneHeightPx allows
        val clampedBottom = computeTabletopSplitPx(
            contentTopPx = 0,
            contentHeightPx = 500,
            hingeTopPx = 480,
            hingeBottomPx = 482,
            minHingeGapPx = 40,
            minPaneHeightPx = 0,
        )
        assertNotNull(clampedBottom)
        requireNotNull(clampedBottom)
        assertEquals(460, clampedBottom.topPanePx)
        assertEquals(40, clampedBottom.hingeGapPx)
        assertEquals(0, clampedBottom.bottomPanePx)
        assertEquals(500, clampedBottom.topPanePx + clampedBottom.hingeGapPx + clampedBottom.bottomPanePx)
    }

    @Test
    fun `shouldShowAppBar hides top bar only in tabletop mode`() {
        assertTrue(shouldShowAppBar(PixelfrayLayoutMode.COMPACT))
        assertTrue(shouldShowAppBar(PixelfrayLayoutMode.MEDIUM_PLUS))
        assertFalse(shouldShowAppBar(PixelfrayLayoutMode.TABLETOP))
    }

    @Test
    fun `root-measured tabletop split reserves entire upper pane above hinge when app bar is hidden`() {
        val split = computeTabletopSplitPx(
            contentTopPx = 0,
            contentHeightPx = 900,
            hingeTopPx = 438,
            hingeBottomPx = 462,
            minHingeGapPx = 24,
            minPaneHeightPx = 240,
        )

        assertNotNull(split)
        requireNotNull(split)
        assertEquals(PixelfrayLayoutMode.TABLETOP, resolveLayoutMode(840f, split))
        assertFalse(shouldShowAppBar(PixelfrayLayoutMode.TABLETOP))
        assertEquals(438, split.topPanePx)
        assertEquals(24, split.hingeGapPx)
        assertEquals(438, split.bottomPanePx)
    }
}
