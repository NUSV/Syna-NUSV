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

/** Android：认证由系统生物识别承担，桌面解锁密码不适用（全部 no-op） */
actual object DesktopUnlockPassword {
    actual val supported: Boolean = false
    actual val MIN_PASSWORD_LENGTH: Int = 6
    actual fun hasPassword(): Boolean = false
    actual fun setPassword(password: String): Boolean = false
    actual fun verifyPassword(password: String): Boolean = false
    actual fun clearPassword() {}
}
