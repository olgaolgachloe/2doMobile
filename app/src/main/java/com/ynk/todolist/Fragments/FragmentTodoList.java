package com.ynk.todolist.Fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ynk.todolist.Adapters.AdapterTodoList;
import com.ynk.todolist.BuildConfig;
import com.ynk.todolist.Database.AppDatabase;
import com.ynk.todolist.Database.DAO;
import com.ynk.todolist.Listeners.RecyclerListClickListener;
import com.ynk.todolist.Model.TodoList;
import com.ynk.todolist.Model.User;
import com.ynk.todolist.R;
import com.ynk.todolist.Tools.Tools;
import com.ynk.todolist.Tools.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import muyan.snacktoa.SnackToa;

public class FragmentTodoList extends Fragment {

    //Database
    private DAO dao;

    //Multi Selection
    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;

    private SimpleDateFormat sdf;
    private String lastSearch = "";
    private long mLastClickTime = 0;
    private User user;

    private TextView tvCompletedTask;

    private View llEmptyBox;
    private List<TodoList> todoLists, searchedLists;
    private AdapterTodoList adapterTodoList;

//    Bottom Sheet Dialog for Share Module
//    private BottomSheetBehavior mBehavior;
//    private BottomSheetDialog mBottomSheetDialog;

    private SearchView.OnQueryTextListener searchListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            searchedLists.clear();
            adapterTodoList.notifyDataSetChanged();
            for (TodoList pp : todoLists) {
                if (pp.getListName().toUpperCase().contains(newText.toUpperCase(new Locale("tr")))) {
                    searchedLists.add(pp);
                }
            }
            adapterTodoList.notifyDataSetChanged();
            lastSearch = newText;
            return false;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            user = new Gson().fromJson(getArguments().getString("user"), User.class);
        }
        sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        dao = AppDatabase.getDb(getActivity()).getDAO();

        setHasOptionsMenu(true);
        todoLists = new ArrayList<>();
        searchedLists = new ArrayList<>();
        adapterTodoList = new AdapterTodoList(getActivity(), searchedLists, new RecyclerListClickListener() {
            @Override
            public void itemClick(View view, Object item, int position) {
                if (mLastClickTime - System.currentTimeMillis() > 2000) {
                    return;
                }
                mLastClickTime = System.currentTimeMillis();
                if (adapterTodoList.getSelectedItemCount() > 0) {
                    enableActionMode(position);
                } else {
                    TodoList todoList = (TodoList) item;
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    Bundle bundle = new Bundle();
                    bundle.putString("user", new Gson().toJson(user));
                    bundle.putLong("listId", todoList.getListId());
                    bundle.putString("listName", todoList.getListName());
                    FragmentTodoListItem fragment = new FragmentTodoListItem();
                    fragment.setArguments(bundle);
                    fragmentTransaction.replace(R.id.content, fragment)
                            .addToBackStack(Utils.todoListFragmentTag).commit();
                }
            }

            @Override
            public void longItemClick(View view, Object item, int position) {
                enableActionMode(position);
            }

            @Override
            public void moreItemClick(View view, Object item, int position, MenuItem menuItem) {
                TodoList todoList = (TodoList) item;
                switch (menuItem.getItemId()) {
                    case R.id.action_delete:
                        dao.deleteTodoListItemsByListId(todoList.getListId());
                        dao.deleteTodoList(todoList.getListId());
                        SnackToa.snackBarSuccess(getActivity(), getString(R.string.todoListDeleteMessage));
                        getTodoLists();
                        break;
                    case R.id.action_update:
                        showAddListDialog(todoList);
                        break;
                    case R.id.action_share:
                        List<TodoList> todoLists = new ArrayList<>();
                        todoLists.add(todoList);
                        showBottomSheetDialog(todoLists);
                        break;
                }
            }

        });
        actionModeCallback = new ActionModeCallback();
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
            searchedLists.addAll(todoLists);
            adapterTodoList.notifyDataSetChanged();
            int completedCount = dao.getTaskCount(user.getUserId(), "1", sdf.format(new Date()));
            tvCompletedTask.setText(String.valueOf(completedCount));
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

    private void enableActionMode(int position) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        adapterTodoList.toggleSelection(position);
        int count = adapterTodoList.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Tools.setSystemBarColor(getActivity(), R.color.blue_grey_700);
            mode.getMenuInflater().inflate(R.menu.menu_todolist, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                deleteSelectedListItems();
                mode.finish();
                return true;
            } else if (id == R.id.action_share) {
                showBottomSheetDialog(getSelectedItems());
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapterTodoList.clearSelections();
            actionMode = null;
            Tools.setSystemBarColor(getActivity(), R.color.colorAccentLight);
        }
    }

    private void deleteSelectedListItems() {
        List<Integer> selectedItemPositions = adapterTodoList.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            TodoList removedList = searchedLists.get(selectedItemPositions.get(i));
            dao.deleteTodoListItemsByListId(removedList.getListId());
            dao.deleteTodoList(removedList.getListId());
            adapterTodoList.removeData(selectedItemPositions.get(i));
        }
        adapterTodoList.notifyDataSetChanged();
        SnackToa.snackBarSuccess(getActivity(), getString(R.string.todoListDeleteMessage));
        getTodoLists();
    }

    private List<TodoList> getSelectedItems() {
        List<TodoList> selectedTodoList = new ArrayList<>();
        List<Integer> selectedItemPositions = adapterTodoList.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            TodoList selectedList = searchedLists.get(selectedItemPositions.get(i));
            selectedTodoList.add(selectedList);
        }
        return selectedTodoList;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.searchBar);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.todoListSearch));
        searchView.setOnQueryTextListener(searchListener);
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchedLists.clear();
                searchedLists.addAll(todoLists);
                adapterTodoList.notifyDataSetChanged();
                return false;
            }
        });
        EditText searchEditText = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchEditText.setHintTextColor(getResources().getColor(R.color.white));
        if (lastSearch != null && !lastSearch.isEmpty()) {
            searchView.setIconified(false);
            searchView.setQuery(lastSearch, false);
        }
    }

    private void showBottomSheetDialog(final List<TodoList> todoLists) {
        //TODO
    }
}
