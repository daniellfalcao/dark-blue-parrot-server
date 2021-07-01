package com.github.daniellfalcao.parrot.server

import com.github.daniellfalcao.parrot.database.dao.PostDAO
import com.github.daniellfalcao.parrot.database.dao.UserDAO
import com.github.daniellfalcao.parrot.server.interceptor.AuthorizationInterceptor
import com.github.daniellfalcao.parrot.server.services.AuthenticationServiceImpl
import com.github.daniellfalcao.parrot.server.services.PostServiceImpl
import com.github.daniellfalcao.parrot.server.services.RegisterServiceImpl
import com.mongodb.client.MongoClients
import io.github.cdimascio.dotenv.dotenv
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.ServerBuilder
import java.io.IOException

fun main() = ParrotServer().run()

class ParrotServer {

    // mongo instance
    private val mongoClient = MongoClients.create(Constants.MONGO_CLIENT_URL)
    private val mongoDatabase = mongoClient.getDatabase(Constants.MONGO_DATABASE_NAME)

    // DAO's
    private val userDAO = UserDAO(mongoDatabase)
    private val postDAO = PostDAO(mongoDatabase)

    @Throws(IOException::class, InterruptedException::class)
    fun run() {
        ServerBuilder.forPort(50051).apply {
            println("Building server...")
            addService(AuthenticationServiceImpl(userDAO))
            addService(RegisterServiceImpl(userDAO))
            addService(PostServiceImpl(postDAO))
            intercept(AuthorizationInterceptor())
        }.build().apply {
            println("Server has been built.")
            println("Starting server...")
        }.start().apply {
            println("Server started!")
            Runtime.getRuntime().addShutdownHook(Thread(({
                println("Receive shutdown request!")
                shutdown()
                println("Server has been stopped successfully.")
            })))
        }.awaitTermination()
    }

    object ContextMetadata {
        val TOKEN_METADATA_KEY: Metadata.Key<String> = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER)
        val USER_ID_CONTEXT_KEY: Context.Key<String> = Context.key("user_id")
    }

    object Constants {

        private var envs = dotenv()

        // env vars name
        private const val ENV_VAR_JWT_SECRET = "JWT_SECRET"
        private const val ENV_VAR_MONGO_CLIENT_URL = "MONGO_CLIENT_URL"
        private const val ENV_VAR_MONGO_DATABASE_NAME = "MONGO_DATABASE_NAME"

        // env vars value
        val JWT_SECRET: String = envs[ENV_VAR_JWT_SECRET]
        val MONGO_CLIENT_URL: String = envs[ENV_VAR_MONGO_CLIENT_URL]
        val MONGO_DATABASE_NAME: String = envs[ENV_VAR_MONGO_DATABASE_NAME]

    }

}


