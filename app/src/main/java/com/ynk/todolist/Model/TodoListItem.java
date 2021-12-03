package com.ynk.todolist.Model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "todolistitem")
public class TodoListItem {

    @PrimaryKey
    private Long listItemId;

    @ColumnInfo
    private String listId;

    public Long getListItemId() {
        return listItemId;
    }

    public void setListItemId(Long listItemId) {
        this.listItemId = listItemId;
    }

    @ColumnInfo
    private Date listItemDeadline;

    @ColumnInfo
    private int listItemStatusCode;

}
