/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.settings

/**
 * SourceMarker plugin configuration.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class SourceMarkerConfig(
    var rootSourcePackages: List<String> = emptyList(),
    var autoResolveEndpointNames: Boolean = false,
    var localMentorEnabled: Boolean = true,
    var pluginConsoleEnabled: Boolean = false,
    var serviceHost: String? = null,
    var accessToken: String? = null,
    var certificatePins: List<String> = emptyList(),
    var serviceToken: String? = null,
    var verifyHost: Boolean = true,
    val serviceName: String? = null
)

val SourceMarkerConfig.serviceHostNormalized: String?
    get() {
        if (serviceHost == null) return null
        var serviceHost = serviceHost!!
            .substringAfter("https://").substringAfter("http://")
        if (serviceHost.contains(":")) {
            serviceHost = serviceHost.split(":")[0]
                .substringAfter("https://").substringAfter("http://")
        }
        return serviceHost
    }

fun SourceMarkerConfig.getServicePortNormalized(defaultServicePort: Int?): Int? {
    if (serviceHost == null) return null
    val hostStr = serviceHost!!.substringAfter("https://").substringAfter("http://")
    if (hostStr.contains(":")) {
        return hostStr.split(":")[1].toInt()
    }
    return defaultServicePort
}

fun SourceMarkerConfig.isSsl(): Boolean {
    return getServicePortNormalized(null) == 443 || serviceHost?.startsWith("https://") == true
}
