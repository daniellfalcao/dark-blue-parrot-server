package com.github.daniellfalcao.parrot.database.extension

import org.bson.types.ObjectId

fun String.toObjectId() = ObjectId(this)