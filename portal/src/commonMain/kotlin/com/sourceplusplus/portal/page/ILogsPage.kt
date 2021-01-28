package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.IPortalPage
import com.sourceplusplus.protocol.portal.PortalConfiguration

/**
 * todo: description.
 *
 * @since 0.1.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class ILogsPage : IPortalPage() {
    override lateinit var configuration: PortalConfiguration
}
