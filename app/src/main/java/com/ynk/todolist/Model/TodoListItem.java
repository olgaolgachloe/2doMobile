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

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public Date getListItemDeadline() {
        return listItemDeadline;
    }

    public void setListItemDeadline(Date listItemDeadline) {
        this.listItemDeadline = listItemDeadline;
    }

    public int getListItemStatusCode() {
        return listItemStatusCode;
    }

    public void setListItemStatusCode(int listItemStatusCode) {
        this.listItemStatusCode = listItemStatusCode;
    }
}
