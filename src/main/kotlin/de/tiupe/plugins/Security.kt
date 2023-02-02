package de.tiupe.plugins

import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.server.sessions.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.*

fun getMd5Digest(str: String): ByteArray = MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8))

val myRealm = "/"
val userTable: Map<String, ByteArray> = mapOf(
    "peter" to getMd5Digest("peter:$myRealm:pw1"),
    "tina" to getMd5Digest("tina:$myRealm:pw2")
)

fun Application.configureSecurity() {

    authentication {
        jwt("ktorhugs-jwt") {
            // Die Einträge hier stehen einzeln in der Konfigurationsdatei in dem Resource-Verzeichnis
            val jwtAudience = this@configureSecurity.environment.config.property("jwt.audience").getString()
            realm = this@configureSecurity.environment.config.property("jwt.realm").getString()
            val secret = this@configureSecurity.environment.config.property("jwt.secret").getString()
            // Hier wird das JW-Token verifiziert, d.h. die Signatur geprüft etc.
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(jwtAudience)
                    .withIssuer(this@configureSecurity.environment.config.property("jwt.issuer").getString())
                    .build()
            )

            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
            // Wenn das Token nicht validiert werden kann, wird diese Meldung geschickt
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
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
        digest("ktorhugs-diget") {
            realm = myRealm
            digestProvider { userName, realm ->
                // println(userName)
                userTable[userName]
            }
            validate { credentials ->
                if (credentials.userName.isNotEmpty()) {
                    // println(credentials.realm)
                    CustomPrincipal(credentials.userName, credentials.realm)
                } else {
                    null
                }
            }

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
    // post("/login") defines an authentication route for receiving POST requests.
    // for jwt-Login
    routing {
        post("/login") {
            val jwtUser = call.receive<JWTUser>()
            // Die Prüfung des Users muss hier passieren.
            // Sie ist hier mit Username und Passwort bewusst einfach gehalten. Es geht nur um
            // ein Beispiel. Man könnte hier auch eine verschachtelte Anmeldung wählen....
            val token = if(jwtUser.password == "geheim") {
             JWT.create()
                .withAudience(this@configureSecurity.environment.config.property("jwt.audience").getString())
                .withIssuer(this@configureSecurity.environment.config.property("jwt.issuer").getString())
                .withClaim("username", jwtUser.username)
                .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // eine Stunde...
                .sign(Algorithm.HMAC256(this@configureSecurity.environment.config.property("jwt.secret").getString()))
            } else {
                "" // to avoid null-pointer-exceptions
            }
            call.respond(hashMapOf("token" to token))
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

data class CustomPrincipal(val userName: String, val realm: String) : Principal

// User-Klasse für den Login-Prozess beim JWT-Erstellen
@Serializable
data class JWTUser(val username: String, val password: String)

