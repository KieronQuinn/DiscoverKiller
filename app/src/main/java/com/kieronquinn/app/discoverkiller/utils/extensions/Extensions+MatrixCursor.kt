package com.kieronquinn.app.discoverkiller.utils.extensions

import android.database.MatrixCursor

fun Map<String, String>.toMatrixCursor(
    vararg columnNames: String = arrayOf("key", "value")
): MatrixCursor {
    return MatrixCursor(columnNames).apply {
        forEach {
            addRow(arrayOf(it.key, it.value))
        }
    }
}