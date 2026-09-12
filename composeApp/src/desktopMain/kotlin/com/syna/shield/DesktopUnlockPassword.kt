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

import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 桌面解锁密码实现：PBKDF2-HMAC-SHA256（210k 迭代，16B 盐，32B 输出），
 * 存储 salt|hash（经 ShieldStorageKey 加密），原子写。
 * 测试可用系统属性 `syna.unlock_pw_path` 覆盖存储路径。
 */
actual object DesktopUnlockPassword {

    private const val ITERATIONS = 210_000
    private const val SALT_LEN = 16
    private const val KEY_LEN = 32

    private fun file(): File {
        val override = System.getProperty("syna.unlock_pw_path")
        if (override != null) return File(override)
        return File(System.getProperty("user.home") ?: ".", ".syna/unlock_pw")
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN * 8)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    actual val supported: Boolean = true

    actual val MIN_PASSWORD_LENGTH: Int = 6

    actual fun hasPassword(): Boolean = try {
        val f = file()
        f.exists() && f.length() > 0
    } catch (e: Exception) {
        false
    }

    actual fun setPassword(password: String): Boolean {
        if (password.length < MIN_PASSWORD_LENGTH) return false
        return try {
            val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
            val hash = derive(password, salt)
            val payload = salt + hash
            val enc = ShieldStorageKey.encryptWithMaster(payload) ?: return false
            val f = file()
            f.parentFile?.mkdirs()
            // 原子写：崩溃损坏会让密码校验永久失败
            val tmp = File(f.absolutePath + ".tmp")
            tmp.writeBytes(enc)
            if (!tmp.renameTo(f)) {
                f.writeBytes(enc)
                tmp.delete()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    actual fun verifyPassword(password: String): Boolean {
        return try {
            val f = file()
            if (!f.exists()) return false
            val raw = ShieldStorageKey.decryptWithMaster(f.readBytes()) ?: return false
            if (raw.size != SALT_LEN + KEY_LEN) return false
            val salt = raw.copyOfRange(0, SALT_LEN)
            val expected = raw.copyOfRange(SALT_LEN, SALT_LEN + KEY_LEN)
            val actual = derive(password, salt)
            MessageDigest.isEqual(expected, actual) // 常数时间比较
        } catch (e: Exception) {
            false
        }
    }

    actual fun clearPassword() {
        try {
            val f = file()
            if (f.exists()) {
                // 覆写后删除（与 SecureWipe 同语义，防取证恢复）
                com.syna.util.SecureWipe.wipeFile(f.absolutePath)
            }
        } catch (e: Exception) {
        }
    }
}
