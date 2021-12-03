package com.ynk.todolist.Database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.ynk.todolist.Model.TodoList;
import com.ynk.todolist.Model.User;

import java.util.List;

@Dao
public interface DAO {

    /* table notification transaction ----------------------------------------------------------- */

    //Insert Querys
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    //Select Querys
    @Query("SELECT * FROM user WHERE userName = :userName AND userPassword = :password")
    User login(String userName, String password);

    @Query("SELECT * FROM user WHERE userName = :userName")
    User loginControl(String userName);

    @Query("SELECT COUNT(*) FROM user WHERE userName = :userName OR userMail = :userMail")
    Integer signUpControl(String userName, String userMail);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTodoList(TodoList todoList);

    //Delete Querys
    @Query("DELETE FROM todolist WHERE listId = :listId")
    void deleteTodoList(Long listId);

    @Query("DELETE FROM todolistitem WHERE listId = :listId")
    void deleteTodoListItemsByListId(Long listId);

    @Query("SELECT todolist.* FROM todolist WHERE userId = :userId")
    List<TodoList> getTodolist(String userId);
}
