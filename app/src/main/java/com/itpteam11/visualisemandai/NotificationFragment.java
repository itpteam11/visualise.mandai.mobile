package com.itpteam11.visualisemandai;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private ArrayAdapter<String> notificationsAdapter;
    private ArrayList<String> notificationsArrayList;
    private ListView notiListView;

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_notification, container, false);

        notiListView = (ListView) view.findViewById(R.id.notificationListView);
        notificationsArrayList = new ArrayList<String>();


        DatabaseReference notiRef = FirebaseDatabase.getInstance().getReference().child("service").child("weather").child("notifications");
        notiRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot notification : dataSnapshot.getChildren()) {
                    String notiTitle = notification.getValue().toString();
                    Log.d("NOTITITLE", notiTitle);
                    notificationsArrayList.add(notiTitle);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        notificationsAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, notificationsArrayList);
        notificationsAdapter.notifyDataSetChanged();
        notiListView.setAdapter(notificationsAdapter);
        return view;
    }

}
