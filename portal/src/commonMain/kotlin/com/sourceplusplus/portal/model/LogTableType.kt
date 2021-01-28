package com.sourceplusplus.portal.model

/**
 * todo: description.
 *
 * @since 0.1.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class LogTableType(
    override val isCentered: Boolean
) : TableType {
    OPERATION(false),
    PATTERN(false),
    OCCURRED(true);

    override val description = name.toLowerCase().capitalize()
}
