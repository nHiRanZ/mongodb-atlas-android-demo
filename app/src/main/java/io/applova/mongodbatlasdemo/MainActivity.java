package io.applova.mongodbatlasdemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import io.applova.mongodbatlasdemo.adapter.PersonAdapter;
import io.applova.mongodbatlasdemo.async.TaskCallback;
import io.applova.mongodbatlasdemo.databinding.ActivityMainBinding;
import io.applova.mongodbatlasdemo.domain.Person;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.sync.Subscription;
import io.realm.mongodb.sync.SyncConfiguration;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("mongodbatlasdemo");
    }

    private ActivityMainBinding binding;
    private final String appID = "MY_APP_ID";
    private final String apiKey = "MY_API_KEY";
    private Realm realm;
    private PersonAdapter adapter;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        App app = new App(new AppConfiguration.Builder(appID).build());
        Credentials credentials = Credentials.apiKey(apiKey);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.addPersonButton.setEnabled(false);
        app.loginAsync(credentials, it -> {
            if (it.isSuccess()) {
                binding.addPersonButton.setEnabled(true);
                SyncConfiguration config = new SyncConfiguration.Builder(app.currentUser())
                        .initialSubscriptions((realm, subscriptions) -> {
                            String subscriptionName = "person_subscription";
                            if (subscriptions.find(subscriptionName) == null) {
                                subscriptions.add(Subscription.create(subscriptionName, realm.where(Person.class)));
                            }
                        })
                        .build();

                Realm.getInstanceAsync(config, new Realm.Callback() {
                    @Override
                    public void onSuccess(Realm realmInstance) {
                        realm = realmInstance;
                        adapter = new PersonAdapter(new ArrayList<>(), realm, result -> {
                            if (result instanceof String) {
                                String personId = (String) result;
                                realm.executeTransactionAsync(realm -> {
                                    Person personToDelete = realm.where(Person.class).equalTo("_id", personId).findFirst();
                                    if (personToDelete != null) {
                                        personToDelete.deleteFromRealm();
                                    }
                                });
                            }
                        });
                        RealmResults<Person> people = realm.where(Person.class).findAllAsync();
                        binding.personsList.setLayoutManager(new LinearLayoutManager(context));
                        binding.personsList.setAdapter(adapter);

                        people.addChangeListener(results -> adapter.updateData(results));
                    }
                });
            } else {
                Log.e("EXAMPLE", "Failed to log in: " + it.getError().getErrorMessage());
            }
        });

        binding.addPersonButton.setOnClickListener(v -> showDialogToAddPerson());
    }

    private void showDialogToAddPerson() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Person");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView);

        EditText nameInput = dialogView.findViewById(R.id.nameInput);
        EditText ageInput = dialogView.findViewById(R.id.ageInput);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String ageStr = ageInput.getText().toString();
            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Invalid age", Toast.LENGTH_SHORT).show();
                return;
            }

            realm.executeTransactionAsync(realm -> {
                        Person person = new Person();
                        person.set_id(String.valueOf(System.currentTimeMillis()));
                        person.setName(name);
                        person.setAge(age);
                        realm.copyToRealmOrUpdate(person);
                    }, () -> Log.d("MainActivity", "Person added successfully!"),
                    error -> Log.e("MainActivity", "Error adding person: " + error.getMessage()));
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realm != null) {
            realm.close();
        }
    }
}
