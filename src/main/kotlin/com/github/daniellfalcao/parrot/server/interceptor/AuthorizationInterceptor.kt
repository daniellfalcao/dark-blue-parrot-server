package com.github.daniellfalcao.parrot.server.interceptor

import com.github.daniellfalcao.parrot.server.ParrotServer
import com.github.daniellfalcao.parrot.server.authorization.Authorization
import com.proto.parrot.service.authentication.AuthenticationServiceGrpc
import com.proto.parrot.service.post.PostServiceGrpc
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCall.Listener
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusRuntimeException


class AuthorizationInterceptor : ServerInterceptor {

    override fun <ReqT, RespT> interceptCall(
        serverCall: ServerCall<ReqT, RespT>,
        metadata: Metadata,
        serverCallHandler: ServerCallHandler<ReqT, RespT>
    ): Listener<ReqT> {
        val needAuthorization = when (serverCall.methodDescriptor?.fullMethodName) {
            AuthenticationServiceGrpc.getSignOutMethod().fullMethodName -> true
            PostServiceGrpc.getGetPostMethod().fullMethodName -> true
            PostServiceGrpc.getGetPostStreamMethod().fullMethodName -> true
            PostServiceGrpc.getCreatePostMethod().fullMethodName -> true
            PostServiceGrpc.getDeletePostMethod().fullMethodName -> true
            PostServiceGrpc.getEditPostMethod().fullMethodName -> true
            PostServiceGrpc.getGetPostsMethod().fullMethodName -> true
            PostServiceGrpc.getGetMyPostsMethod().fullMethodName -> true
            PostServiceGrpc.getSwapLikePostMethod().fullMethodName -> true
            else -> false
        }
        val status: Status
        if (needAuthorization) {
            val token = metadata.get(ParrotServer.ContextMetadata.TOKEN_METADATA_KEY)
            status = if (token == null) {
                Status.UNAUTHENTICATED.withDescription("Authorization token is missing.")
            } else {
                try {
                    val subject = Authorization.decode(token)
                    val context = Context.current().withValue(ParrotServer.ContextMetadata.USER_ID_CONTEXT_KEY, subject)
                    return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler)
                } catch (error: Exception) {
                    if (error is StatusRuntimeException) {
                        Status.UNAUTHENTICATED.withDescription(error.status.description)
                    } else {
                        Status.UNAUTHENTICATED.withDescription("Authorization token is not valid.")
                    }
                }
            }
            serverCall.close(status, metadata)
            return object : ServerCall.Listener<ReqT>() {}
        } else {
            return serverCallHandler.startCall(serverCall, metadata)
        }
    }
}