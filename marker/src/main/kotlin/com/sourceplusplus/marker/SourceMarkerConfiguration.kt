package com.sourceplusplus.marker

import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.SourceFileMarkerProvider
import com.sourceplusplus.marker.source.mark.api.filter.CreateSourceMarkFilter
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkConfiguration

/**
 * Used to configure [SourceFileMarker]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerConfiguration {
    var createSourceMarkFilter: CreateSourceMarkFilter = CreateSourceMarkFilter.ALL
    var sourceFileMarkerProvider: SourceFileMarkerProvider = object : SourceFileMarkerProvider {}
    var gutterMarkConfiguration: GutterMarkConfiguration = GutterMarkConfiguration()
    var inlayMarkConfiguration: InlayMarkConfiguration = InlayMarkConfiguration()
}
