package com.github.daniellfalcao.parrot.server.services

import com.github.daniellfalcao.parrot.database.dao.UserDAO
import com.github.daniellfalcao.parrot.database.exception.InvalidUsernameOrPasswordException
import com.github.daniellfalcao.parrot.database.exception.UserNotFoundException
import com.github.daniellfalcao.parrot.server.authorization.Authorization
import com.github.daniellfalcao.parrot.server.exception.UnexpectedErrorException
import com.google.protobuf.Empty
import com.proto.parrot.service.authentication.AuthenticationServiceGrpc
import com.proto.parrot.service.authentication.SignInRequest
import com.proto.parrot.service.authentication.SignInResponse
import io.grpc.stub.StreamObserver

class AuthenticationServiceImpl(
    private val userDAO: UserDAO
) : AuthenticationServiceGrpc.AuthenticationServiceImplBase() {

    override fun signIn(
        request: SignInRequest,
        responseObserver: StreamObserver<SignInResponse>
    ) {
        try {
            val user = userDAO.readUser(request.username, request.password)
            val response = SignInResponse.newBuilder()
                .setToken(Authorization.encode(user.id))
                .setUser(user)
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (error: UserNotFoundException) {
            responseObserver.onError(InvalidUsernameOrPasswordException())
        } catch (error: Exception) {
            responseObserver.onError(UnexpectedErrorException(error))
        }
    }

    // TODO: invalidate token?
    override fun signOut(
        request: Empty,
        responseObserver: StreamObserver<Empty>
    ) {
        responseObserver.onNext(Empty.newBuilder().build())
        responseObserver.onCompleted()
    }
}