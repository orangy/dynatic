package org.jetbrains.dynatic.test


interface Strings {
    var name: String
}

interface Numbers {
    val count: Int
    var size: Long
    val percent: Double
}

interface Logic {
    var yesno: Boolean
}

interface Dates {
    var created: String
}

interface Cascade {
    val numbers: Numbers
    val dates: Dates
}
