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
        oauth("ktorhugs-oauth-google") {
            // Gibt die Rücksprung-Adresse vom OAuth-Server an, wenn die Authentifizierung
            // erfolgt ist. Diese Adresse muss in der Regel beim Auth-Provider als valide
            // Rücksprung-Adresse eingetragen sein, sonst erfolgt der Aufruf dorthin nicht.
            urlProvider = { "http://localhost:8081/callback" }

            // Insgesamt wird in dieser Sektion, also im Provider-Lookup festgelegt,
            // wie mit dem Authentication-Provider zu kommunizieren ist.
            // Wenn man über die Klasse fährt, sieht man sehr gut, welche Parameter
            // eingestellt werden können.
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    // Gegen diese Url findet die Authorisierung statt
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    // von dieser Url wird das Access-Token bezogen, in dem steht,
                    // mit welchen Rechten der User auf die Resourcen des "Drittanbieters"
                    // zugreifen darf.
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",

                    requestMethod = HttpMethod.Post,
                    // Mit der Client-ID werden die Daten von Google geholt, leider war die
                    // Einrichtung nicht so trivial wie gehofft.
                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),

                    // Zugriff auf die Google-Api, um das Profil des Nutzers zu laden
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile")
                )
            }
            // mit dem hier eingetragenen Client werden die Anfragen an den
            // OAuth-Server durchgeführt. Hier ein Apache-HTTP-Client
            client = HttpClient(Apache)
        }
    }
    authentication {
        digest("ktorhugs-digest") {
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
        authenticate("ktorhugs-oauth-google") {
            // Dies ist die URL mit der man sich ds AUTH-Token von google holt
            // Es fehlt die GOOGLE-Client-ID für eine Anwendung...
            get("login") {
                // hier wird der Login-Prozess aufgerufen, da die URL mit dem
                // entsprechenden Authentifizierungsmechanismus abgesichert ist.
                // Anschließend wird einfach an die callback-Url weitergeleitet.
                // Das erklärt auch, warum in dem Callback eine
                // "localhost"-Adresse stehen darf.

                // Redirects automatisch zur 'authorizeUrl'

                call.respondRedirect("/callback")
            }

            get("/callback") {
                // hier landet der Callback von der Authentifizierung,
                // Mann kan hier die Eigenschaften des Tokens auslesen und sich
                // mit seiner eigenen Logik einklinken.
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                call.sessions.set(UserSession(principal?.accessToken.toString()))

                // auch hier wird nochmals ein Redirect gemacht, der wie immer über den
                // Browser läuft.
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

