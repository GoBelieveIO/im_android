/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.view;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.Location;
import com.beetle.imlib.R;

import java.beans.PropertyChangeEvent;

public class MessageLocationView extends MessageContentView {

    protected ProgressBar progressBar;

    public MessageLocationView(Context context) {
        super(context);

        int contentLayout = R.layout.chat_content_location;
        inflater.inflate(contentLayout, this);
        progressBar = (ProgressBar)findViewById(R.id.progress_bar);
    }

    public void setMessage(IMessage msg) {
        super.setMessage(msg);
        Location loc = (Location)this.message.content;
        if (loc.address != null && loc.address.length() > 0) {
            TextView content = (TextView) findViewById(R.id.text);
            content.setText(loc.address);
        }
        boolean geocoding = this.message.getGeocoding();
        if (geocoding) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
        requestLayout();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getPropertyName().equals("geocoding")) {
            boolean geocoding = this.message.getGeocoding();
            if (geocoding) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                Location loc = (Location)this.message.content;
                if (loc.address != null && loc.address.length() > 0) {
                    TextView content = (TextView) findViewById(R.id.text);
                    content.setText(loc.address);
                }
            }
        }
    }
}
