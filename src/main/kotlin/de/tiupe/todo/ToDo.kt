package de.tiupe.todo

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt


/*
  Das Routing geht hier noch ein bisschen durcheinander, funktioniert aber.
  Man kann löschen, hinzufügen, nach einer id Fragen und wenn es diese nicht gibt, wird
  ein 404 zurückgegeben.
* */
fun Application.configureToDoRouting() {
    routing {
        route("/todos") {
            authenticate("ktorhugs-basicauth") {
                get("/{id}") {
                    val id = call.parameters["id"]?.toInt() ?: -1
                    ToDos.getToDoOrNull(id)?.also {
                        call.respond(it)
                    } ?: call.respond(HttpStatusCode.NotFound, "Ein Todo mit dieser ID ist nicht vorhanden")
                }
            }

        }
            authenticate("ktorhugs-digest") {
            get<ToDos> { mytodos: ToDos ->
                call.respond(ToDos.toDos)
               }
            }
        get("/todosbypriority") {
            val qp: Parameters = call.request.queryParameters
            val ascending: String = qp["ascending"] ?: ""
            if (ascending == "true")
                call.respond(ToDos.getByPriority(true))
            else
                call.respond(ToDos.getByPriority())
        }

        post<ToDo>("/") {
            val toDoToAdd: ToDo = call.receive<ToDo>()
            val idOfSetToDo = ToDos.setToDo(toDoToAdd)
            call.respond(ToDos.getToDo(idOfSetToDo))
        }

        authenticate("ktorhugs-jwt") {
            delete("/{id}") {
                val idToDelete = call.parameters["id"]?.toInt() ?: -1
                call.respond(ToDos.deleteToDo(idToDelete))
            }
        }
    }
}

@Suppress("unused")
enum class Priority(val level: Int) {
    HIGH(10),
    NORMAL(5),
    LOW(1)
}

@Serializable
data class ToDo(val id: Int = 0, val txt: String, val priority: Priority = Priority.NORMAL)

@Serializable
@Resource("/todos")
object ToDos {
    val toDos: MutableSet<ToDo> = mutableSetOf(ToDo(1, "Füße baumeln lassen"))

    fun getToDo(id: Int): ToDo = toDos.first { it.id == id }

    fun getToDoOrNull(id: Int): ToDo? {
        return try {
            getToDo(id)
        } catch (nseex: java.util.NoSuchElementException) {
            null
        }
    }


    fun setToDo(toDo: ToDo): Int {
        val toDoToSet = if (toDo.id == 0)
            toDo.copy(id = nextId())
        else toDo
        toDos.add(toDoToSet)
        return toDoToSet.id
    }

    fun deleteToDo(id: Int): Set<ToDo> {
        toDos.remove(getToDo(id))
        return toDos
    }

    private fun nextId(): Int {
        return toDos.maxBy {
            it.id
        }.id + 1
    }

    fun getByPriority(ascending: Boolean = false): Set<ToDo> {
        return if (ascending)
            toDos.sortedBy { it.priority.level }.toSet()
        else
            toDos.sortedByDescending { it.priority.level }.toSet()
    }

    fun getHashForPW(pw: String) {
        val hashedPW = BCrypt.hashpw(pw, BCrypt.gensalt())
        println("$pw: Hash: $hashedPW")
    }
}