package org.croudtrip.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.dao.Dao;

import org.croudtrip.DB_Dummy;
import org.croudtrip.MainApplication;
import org.croudtrip.R;
import org.croudtrip.UserResource;
import org.croudtrip.auth.User;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import retrofit.RestAdapter;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Dummy activity for quick testing various things (server, db, ...).
 */
public class DummyActivity extends ActionBarActivity{

    private Dao<DB_Dummy, Long> dummyDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy);

        setupDb();
        findViewById(R.id.db).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Query db for all DB_Dummies
                    List<DB_Dummy> dummies = dummyDao.queryForAll();
                    String content = "Found these dummies in DB: " + Arrays.toString(dummies.toArray()) + "\n"
                            + "Found DB_Dummy with id 1: " + dummyDao.queryForId(1L).toString();
                    Toast.makeText(DummyActivity.this, content, Toast.LENGTH_LONG).show();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        final UserListAdapter listAdapter = new UserListAdapter();
                ((ListView) findViewById(R.id.list)).setAdapter(listAdapter);

        UserResource userResource = new RestAdapter.Builder()
                .setEndpoint(getString(R.string.server_address))
                .build()
                .create(UserResource.class);

        userResource.getAllUsers()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<User>>() {
                    @Override
                    public void call(List<User> users) {
                        listAdapter.setItems(users);
                    }
                });
    }


    private void setupDb() {
        try {
            dummyDao = ((MainApplication) getApplicationContext()).getHelper().getDB_DummyDao();

            // Create some entries to demonstrate how this is working
            DB_Dummy brother = new DB_Dummy("Brother", null);
            dummyDao.create(brother);

            DB_Dummy dummy = new DB_Dummy("Dummy", brother);
            dummyDao.create(dummy);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private final class UserListAdapter extends BaseAdapter {

        private final List<User> users = new LinkedList<>();

        public void setItems(List<User> users) {
            this.users.clear();
            this.users.addAll(users);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return users.size();
        }

        @Override
        public User getItem(int position) {
            return users.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final User user = getItem(position);
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            ((TextView) view.findViewById(android.R.id.text1)).setText(user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ")");
            return view;
        }
    }
}
