// SPDX-License-Identifier: GPL-3.0-only
// Syna — LAN instant messenger, Copyright (C) 2026 Verlintas
// Licensed under the GNU General Public License v3.0: https://www.gnu.org/licenses/

rootProject.name = "Syna-NUSV"

val isCI = System.getenv("CI") != null

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        if (!isCI) {
            // Aliyun mirrors speed up builds in China but are unreliable on
            // GitHub-hosted runners, so they are only used outside CI.
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/central")
            maven("https://maven.aliyun.com/repository/gradle-plugin")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        if (!isCI) {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/central")
            maven("https://maven.aliyun.com/repository/gradle-plugin")
        }
        google()
        mavenCentral()
    }
}

include(":composeApp")
