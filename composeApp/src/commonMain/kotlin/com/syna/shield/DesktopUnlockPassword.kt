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

/**
 * 桌面端解锁密码（macOS / Windows / Linux）。
 *
 * 背景：桌面 JVM 没有统一的生物识别 API，此前桌面锁定页"点一下按钮"即解锁，
 * 锁只是 UI 阻挡。本机制提供真正的第一因子（PBKDF2-HMAC-SHA256 派生校验，
 * 常数时间比较），可与 TOTP 第二因子叠加；未设置密码时行为保持原样。
 *
 * Android 端由系统生物识别承担，[supported] = false，所有操作 no-op。
 */
expect object DesktopUnlockPassword {
    /** 当前平台是否支持桌面解锁密码（Android 为 false） */
    val supported: Boolean

    /** 是否已设置密码 */
    fun hasPassword(): Boolean

    /**
     * 设置（或修改）解锁密码：PBKDF2 派生后加密存储。
     * 密码长度 < [MIN_PASSWORD_LENGTH] 或存储不可用时返回 false（不静默失败）。
     */
    fun setPassword(password: String): Boolean

    /** 校验密码（常数时间比较，防时序侧信道） */
    fun verifyPassword(password: String): Boolean

    /** 清除密码（需调用方已校验旧密码） */
    fun clearPassword()

    /** 最短密码长度 */
    val MIN_PASSWORD_LENGTH: Int
}
