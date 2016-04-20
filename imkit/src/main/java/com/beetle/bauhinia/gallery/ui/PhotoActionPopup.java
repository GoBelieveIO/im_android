/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.beetle.bauhinia.gallery.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.beetle.imkit.R;

import java.util.ArrayList;

/**
 * Shows a popup asking the user what to do for a photo. The result is passed back to the Listener
 */
public class PhotoActionPopup {
    public static final String TAG = PhotoActionPopup.class.getSimpleName();

    public static Dialog createDialog(Context context, final Listener listener) {
        final ListAdapter adapter = getListAdapter(context, listener);

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setAdapter(adapter, null)
                .create();

        dialog.setCanceledOnTouchOutside(true);

        dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                pickItem(adapter, position);
                dialog.dismiss();
            }
        });

        return dialog;
    }

    private static void pickItem(ListAdapter adapter, int position) {
        final ChoiceListItem choice = (ChoiceListItem) adapter.getItem(position);
        choice.onPick();
    }

    private static ListAdapter getListAdapter(final Context context, final Listener listener) {
        final ArrayList<ChoiceListItem> choices = new ArrayList<ChoiceListItem>(1);

        // Save photo.
        choices.add(new ChoiceListItem(ChoiceListItem.ID_SAVE_TO_PHONE,
                context.getString(R.string.gallery_save_to_phone),
                new Runnable() {
                    @Override
                    public void run() {
                        listener.onSaveToPhone();
                    }
                }));

        return new ArrayAdapter<ChoiceListItem>(context, R.layout.gallery_select_dialog_item, choices);
    }

    private static final class ChoiceListItem {
        private final int mId;
        private final String mCaption;
        private final Runnable action;

        public static final int ID_SAVE_TO_PHONE = 0;

        public ChoiceListItem(int id, String caption, Runnable action) {
            mId = id;
            mCaption = caption;
            this.action = action;
        }

        public void onPick() {
            if (action != null) {
                action.run();
            }
        }

        @Override
        public String toString() {
            return mCaption;
        }

        public int getId() {
            return mId;
        }
    }

    public interface Listener {
        void onSaveToPhone();
    }
}
