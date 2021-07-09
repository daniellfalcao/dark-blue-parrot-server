package com.github.daniellfalcao.parrot.database.dao

import com.github.daniellfalcao.parrot.database.exception.PostNotFoundException
import com.github.daniellfalcao.parrot.database.extension.toObjectId
import com.google.protobuf.Timestamp
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.client.result.DeleteResult
import com.proto.parrot.service.post.Author
import com.proto.parrot.service.post.Post
import org.bson.BsonArray
import org.bson.Document
import org.bson.types.ObjectId
import java.time.Instant
import kotlin.concurrent.thread


class PostDAO(database: MongoDatabase) {

    companion object {
        // collection
        const val COLLECTION_POST = "post"

        // fields
        const val FIELD_ID = "_id"
        const val FIELD_MESSAGE = "message"
        const val FIELD_AUTHOR_ID = "author_id"
        const val FIELD_LIKES = "likes"
        const val FIELD_CREATED_AT = "created_at"
        const val FIELD_UPDATED_AT = "updated_at"

        // paging limits
        const val POST_LIST_LIMIT = 10
        const val POST_LIST_USER_LIMIT = 10
    }

    private val postCollection = database.getCollection(COLLECTION_POST)
    private val userCollection = database.getCollection(UserDAO.COLLECTION_USER)

    fun createPost(currentUserId: String, message: String): Post {
        val currentUserObjectId = ObjectId(currentUserId)
        val currentTime = Instant.now().toEpochMilli()
        return Document().apply {
            append(FIELD_AUTHOR_ID, currentUserObjectId)
            append(FIELD_MESSAGE, message)
            append(FIELD_LIKES, BsonArray(listOf()))
            append(FIELD_CREATED_AT, currentTime)
            append(FIELD_UPDATED_AT, currentTime)
        }.also {
            postCollection.insertOne(it)
        }.toPost(ObjectId(currentUserId))
    }

    @Throws(PostNotFoundException::class)
    fun getPost(postId: String, currentUserId: String): Post {
        return try {
            postCollection.find(eq(FIELD_ID, postId.toObjectId())).first()?.toPost(ObjectId(currentUserId))!!
        } catch (e: Exception) {
            throw PostNotFoundException()
        }
    }

    // TODO: Use mongo.collection.watch()
    @Throws(PostNotFoundException::class)
    fun observePost(postId: String, currentUserId: String, onChange: (Post) -> Unit) {
        thread {
            try {
                var lastPost = getPost(postId, currentUserId)
                onChange(lastPost)
                while (true) {
                    Thread.sleep(1000)
                    getPost(postId, currentUserId).also {
                        if (lastPost.message != it.message || lastPost.likes != it.likes) {
                            onChange(it)
                            lastPost = it
                        }
                    }
                }
            } catch (e: Exception) {
                throw PostNotFoundException()
            }
        }
    }

    @Throws(PostNotFoundException::class)
    fun editPost(postId: String, message: String) {
        try {
            val filter = eq(FIELD_ID, postId.toObjectId())
            val updates = Updates.combine(
                Updates.set(FIELD_MESSAGE, message),
                Updates.set(FIELD_UPDATED_AT, Instant.now().toEpochMilli()),
            )
            postCollection.findOneAndUpdate(filter, updates)
        } catch (e: Exception) {
            throw PostNotFoundException()
        }
    }

    @Throws(PostNotFoundException::class)
    fun deletePost(postId: String) {
        val result: DeleteResult
        try {
            result = postCollection.deleteOne(eq(FIELD_ID, postId.toObjectId()))
        } catch (e: Exception) {
            throw PostNotFoundException()
        }
        if (result.deletedCount == 0L) {
            throw PostNotFoundException()
        }
    }

    @Throws(PostNotFoundException::class)
    fun swapLikeOfPost(postId: String, currentUserId: String) {
        try {
            val currentUserObjectId = currentUserId.toObjectId()
            val postObjectId = postId.toObjectId()
            val isLiked = postCollection.find(eq(FIELD_ID, postObjectId)).first()?.isLiked(currentUserObjectId)
            val filter = eq(FIELD_ID, postObjectId)
            val updates = if (isLiked == true) {
                Updates.pull(FIELD_LIKES, currentUserObjectId)
            } else {
                Updates.push(FIELD_LIKES, currentUserObjectId)
            }
            postCollection.findOneAndUpdate(filter, updates)
        } catch (e: Exception) {
            throw PostNotFoundException()
        }
    }

    @Throws(PostNotFoundException::class)
    fun getPosts(lastPostId: String, currentUserId: String): List<Post> {
        return try {
            val currentUserObjectId = ObjectId(currentUserId)
            val queryResult = if (lastPostId.isBlank()) {
                val post = postCollection.find().sort(Sorts.descending(FIELD_ID)).limit(1).first()!!
                postCollection.find(lte(FIELD_ID, post.getObjectId(FIELD_ID)))
                    .limit(POST_LIST_LIMIT).sort(Sorts.descending(FIELD_ID))
            } else {
                val postObjectId = lastPostId.toObjectId()
                postCollection.find(lt(FIELD_ID, postObjectId)).limit(POST_LIST_LIMIT).sort(Sorts.descending(FIELD_ID))
            }
            queryResult.toList().map {
                it.toPost(currentUserObjectId)
            }
        } catch (e: Exception) {
            throw PostNotFoundException()
        }
    }

    @Throws(PostNotFoundException::class)
    fun getPostOfUser(lastPostId: String, currentUserId: String): List<Post> {
        return try {
            val currentUserObjectId = currentUserId.toObjectId()
            val queryResult = if (lastPostId.isBlank()) {
                val post = postCollection.find(eq(FIELD_AUTHOR_ID, currentUserObjectId))
                    .sort(Sorts.descending(FIELD_ID)).limit(1).first()!!
                postCollection.find(
                    and(
                        lte(FIELD_ID, post.getObjectId(FIELD_ID)),
                        eq(FIELD_AUTHOR_ID, currentUserObjectId)
                    )
                ).limit(POST_LIST_LIMIT).sort(Sorts.descending(FIELD_ID))
            } else {
                val postObjectId = ObjectId(lastPostId)
                postCollection.find(
                    and(
                        lt(FIELD_ID, postObjectId),
                        eq(FIELD_AUTHOR_ID, currentUserObjectId)
                    )
                ).limit(POST_LIST_USER_LIMIT).sort(Sorts.descending(FIELD_ID))
            }
            queryResult.toList().map {
                it.toPost(currentUserObjectId)
            }
        } catch (e: Exception) {
            throw PostNotFoundException()
        }
    }

    // region extensions

    private fun Document.toPost(currentUserObjectId: ObjectId): Post {
        val author = userCollection.find(eq(UserDAO.FIELD_ID, getObjectId(FIELD_AUTHOR_ID))).first()?.toAuthor()
        val isLiked = isLiked(currentUserObjectId)
        val createdAt = Instant.ofEpochMilli(getLong(FIELD_CREATED_AT))
        val updatedAt = Instant.ofEpochMilli(getLong(FIELD_UPDATED_AT))
        return Post.newBuilder()
            .setId(getObjectId(FIELD_ID).toString())
            .setAuthor(author)
            .setMessage(getString(FIELD_MESSAGE))
            .setLike(isLiked)
            .setLikes(getList(FIELD_LIKES, ObjectId::class.java, listOf()).size)
            .setCreatedAt(
                Timestamp.newBuilder()
                    .setSeconds(createdAt.epochSecond)
                    .setNanos(createdAt.nano)
                    .build()
            ).setUpdatedAt(
                Timestamp.newBuilder()
                    .setSeconds(updatedAt.epochSecond)
                    .setNanos(updatedAt.nano)
                    .build()
            ).build()
    }

    private fun Document.toAuthor() = Author.newBuilder()
        .setId(getObjectId(UserDAO.FIELD_ID).toString())
        .setUsername(getString(UserDAO.FIELD_USERNAME))
        .setParrot(getString(UserDAO.FIELD_PARROT))
        .build()

    private fun Document.isLiked(currentUserObjectId: ObjectId): Boolean {
        return getList(FIELD_LIKES, ObjectId::class.java, listOf()).find { it == currentUserObjectId } != null
    }

    // endregion

}