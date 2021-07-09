package com.github.daniellfalcao.parrot.server.services

import com.github.daniellfalcao.parrot.database.dao.UserDAO
import com.github.daniellfalcao.parrot.server.exception.UnexpectedErrorException
import com.google.protobuf.Empty
import com.proto.parrot.service.authentication.CheckUsernameAvailabilityRequest
import com.proto.parrot.service.authentication.CheckUsernameAvailabilityResponse
import com.proto.parrot.service.authentication.RegisterServiceGrpc
import com.proto.parrot.service.authentication.SignUpRequest
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver

class RegisterServiceImpl(private val userDAO: UserDAO) : RegisterServiceGrpc.RegisterServiceImplBase() {

    override fun signUp(
        request: SignUpRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        try {
            userDAO.createUser(request.username, request.password, request.birthday, request.parrot)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (error: StatusRuntimeException) {
            responseObserver.onError(error)
        } catch (error: Exception) {
            responseObserver.onError(UnexpectedErrorException(error))
        }
    }

    override fun checkUsernameAvailability(
        request: CheckUsernameAvailabilityRequest,
        responseObserver: StreamObserver<CheckUsernameAvailabilityResponse>
    ) {
        val response = CheckUsernameAvailabilityResponse.newBuilder()
            .setIsAvailable(userDAO.isUsernameAvailable(request.username))
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

}