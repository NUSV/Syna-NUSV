/*
 * Syna — LAN instant messenger (GPL-3.0)
 *
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.syna.shield

import kotlinx.coroutines.delay
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopPasswordTest {

    private val tmpPaths = mutableListOf<String>()

    private fun newPath(): String {
        val p = java.nio.file.Files.createTempDirectory("syna-pw")
            .resolve("unlock_pw").toString()
        tmpPaths.add(p)
        System.setProperty("syna.unlock_pw_path", p)
        return p
    }

    @AfterTest
    fun cleanup() {
        System.clearProperty("syna.unlock_pw_path")
        tmpPaths.forEach { runCatching { java.io.File(it).delete() } }
    }

    @Test
    fun passwordSetVerifyClearRoundTrip() {
        newPath()
        assertFalse(DesktopUnlockPassword.hasPassword())
        assertFalse(DesktopUnlockPassword.setPassword("12345"), "少于 6 位应拒绝")
        assertTrue(DesktopUnlockPassword.setPassword("correct-horse"))
        assertTrue(DesktopUnlockPassword.hasPassword())
        assertTrue(DesktopUnlockPassword.verifyPassword("correct-horse"))
        assertFalse(DesktopUnlockPassword.verifyPassword("wrong-horse"))
        assertFalse(DesktopUnlockPassword.verifyPassword(""))
        DesktopUnlockPassword.clearPassword()
        assertFalse(DesktopUnlockPassword.hasPassword())
        assertFalse(DesktopUnlockPassword.verifyPassword("correct-horse"), "清除后旧密码应失效")
    }

    @Test
    fun controllerPasswordUnlockAndBruteForceCounting() = kotlinx.coroutines.runBlocking {
        val dir = java.nio.file.Files.createTempDirectory("syna-pw-shield")
        System.setProperty("syna.unlock_pw_path", dir.resolve("unlock_pw").toString())
        tmpPaths.add(dir.resolve("unlock_pw").toString())
        val events = dir.resolve("events.jsonl").toString()
        val controller = ShieldController(enabled = true, eventsPathOverride = events)
        controller.start()
        assertTrue(DesktopUnlockPassword.setPassword("s3cret-pass"))

        controller.reportThreat(ShieldThreat.VPN_CHANGE)
        assertEquals(ShieldState.LOCKED, controller.state.value)
        // 错误密码：计入暴力防护
        controller.requestUnlockWithPassword("wrong-pass")
        assertEquals(ShieldState.LOCKED, controller.state.value)
        assertEquals(1, controller.biometricFails.value)
        // 冷却期内正确密码也被忽略（防连续尝试）
        controller.requestUnlockWithPassword("s3cret-pass")
        assertEquals(ShieldState.LOCKED, controller.state.value)
        // 冷却结束后正确密码解锁
        delay(1_200)
        controller.requestUnlockWithPassword("s3cret-pass")
        assertEquals(ShieldState.UNLOCKED, controller.state.value)
        assertEquals(0, controller.biometricFails.value)
        controller.stop()
        ShieldGate.disarm()
    }

    @Test
    fun passwordThenTotpSecondFactor() = kotlinx.coroutines.runBlocking {
        val dir = java.nio.file.Files.createTempDirectory("syna-pw-totp")
        System.setProperty("syna.unlock_pw_path", dir.resolve("unlock_pw").toString())
        tmpPaths.add(dir.resolve("unlock_pw").toString())
        val events = dir.resolve("events.jsonl").toString()
        val seedPath = dir.resolve("seed.bin").toString()
        val controller = ShieldController(enabled = true, eventsPathOverride = events, totpSeedPathOverride = seedPath)
        controller.start()
        assertTrue(DesktopUnlockPassword.setPassword("s3cret-pass"))
        controller.enableTotp()

        controller.reportThreat(ShieldThreat.VPN_CHANGE)
        assertEquals(ShieldState.LOCKED, controller.state.value)
        // 密码（第一因子）通过 → 进入 TOTP 等待，而非直接解锁
        controller.requestUnlockWithPassword("s3cret-pass")
        assertEquals(ShieldState.AWAITING_TOTP, controller.state.value)
        // 第二因子正确码 → 解锁
        val seed = TotpSeedStore.load(seedPath) ?: error("种子应存在")
        controller.verifyTotp(TotpCode.generate(seed, System.currentTimeMillis()))
        assertEquals(ShieldState.UNLOCKED, controller.state.value)
        controller.stop()
        ShieldGate.disarm()
    }
}
