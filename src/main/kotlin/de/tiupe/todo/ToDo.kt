package de.tiupe.todo

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Application.configureToDoRouting(){
    routing {
        get<ToDos> { mytodos: ToDos ->
            call.respond(ToDos.toDos)
        }
        get("/todosbypriority"){
            val qp: Parameters = call.request.queryParameters
            val ascending: String = qp["ascending"] ?: ""
            if(ascending=="true")
                call.respond(ToDos.getByPriority(true))
            else
                call.respond(ToDos.getByPriority())
        }
        post<ToDo>("/"){
            val toDoToAdd: ToDo = call.receive<ToDo>()
            val idOfSetToDo = ToDos.setToDo(toDoToAdd)
            call.respond(ToDos.getToDo(idOfSetToDo))
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
object ToDos{
    val toDos: MutableSet<ToDo> = mutableSetOf(ToDo(1, "Füße baumeln lassen"))

    fun getToDo(id: Int) = toDos.first { it.id == id }

    fun setToDo(toDo: ToDo): Int {
        val toDoToSet = if(toDo.id == 0)
            toDo.copy(id= nextId())
        else toDo
        toDos.add(toDoToSet)
        return toDoToSet.id
    }
    private fun nextId(): Int {
        return toDos.maxBy {
            it.id
        }.id + 1
    }

    fun getByPriority(ascending: Boolean = false) : Set<ToDo> {
        return if(ascending)
            toDos.sortedBy { it.priority.level }.toSet()
        else
            toDos.sortedByDescending { it.priority.level }.toSet()
    }
}