package de.tiupe.plugins

import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.tiupe.todo.ToDos
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.server.sessions.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.mindrot.jbcrypt.BCrypt

fun Application.configureSecurity() {

    authentication {
        jwt {
            val jwtAudience = ""//this@configureSecurity.environment.config.property("jwt.audience").getString()
            realm = ""//this@configureSecurity.environment.config.property("jwt.realm").getString()
            verifier(
                JWT
                    .require(Algorithm.HMAC256("secret"))
                    .withAudience(jwtAudience)
                    .withIssuer("peterWars")//this@configureSecurity.environment.config.property("jwt.domain").getString())
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
    authentication {
        oauth("auth-oauth-google") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile")
                )
            }
            client = HttpClient(Apache)
        }
    }
    authentication {
        basic("ktorhugs-basicauth") {
            realm = ""
            validate { userPwdCredential ->

                if(USERS.containsKey(userPwdCredential.name) &&
                    BCrypt.checkpw(userPwdCredential.password, USERS[userPwdCredential.name])){
                    UserIdPrincipal(userPwdCredential.name)
                } else {
                    null
                }
            }
        }
    }
    routing {
        authenticate("auth-oauth-google") {
            get("login") {
                call.respondRedirect("/callback")
            }

            get("/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                call.sessions.set(UserSession(principal?.accessToken.toString()))
                call.respondRedirect("/hello")
            }
        }
    }
}

class UserSession(accessToken: String)

val USERS = mapOf<String, String>(
    "tina" to "\$2a\$10\$Op4jdOFZ5Zn.HXZQ2sLhD.IKXewfuE0GbPHR3DIkFlewpeIRyU9za",
    "peter" to "\$2a\$10\$eLSlcR.6Lw.AaP987gC8M.KUZ/bMNO3ol5AmU7VYL3ESgjcoyvE.2",
    "lara" to  "\$2a\$10\$MQH1hBRWpu7MPyuxSg4yVO.XwvqKQPjPE8OIm8Vcg1Ya5Bp14lz.2",
    "inken" to "\$2a\$10\$Nh1PpZwLySJWMityohOP2.rYke1u3oOd8N5wunYDM8k6lYs.Ljmvu"
)
