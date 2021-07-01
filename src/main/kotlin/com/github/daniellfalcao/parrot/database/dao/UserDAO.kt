package com.github.daniellfalcao.parrot.database.dao

import com.github.daniellfalcao.parrot.database.exception.UserAlreadyExistsException
import com.github.daniellfalcao.parrot.database.exception.UserNotFoundException
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.proto.parrot.service.authentication.User
import org.bson.Document

class UserDAO(database: MongoDatabase) {

    companion object {
        // collection
        const val COLLECTION_USER = "user"

        // fields
        const val FIELD_ID = "_id"
        const val FIELD_USERNAME = "username"
        const val FIELD_BIRTHDAY = "birthday"
        const val FIELD_PASSWORD = "password"
    }

    private val userCollection = database.getCollection(COLLECTION_USER)

    fun isUsernameAvailable(username: String): Boolean {
        return userCollection.find(eq(FIELD_USERNAME, username)).first() == null
    }

    // TODO: encrypt password before save in mongo
    @Throws(UserAlreadyExistsException::class)
    fun createUser(username: String, password: String, birthday: String): User {
        userCollection.find(eq(FIELD_USERNAME, username)).first()?.let {
            throw UserAlreadyExistsException()
        }
        return Document().apply {
            append(FIELD_USERNAME, username)
            append(FIELD_BIRTHDAY, birthday)
            append(FIELD_PASSWORD, password)
        }.also {
            userCollection.insertOne(it)
        }.toUser()
    }

    @Throws(UserNotFoundException::class)
    fun getUserByUsername(username: String): User {
        return try {
            userCollection.find(eq(FIELD_USERNAME, username)).first()?.toUser()!!
        } catch (e: Exception) {
            throw UserNotFoundException()
        }
    }

    @Throws(UserNotFoundException::class)
    fun getUserById(id: String): User {
        return try {
            userCollection.find(eq(FIELD_ID, id)).first()?.toUser()!!
        } catch (e: Exception) {
            throw UserNotFoundException()
        }
    }

    @Throws(UserNotFoundException::class)
    fun readUser(username: String, password: String): User {
        return userCollection.find(
            and(
                eq(FIELD_USERNAME, username),
                eq(FIELD_PASSWORD, password)
            )
        ).first()?.toUser() ?: run {
            throw UserNotFoundException()
        }
    }

    // region extensions

    private fun Document.toUser() = User.newBuilder()
        .setId(getObjectId(FIELD_ID).toString())
        .setUsername(getString(FIELD_USERNAME))
        .setBirthday(getString(FIELD_BIRTHDAY))
        .build()

    // endregion

}