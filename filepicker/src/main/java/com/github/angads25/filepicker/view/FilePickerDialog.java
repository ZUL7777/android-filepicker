/*
 * Copyright (C) 2016 Angad Singh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.angads25.filepicker.view;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.github.angads25.filepicker.R;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.controller.NotifyItemChecked;
import com.github.angads25.filepicker.controller.adapters.FileListAdapter;
import com.github.angads25.filepicker.controller.interfaces.OnItemClickListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.model.FileListItem;
import com.github.angads25.filepicker.model.MarkedItemList;
import com.github.angads25.filepicker.utils.ExtensionFilter;
import com.github.angads25.filepicker.utils.RecyclerViewTouchHandler;
import com.github.angads25.filepicker.utils.Utility;
import com.github.angads25.filepicker.widget.MaterialCheckbox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Keep;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * <p>
 * Created by Angad Singh on 09-07-2016.
 * </p>
 */

@Keep
public class FilePickerDialog extends Dialog implements OnItemClickListener {
    private static final int EXTERNAL_READ_PERMISSION_GRANT = 112;
    private Context context;
    private RecyclerView listView;
    private AppCompatTextView dname, dir_path, title;
    private DialogProperties properties;
    private DialogSelectionListener callbacks;
    private ArrayList<FileListItem> internalList;
    private ExtensionFilter filter;
    private FileListAdapter mFileListAdapter;
    private AppCompatButton select;
    private String titleStr = null;
    private String positiveBtnNameStr = null;
    private String negativeBtnNameStr = null;

    FilePickerDialog(Context context) {
        super(context);
        this.context = context;
        properties = new DialogProperties();
        filter = new ExtensionFilter(properties);
        internalList = new ArrayList<>();
    }

    public FilePickerDialog(Context context, DialogProperties properties) {
        super(context);
        this.context = context;
        this.properties = properties;
        filter = new ExtensionFilter(properties);
        internalList = new ArrayList<>();
    }

    public FilePickerDialog(Context context, DialogProperties properties, int themeResId) {
        super(context, themeResId);
        this.context = context;
        this.properties = properties;
        filter = new ExtensionFilter(properties);
        internalList = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_main);
        listView = findViewById(R.id.fileList);
        listView.setLayoutManager(new LinearLayoutManager(getContext()));

        select = findViewById(R.id.select);
        int size = MarkedItemList.getFileCount();
        if (size == 0) {
            select.setEnabled(false);
            int color;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                color = context.getResources().getColor(R.color.colorAccent, context.getTheme());
            } else {
                color = context.getResources().getColor(R.color.colorAccent);
            }
            select.setTextColor(Color.argb(128, Color.red(color), Color.green(color), Color.blue(color)));
        }
        dname = findViewById(R.id.dname);
        title = findViewById(R.id.title);
        dir_path = findViewById(R.id.dir_path);
        Button cancel = findViewById(R.id.cancel);
        if (negativeBtnNameStr != null) {
            cancel.setText(negativeBtnNameStr);
        }
        select.setOnClickListener(view -> {
            /*  Select Button is clicked. Get the array of all selected items
             *  from MarkedItemList singleton.
             */
            String[] paths = MarkedItemList.getSelectedPaths();
            //NullPointerException fixed in v1.0.2
            if (callbacks != null) {
                callbacks.onSelectedFilePaths(paths);
            }
            dismiss();
        });
        cancel.setOnClickListener(view -> cancel());
        mFileListAdapter = new FileListAdapter(internalList, context, properties);
        mFileListAdapter.setNotifyItemCheckedListener(() -> {
            /*  Handler function, called when a checkbox is checked ie. a file is
             *  selected.
             */
            positiveBtnNameStr = positiveBtnNameStr == null ?
                    context.getResources().getString(R.string.choose_button_label) : positiveBtnNameStr;
            int size1 = MarkedItemList.getFileCount();
            if (size1 == 0) {
                select.setEnabled(false);
                int color;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    color = context.getResources().getColor(R.color.colorAccent, context.getTheme());
                }
                else {
                    color = context.getResources().getColor(R.color.colorAccent);
                }
                select.setTextColor(Color.argb(128, Color.red(color), Color.green(color), Color.blue(color)));
                select.setText(positiveBtnNameStr);
            } else {
                select.setEnabled(true);
                int color;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    color = context.getResources().getColor(R.color.colorAccent, context.getTheme());
                }
                else {
                    color = context.getResources().getColor(R.color.colorAccent);
                }
                select.setTextColor(color);
                String button_label = positiveBtnNameStr + " (" + size1 + ") ";
                select.setText(button_label);
            }
            if (properties.selection_mode == DialogConfigs.SINGLE_MODE) {
                /*  If a single file has to be selected, clear the previously checked
                 *  checkbox from the list.
                 */
                mFileListAdapter.notifyDataSetChanged();
            }
        });
        listView.setAdapter(mFileListAdapter);

        //Title method added in version 1.0.5
        setTitle();
    }

    private void setTitle() {
        if (title == null || dname == null) {
            return;
        }
        if (titleStr != null) {
            if (title.getVisibility() == View.INVISIBLE) {
                title.setVisibility(View.VISIBLE);
            }
            title.setText(titleStr);
            if (dname.getVisibility() == View.VISIBLE) {
                dname.setVisibility(View.INVISIBLE);
            }
        } else {
            if (title.getVisibility() == View.VISIBLE) {
                title.setVisibility(View.INVISIBLE);
            }
            if (dname.getVisibility() == View.INVISIBLE) {
                dname.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        positiveBtnNameStr = (
                positiveBtnNameStr == null ?
                context.getResources().getString(R.string.choose_button_label) :
                positiveBtnNameStr
        );
        select.setText(positiveBtnNameStr);
        if (Utility.checkStorageAccessPermissions(context)) {
            File currLoc;
            internalList.clear();
            if (properties.offset.isDirectory() && validateOffsetPath()) {
                currLoc = new File(properties.offset.getAbsolutePath());
                FileListItem parent = new FileListItem();
                parent.setFilename(context.getString(R.string.label_parent_dir));
                parent.setDirectory(true);
                parent.setLocation(currLoc.getParentFile().getAbsolutePath());
                parent.setTime(currLoc.lastModified());
                internalList.add(parent);
            } else if (properties.root.exists() && properties.root.isDirectory()) {
                currLoc = new File(properties.root.getAbsolutePath());
            } else {
                currLoc = new File(properties.error_dir.getAbsolutePath());
            }
            dname.setText(currLoc.getName());
            dir_path.setText(currLoc.getAbsolutePath());
            setTitle();
            internalList = Utility.prepareFileListEntries(internalList, currLoc, filter);
            mFileListAdapter.notifyDataSetChanged();
            RecyclerViewTouchHandler handler = new RecyclerViewTouchHandler(context, listView);
            handler.setOnClickListener(this);
        }
    }

    private boolean validateOffsetPath() {
        String offset_path = properties.offset.getAbsolutePath();
        String root_path = properties.root.getAbsolutePath();
        return !offset_path.equals(root_path) && offset_path.contains(root_path);
    }

    @Override
    public void onClick(RecyclerView parent1, View view, int position) {
        if (internalList.size() > position) {
            FileListItem fitem = internalList.get(position);
            if (fitem.isDirectory()) {
                if (new File(fitem.getLocation()).canRead()) {
                    File currLoc = new File(fitem.getLocation());
                    dname.setText(currLoc.getName());
                    setTitle();
                    dir_path.setText(currLoc.getAbsolutePath());
                    internalList.clear();
                    if (!currLoc.getName().equals(properties.root.getName())) {
                        FileListItem parent = new FileListItem();
                        parent.setFilename(context.getString(R.string.label_parent_dir));
                        parent.setDirectory(true);
                        parent.setLocation(currLoc.getParentFile().getAbsolutePath());
                        parent.setTime(currLoc.lastModified());
                        internalList.add(parent);
                    }
                    internalList = Utility.prepareFileListEntries(internalList, currLoc, filter);
                    mFileListAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(context, R.string.error_dir_access, Toast.LENGTH_SHORT).show();
                }
            } else {
                MaterialCheckbox fmark = view.findViewById(R.id.file_mark);
                fmark.performClick();
            }
        }
    }

    public DialogProperties getProperties() {
        return properties;
    }

    public void setProperties(DialogProperties properties) {
        this.properties = properties;
        filter = new ExtensionFilter(properties);
    }

    public void setDialogSelectionListener(DialogSelectionListener callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public void setTitle(CharSequence titleStr) {
        if (titleStr != null) {
            this.titleStr = titleStr.toString();
        } else {
            this.titleStr = null;
        }
        setTitle();
    }

    public void setPositiveBtnName(CharSequence positiveBtnNameStr) {
        if (positiveBtnNameStr != null) {
            this.positiveBtnNameStr = positiveBtnNameStr.toString();
        } else {
            this.positiveBtnNameStr = null;
        }
    }

    public void setNegativeBtnName(CharSequence negativeBtnNameStr) {
        if (negativeBtnNameStr != null) {
            this.negativeBtnNameStr = negativeBtnNameStr.toString();
        } else {
            this.negativeBtnNameStr = null;
        }
    }

    public void markFiles(List<String> paths) {
        if (paths != null && paths.size() > 0) {
            if (properties.selection_mode == DialogConfigs.SINGLE_MODE) {
                File temp = new File(paths.get(0));
                switch (properties.selection_type) {
                    case DialogConfigs.DIR_SELECT:
                        if (temp.exists() && temp.isDirectory()) {
                            FileListItem item = new FileListItem();
                            item.setFilename(temp.getName());
                            item.setDirectory(temp.isDirectory());
                            item.setMarked(true);
                            item.setTime(temp.lastModified());
                            item.setLocation(temp.getAbsolutePath());
                            MarkedItemList.addSelectedItem(item);
                        }
                        break;

                    case DialogConfigs.FILE_SELECT:
                        if (temp.exists() && temp.isFile()) {
                            FileListItem item = new FileListItem();
                            item.setFilename(temp.getName());
                            item.setDirectory(temp.isDirectory());
                            item.setMarked(true);
                            item.setTime(temp.lastModified());
                            item.setLocation(temp.getAbsolutePath());
                            MarkedItemList.addSelectedItem(item);
                        }
                        break;

                    case DialogConfigs.FILE_AND_DIR_SELECT:
                        if (temp.exists()) {
                            FileListItem item = new FileListItem();
                            item.setFilename(temp.getName());
                            item.setDirectory(temp.isDirectory());
                            item.setMarked(true);
                            item.setTime(temp.lastModified());
                            item.setLocation(temp.getAbsolutePath());
                            MarkedItemList.addSelectedItem(item);
                        }
                        break;
                }
            } else {
                for (String path : paths) {
                    switch (properties.selection_type) {
                        case DialogConfigs.DIR_SELECT:
                            File temp = new File(path);
                            if (temp.exists() && temp.isDirectory()) {
                                FileListItem item = new FileListItem();
                                item.setFilename(temp.getName());
                                item.setDirectory(temp.isDirectory());
                                item.setMarked(true);
                                item.setTime(temp.lastModified());
                                item.setLocation(temp.getAbsolutePath());
                                MarkedItemList.addSelectedItem(item);
                            }
                            break;

                        case DialogConfigs.FILE_SELECT:
                            temp = new File(path);
                            if (temp.exists() && temp.isFile()) {
                                FileListItem item = new FileListItem();
                                item.setFilename(temp.getName());
                                item.setDirectory(temp.isDirectory());
                                item.setMarked(true);
                                item.setTime(temp.lastModified());
                                item.setLocation(temp.getAbsolutePath());
                                MarkedItemList.addSelectedItem(item);
                            }
                            break;

                        case DialogConfigs.FILE_AND_DIR_SELECT:
                            temp = new File(path);
                            if (temp.exists() && (temp.isFile() || temp.isDirectory())) {
                                FileListItem item = new FileListItem();
                                item.setFilename(temp.getName());
                                item.setDirectory(temp.isDirectory());
                                item.setMarked(true);
                                item.setTime(temp.lastModified());
                                item.setLocation(temp.getAbsolutePath());
                                MarkedItemList.addSelectedItem(item);
                            }
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void show() {
        if (!Utility.checkStorageAccessPermissions(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ((Activity) context).requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXTERNAL_READ_PERMISSION_GRANT);
            }
        } else {
            super.show();
            positiveBtnNameStr = positiveBtnNameStr == null ?
                    context.getResources().getString(R.string.choose_button_label) : positiveBtnNameStr;
            select.setText(positiveBtnNameStr);
            int size = MarkedItemList.getFileCount();
            if (size == 0) {
                select.setText(positiveBtnNameStr);
            } else {
                String button_label = positiveBtnNameStr + " (" + size + ") ";
                select.setText(button_label);
            }
        }
    }

    @Override
    public void onBackPressed() {
        //currentDirName is dependent on dname
        String currentDirName = dname.getText().toString();
        if (internalList.size() > 0) {
            FileListItem fitem = internalList.get(0);
            File currLoc = new File(fitem.getLocation());
            if (currentDirName.equals(properties.root.getName()) ||
                    !currLoc.canRead()) {
                super.onBackPressed();
            } else {
                dname.setText(currLoc.getName());
                dir_path.setText(currLoc.getAbsolutePath());
                internalList.clear();
                if (!currLoc.getName().equals(properties.root.getName())) {
                    FileListItem parent = new FileListItem();
                    parent.setFilename(context.getString(R.string.label_parent_dir));
                    parent.setDirectory(true);
                    parent.setLocation(currLoc.getParentFile().getAbsolutePath());
                    parent.setTime(currLoc.lastModified());
                    internalList.add(parent);
                }
                internalList = Utility.prepareFileListEntries(internalList, currLoc, filter);
                mFileListAdapter.notifyDataSetChanged();
            }
            setTitle();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void dismiss() {
        MarkedItemList.clearSelectionList();
        internalList.clear();
        super.dismiss();
    }
}
