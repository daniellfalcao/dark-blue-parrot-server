package com.github.daniellfalcao.parrot.server.services

import com.github.daniellfalcao.parrot.database.dao.PostDAO
import com.github.daniellfalcao.parrot.database.exception.DeletePostForbiddenOperationException
import com.github.daniellfalcao.parrot.database.exception.EditPostForbiddenOperationException
import com.github.daniellfalcao.parrot.server.ParrotServer
import com.github.daniellfalcao.parrot.server.exception.UnexpectedErrorException
import com.google.protobuf.Empty
import com.proto.parrot.service.post.CreatePostRequest
import com.proto.parrot.service.post.EditPostRequest
import com.proto.parrot.service.post.PostRequest
import com.proto.parrot.service.post.PostResponse
import com.proto.parrot.service.post.PostServiceGrpc
import com.proto.parrot.service.post.PostsRequest
import com.proto.parrot.service.post.PostsResponse
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver


class PostServiceImpl(private val postDAO: PostDAO) : PostServiceGrpc.PostServiceImplBase() {

    override fun createPost(
        request: CreatePostRequest,
        responseObserver: StreamObserver<PostResponse>
    ) {
        val userId = ParrotServer.ContextMetadata.USER_ID_CONTEXT_KEY.get()
        try {
            val post = postDAO.createPost(userId, request.message)
            val response = PostResponse.newBuilder()
                .setPost(post)
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (error: StatusRuntimeException) {
            responseObserver.onError(error)
        } catch (error: Exception) {
            responseObserver.onError(UnexpectedErrorException(error))
        }
    }

    override fun getPost(
        request: PostRequest,
        responseObserver: StreamObserver<PostResponse>
    ) {
        val userId = ParrotServer.ContextMetadata.USER_ID_CONTEXT_KEY.get()
        try {
            val post = postDAO.getPost(request.postId, userId)
            val response = PostResponse.newBuilder()
                .setPost(post)
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (error: StatusRuntimeException) {
            responseObserver.onError(error)
        } catch (error: Exception) {
            responseObserver.onError(UnexpectedErrorException(error))
        }
    }

    override fun getPostStream(
        request: PostRequest,
        responseObserver: StreamObserver<PostResponse>
    ) {
        val userId = ParrotServer.ContextMetadata.USER_ID_CONTEXT_KEY.get()
        try {
            postDAO.observePost(request.postId, userId) {
                val response = PostResponse.newBuilder()
                    .setPost(it)
                    .build()
                responseObserver.onNext(response)
            }
        } catch (error: StatusRuntimeException) {
            responseObserver.onError(error)
        } catch (error: Exception) {
            responseObserver.onError(UnexpectedErrorException(error))
        }
    }

    override fun getPosts(
        request: PostsRequest,
        responseObserver: StreamObserver<PostsResponse>
    ) {
        val userId = ParrotServer.ContextMetadata.USER_ID_CONTEXT_KEY.get()
        try {
            val posts = postDAO.getPosts(request.lastPostId, userId)
            val response = PostsResponse.newBuilder()
                .addAllPosts(posts)
                .setPostsMaxSize(PostDAO.POST_LIST_LIMIT)
                .setPostsSize(posts.size)
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            val response = PostsResponse.newBuilder()
                .addAllPosts(listOf())
                .setPostsMaxSize(PostDAO.POST_LIST_LIMIT)
                .setPostsSize(0)
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    override fun getMyPosts(
        request: PostsRequest,
        responseObserver: StreamObserver<PostsResponse>
    ) {
        val userId = ParrotServer.ContextMetadata.USER_ID_CONTEXT_KEY.get()
        try {
            val posts = postDAO.getPostOfUser(request.lastPostId, userId)
            val response = PostsResponse.newBuilder()
                .addAllPosts(posts)
                .setPostsMaxSize(PostDAO.POST_LIST_USER_LIMIT)
                .setPostsSize(posts.size)
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            val response = PostsResponse.newBuilder()
                .addAllPosts(listOf())
                .setPostsMaxSize(PostDAO.POST_LIST_USER_LIMIT)
                .setPostsSize(0)
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    override fun editPost(
        request: EditPostRequest,
        responseObserver: StreamObserver<PostResponse>
    ) {
        val userId = ParrotServer.ContextMetadata.USER_ID_CONTEXT_KEY.get()
        validateUserAccessToPost(request.postId, userId,
            onAccessGranted = {
                try {
                    postDAO.editPost(request.postId, request.message)
                    val post = postDAO.getPost(request.postId, userId)
                    val response = PostResponse.newBuilder()
                        .setPost(post)
                        .build()
                    responseObserver.onNext(response)
                    responseObserver.onCompleted()
                } catch (error: StatusRuntimeException) {
                    responseObserver.onError(error)
                } catch (error: Exception) {
                    responseObserver.onError(UnexpectedErrorException(error))
                }
            },
            onAccessDenied = {
                responseObserver.onError(EditPostForbiddenOperationException())
            },
            onCatch = {
                responseObserver.onError(it)
            }
        )
    }

    override fun deletePost(
        request: PostRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        val userId = ParrotServer.ContextMetadata.USER_ID_CONTEXT_KEY.get()
        validateUserAccessToPost(request.postId, userId,
            onAccessGranted = {
                try {
                    postDAO.deletePost(request.postId)
                    responseObserver.onNext(Empty.getDefaultInstance())
                    responseObserver.onCompleted()
                } catch (error: StatusRuntimeException) {
                    responseObserver.onError(error)
                } catch (error: Exception) {
                    responseObserver.onError(UnexpectedErrorException(error))
                }
            },
            onAccessDenied = {
                responseObserver.onError(DeletePostForbiddenOperationException())
            },
            onCatch = {
                responseObserver.onError(it)
            }
        )
    }

    override fun swapLikePost(
        request: PostRequest,
        responseObserver: StreamObserver<PostResponse>
    ) {
        val userId = ParrotServer.ContextMetadata.USER_ID_CONTEXT_KEY.get()
        try {
            postDAO.swapLikeOfPost(request.postId, userId)
            val post = postDAO.getPost(request.postId, userId)
            val response = PostResponse.newBuilder()
                .setPost(post)
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (error: StatusRuntimeException) {
            responseObserver.onError(error)
        } catch (error: Exception) {
            responseObserver.onError(UnexpectedErrorException(error))
        }
    }

    private fun validateUserAccessToPost(
        postId: String,
        userId: String,
        onAccessGranted: () -> Unit,
        onAccessDenied: () -> Unit,
        onCatch: (StatusRuntimeException) -> Unit
    ) {
        try {
            val post = postDAO.getPost(postId, userId)
            if (post.author.id == userId) {
                onAccessGranted()
            } else {
                onAccessDenied()
            }
        } catch (error: StatusRuntimeException) {
            onCatch(error)
        } catch (error: Exception) {
            onCatch(UnexpectedErrorException(error))
        }
    }

}