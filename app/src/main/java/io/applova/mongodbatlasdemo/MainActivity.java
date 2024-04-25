package io.applova.mongodbatlasdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import io.applova.mongodbatlasdemo.adapter.PersonAdapter;
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
    private final String appID = "application-0-fzyts";
    private final String apiKey = "0CfKPr9Pi4meIoj1cV7I7Ux9x4F4yjPSSNl13NpMerk8XheDuLwWsKqUoPWtHnCB";
    private Realm realm;
    private PersonAdapter adapter;
    private Context context;
    private JmDNS jmdns;
    private ConnectivityManager connectivityManager;
    private boolean isOnline = true;
    private final String APP_NAME = "io.applova.mongodbatlasdemo";

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
                                deletePerson((String) result);
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

        setupJmDNS();
        setupNetworkCallback();
    }

    private void setupNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                updateOnlineStatus(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                updateOnlineStatus(false);
            }
        });
    }

    private void updateOnlineStatus(boolean isOnline) {
        this.isOnline = isOnline;
        runOnUiThread(() -> {
            if (isOnline) {
                Toast.makeText(MainActivity.this, "Internet is available", Toast.LENGTH_SHORT).show();
                binding.onlineStatus.setText("Online");
                binding.onlineStatus.setBackgroundColor(getResources().getColor(R.color.online_green));
            } else {
                Toast.makeText(MainActivity.this, "Internet is not available", Toast.LENGTH_SHORT).show();
                binding.onlineStatus.setText("Offline");
                binding.onlineStatus.setBackgroundColor(getResources().getColor(R.color.offline_red));
            }
        });
    }

    private void setupJmDNS() {
        new AsyncTask<Void, Void, JmDNS>() {
            protected JmDNS doInBackground(Void... voids) {
                try {
                    // Perform network operation in background
                    return JmDNS.create(InetAddress.getLocalHost());
                } catch (IOException e) {
                    Log.e("JmDNS", "Setup failed", e);
                    return null;
                }
            }

            protected void onPostExecute(JmDNS jmdns) {
                if (jmdns != null) {
                    MainActivity.this.jmdns = jmdns;
                    registerService();
                } else {
                    Log.e("JmDNS", "Error setting up JmDNS");
                }
            }
        }.execute();
    }

    private void registerService() {
        String type = "_http._tcp.local.";
        String name = "AndroidApp";
        int port = 12345;  // The port your service is listening on
        try {
            jmdns.registerService(ServiceInfo.create(type, name, port, "path=index.html"));

            jmdns.addServiceListener(type, new ServiceListener() {
                @Override
                public void serviceResolved(ServiceEvent ev) {
                    Log.d("JmDNS", "Service resolved: " + ev.getInfo().getQualifiedName() + " port:" + ev.getInfo().getPort());
                    applyReceivedData(ev.getInfo().getNiceTextString());
                }

                @Override
                public void serviceAdded(ServiceEvent event) {
                    jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
                }

                @Override
                public void serviceRemoved(ServiceEvent ev) {
                    Log.d("JmDNS", "Service removed: " + ev.getName());
                }
            });
        } catch (IOException e) {
            Log.e("JmDNS", "Error registering service", e);
        }
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
                addOrUpdatePerson(name, age); // Using the new method
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Invalid age", Toast.LENGTH_SHORT).show();
                return;
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addOrUpdatePerson(String name, int age) {
        realm.executeTransactionAsync(realm -> {
                    Person person = new Person();
                    person.set_id(String.valueOf(System.currentTimeMillis()));
                    person.setName(name);
                    person.setAge(age);
                    realm.copyToRealmOrUpdate(person);
                    if (!isOnline) {
                        sendUpdateViaJmDNS(new Gson().toJson(person)); // Send update if offline
                    }
                }, () -> Log.d("MainActivity", "Person added successfully!"),
                error -> Log.e("MainActivity", "Error adding person: " + error.getMessage()));
    }

    private void deletePerson(String personId) {
        realm.executeTransactionAsync(realm -> {
            Person personToDelete = realm.where(Person.class).equalTo("_id", personId).findFirst();
            if (personToDelete != null) {
                HashMap<String, String> message = new HashMap<>();
                message.put("action", "delete");
                message.put("_id", personToDelete.get_id());
                String jsonMessage = new Gson().toJson(message);

                personToDelete.deleteFromRealm();

                if (!isOnline) {
                    sendUpdateViaJmDNS(jsonMessage); // Send deletion message if offline
                }
            }
        });
    }

    private void sendUpdateViaJmDNS(String jsonMessage) {
        try {
            ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", APP_NAME, 12345, jsonMessage);
            jmdns.registerService(serviceInfo);
            Log.d("JmDNS", "Sent update via JmDNS: " + jsonMessage);
        } catch (Exception e) {
            Log.e("JmDNS", "Error sending update via JmDNS", e);
        }
    }

    private void applyReceivedData(String json) {
        Gson gson = new Gson();
        HashMap<String, String> data = gson.fromJson(json, HashMap.class);
        if (data.containsKey("action") && "delete".equals(data.get("action"))) {
            realm.executeTransactionAsync(r -> {
                Person personToDelete = r.where(Person.class).equalTo("_id", data.get("_id")).findFirst();
                if (personToDelete != null) {
                    personToDelete.deleteFromRealm();
                }
            });
        } else {
            Person person = gson.fromJson(json, Person.class);
            realm.executeTransactionAsync(r -> r.insertOrUpdate(person));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realm != null) {
            realm.close();
        }
        if (jmdns != null) {
            try {
                jmdns.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(new ConnectivityManager.NetworkCallback());
        }
    }
}