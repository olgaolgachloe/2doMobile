package com.ynk.todolist.Fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ynk.todolist.Adapters.AdapterTodoList;
import com.ynk.todolist.Database.DAO;
import com.ynk.todolist.Model.TodoList;
import com.ynk.todolist.Model.User;
import com.ynk.todolist.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import muyan.snacktoa.SnackToa;

public class FragmentTodoList extends Fragment {

    //Database
    private DAO dao;

    private SimpleDateFormat sdf;
    private String lastSearch = "";
    private long mLastClickTime = 0;
    private User user;

    private TextView tvCompletedTask;

    private View llEmptyBox;
    private List<TodoList> todoLists;
    private AdapterTodoList adapterTodoList;

    //Bottom Sheet Dialog for Share Module
    private BottomSheetBehavior mBehavior;
    private BottomSheetDialog mBottomSheetDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todolist, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        toolbar.setTitle(getString(R.string.todoListPageTitle, user.getUserNameSurname()));
        toolbar.setSubtitle(getString(R.string.todoListPageSubTitle));
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        tvCompletedTask = view.findViewById(R.id.tvCompletedTask);

        View bottomSheet = view.findViewById(R.id.bottomSheet);
        llEmptyBox = view.findViewById(R.id.llEmptyBox);

        //Components
        RecyclerView recyclerViewTodoList = view.findViewById(R.id.recyclerViewTodoList);
        recyclerViewTodoList.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerViewTodoList.setHasFixedSize(true);
        recyclerViewTodoList.setAdapter(adapterTodoList);

        mBehavior = BottomSheetBehavior.from(bottomSheet);

        getTodoLists();

        FloatingActionButton floatingActionButton = view.findViewById(R.id.fabNewList);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddListDialog(null);
            }
        });

        return view;
    }

    private void getTodoLists() {
        todoLists.clear();
        //searchedLists.clear();
        List<TodoList> todoLists1 = dao.getTodolist(String.valueOf(user.getUserId()));
        if (todoLists1.isEmpty()) {
            llEmptyBox.setVisibility(View.VISIBLE);
        } else {
            llEmptyBox.setVisibility(View.GONE);
            todoLists.addAll(dao.getTodolist(String.valueOf(user.getUserId())));
            //searchedLists.addAll(todoLists);
            adapterTodoList.notifyDataSetChanged();
            //int completedCount = dao.getTaskCount(user.getUserId(), "1", sdf.format(new Date()));
            //tvCompletedTask.setText(String.valueOf(completedCount));
        }
    }

    private void showAddListDialog(final TodoList todoList) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_new_list);
        dialog.setCancelable(true);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        final EditText etListName = dialog.findViewById(R.id.etListName);
        final EditText etPriority = dialog.findViewById(R.id.etPriority);
        final TextView tvHeader = dialog.findViewById(R.id.tvHeader);
        final Button buttonSave = dialog.findViewById(R.id.buttonSave);

        final String[] priority = getResources().getStringArray(R.array.listPriority);
        if (todoList != null) {
            etListName.setText(todoList.getListName());
            etPriority.setText(priority[todoList.getListPriority()]);
            tvHeader.setText(getString(R.string.todoListDialogHeaderUpdate));
            buttonSave.setText(getString(R.string.todoListItemDialogSubmitUpdate));
        } else {
            etListName.getText().clear();
            etPriority.setText(priority[0]);
            tvHeader.setText(getString(R.string.todoListDialogHeaderCreate));
            buttonSave.setText(getString(R.string.todoListItemDialogSubmitNew));
        }

        etPriority.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPriorityDialog(priority, v);
            }
        });

        dialog.findViewById(R.id.buttonClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.buttonSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(etListName.getText().toString().trim())) {
                    etListName.setError(getString(R.string.todolistAddListNameError));
                    return;
                }
                String message = "";
                TodoList todoList1;
                if (todoList != null) {
                    todoList1 = todoList;
                    message = getString(R.string.todoListUpdateMessage);
                } else {
                    todoList1 = new TodoList();
                    message = getString(R.string.todoListCreateMessage);
                }
                todoList1.setUserId(String.valueOf(user.getUserId()));
                todoList1.setListName(etListName.getText().toString());
                todoList1.setListAddDate(new Date());
                todoList1.setListPriority(getPriorityIndex(priority, etPriority.getText().toString()));
                dao.insertTodoList(todoList1);
                dialog.dismiss();
                getTodoLists();
                SnackToa.snackBarSuccess(getActivity(), message);
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private int getPriorityIndex(final String[] array, String selected) {
        int index = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(selected)) index = i;
        }
        return index;
    }

    private void showPriorityDialog(final String[] array, final View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.todoListPriority));
        builder.setSingleChoiceItems(array, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ((EditText) v).setText(array[i]);
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

}
