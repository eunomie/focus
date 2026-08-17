package dev.eunomie.focus.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AllowedAppsTest {

    private val five = listOf("a", "b", "c", "d", "e")

    @Test
    fun `adds to the end so order follows the choosing`() {
        assertEquals(listOf("a", "b"), AllowedApps.toggle(listOf("a"), "b", max = 5))
    }

    @Test
    fun `refuses to add past the cap`() {
        assertEquals(five, AllowedApps.toggle(five, "f", max = 5))
    }

    @Test
    fun `removing works even when at the cap`() {
        assertEquals(listOf("a", "b", "d", "e"), AllowedApps.toggle(five, "c", max = 5))
    }

    @Test
    fun `moving swaps with the neighbour`() {
        assertEquals(listOf("b", "a", "c"), AllowedApps.move(listOf("a", "b", "c"), "a", 1))
        assertEquals(listOf("a", "c", "b"), AllowedApps.move(listOf("a", "b", "c"), "c", -1))
    }

    @Test
    fun `moving past either end changes nothing`() {
        val list = listOf("a", "b", "c")
        assertEquals(list, AllowedApps.move(list, "a", -1))
        assertEquals(list, AllowedApps.move(list, "c", 1))
    }

    @Test
    fun `moving something absent changes nothing`() {
        val list = listOf("a", "b")
        assertEquals(list, AllowedApps.move(list, "zzz", 1))
    }
}
